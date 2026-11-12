package com.tovo.erp.conges.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.model.*;
import com.tovo.erp.conges.repository.*;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.temps.service.CalendrierTempsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CongeService {
    private static final String MODE_OUVRABLES = "OUVRABLES";

    private final TypeCongeRepository types;
    private final SoldeCongeRepository soldes;
    private final DroitCongeRepository droits;
    private final DemandeCongeRepository demandes;
    private final AbsenceRepository absences;
    private final MouvementCongeRepository mouvements;
    private final EmployeService employes;
    private final CalendrierTempsService calendrier;
    private final ParametreCongeService parametres;
    private final PeriodeBloqueeRepository periodesBloquees;
    private final SuppleanceService suppleances;

    public CongeService(TypeCongeRepository types, SoldeCongeRepository soldes,
                        DemandeCongeRepository demandes, AbsenceRepository absences,
                        DroitCongeRepository droits, MouvementCongeRepository mouvements,
                        EmployeService employes, CalendrierTempsService calendrier,
                        ParametreCongeService parametres, PeriodeBloqueeRepository periodesBloquees,
                        SuppleanceService suppleances) {
        this.types = types;
        this.soldes = soldes;
        this.droits = droits;
        this.demandes = demandes;
        this.absences = absences;
        this.mouvements = mouvements;
        this.employes = employes;
        this.calendrier = calendrier;
        this.parametres = parametres;
        this.periodesBloquees = periodesBloquees;
        this.suppleances = suppleances;
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.TypeDto> types() {
        return types.findByActifTrueOrderByLibelleAsc().stream().map(CongeDtos.TypeDto::de).toList();
    }

    @Transactional(readOnly = true)
    public CongeDtos.EmployeCourantDto employeCourant(UtilisateurConnecte utilisateur) {
        Long id = idEmployeDe(utilisateur)
                .orElseThrow(() -> BusinessException.notFound(
                        "Aucun dossier employe n'est associe a votre compte."));
        return new CongeDtos.EmployeCourantDto(id);
    }

    @Transactional
    public List<CongeDtos.SoldeDto> soldes(Long idEmploye, int annee) {
        verifierEmploye(idEmploye);
        Map<Long, SoldeConge> existants = soldes.findByIdEmployeAndAnnee(idEmploye, annee).stream()
                .collect(Collectors.toMap(SoldeConge::getIdTypeConge, Function.identity()));
        BigDecimal plafond = plafond();
        BigDecimal seuilAlerte = seuilAlerte();
        BigDecimal total = totalPourEmploye(idEmploye);
        return types.findByActifTrueOrderByLibelleAsc().stream().map(type -> {
            SoldeConge solde = existants.get(type.getId());
            if (solde == null) {
                solde = soldes.save(nouveauSolde(idEmploye, type, annee));
            }
            return soldeDto(solde, type, plafond, total.compareTo(seuilAlerte) >= 0);
        }).toList();
    }

    @Transactional(readOnly = true)
    public void verifierAccesEmploye(Long idEmploye, UtilisateurConnecte utilisateur) {
        verifierEmploye(idEmploye);
        if ("EMPLOYE".equals(utilisateur.getRoleNom())) {
            idEmployeDe(utilisateur).filter(id -> id.equals(idEmploye))
                    .orElseThrow(() -> BusinessException.forbidden(
                            "Vous ne pouvez consulter que vos propres donnees."));
        }
    }

    // ------------------------------------------------------------------
    // Demandes
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> demandes(String statut) {
        return demandes(parserStatutDemande(statut));
    }

    private StatutDemandeConge parserStatutDemande(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        try {
            return StatutDemandeConge.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_INVALIDE", "Statut de demande de conge inconnu : " + statut);
        }
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> demandes(StatutDemandeConge statut) {
        Map<Long, String> noms = nomsTypes();
        var liste = statut == null ? demandes.findAllByOrderByDateCreationDesc()
                : demandes.findByStatutOrderByDateCreationDesc(statut);
        return liste.stream().map(d -> CongeDtos.DemandeDto.de(d, noms.get(d.getIdTypeConge()))).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> demandesEmploye(Long idEmploye) {
        verifierEmploye(idEmploye);
        Map<Long, String> noms = nomsTypes();
        return demandes.findByIdEmployeOrderByDateCreationDesc(idEmploye).stream()
                .map(d -> CongeDtos.DemandeDto.de(d, noms.get(d.getIdTypeConge())))
                .toList();
    }

    @Transactional
    public CongeDtos.DemandeDto creerDemande(CongeDtos.DemandeRequest request) {
        verifierEmploye(request.idEmploye());
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin doit etre posterieure ou egale au debut.");
        }
        TypeConge type = types.findById(request.idTypeConge())
                .orElseThrow(() -> BusinessException.notFound("Type de conge introuvable."));
        BigDecimal jours = joursDecomptes(request.dateDebut(), request.dateFin(),
                request.demiJourneeDebut(), request.demiJourneeFin());
        if (jours.signum() <= 0) {
            throw new BusinessException("DUREE_INVALIDE", "La periode ne contient aucun jour decomptable.");
        }
        if (type.getDureeMaxJours() != null
                && jours.compareTo(BigDecimal.valueOf(type.getDureeMaxJours())) > 0) {
            throw new BusinessException("DUREE_MAX_DEPASSEE",
                    "La duree maximale de ce type de conge est de " + type.getDureeMaxJours() + " jours.");
        }
        verifierPrevenance(type, jours, request.dateDebut());
        verifierChevauchements(request.idEmploye(), request.dateDebut(), request.dateFin());
        verifierPeriodeBloquee(request.dateDebut(), request.dateFin(), false);
        verifierDelaiDeclaration(type, request.dateDebut());
        verifierEffectifMinimum(type, request.idEmploye(), request.dateDebut(), request.dateFin(),
                jours, request.idRemplacant(), null);

        DemandeConge demande = new DemandeConge();
        demande.setIdEmploye(request.idEmploye());
        demande.setIdTypeConge(type.getId());
        demande.setDateDebut(request.dateDebut());
        demande.setDateFin(request.dateFin());
        demande.setDemiJourneeDebut(request.demiJourneeDebut());
        demande.setDemiJourneeFin(request.demiJourneeFin());
        demande.setNombreJours(jours);
        demande.setMotif(request.motif().trim());
        demande.setIdRemplacant(request.idRemplacant());
        demande.setJustificatifDocumentId(request.justificatifDocumentId());
        demande.setDateDepot(LocalDateTime.now());
        demande.setStatut(StatutDemandeConge.SOUMISE);
        demande = demandes.save(demande);

        if (type.isDecompteSolde()) {
            reserver(demande, type, jours);
        }
        if (request.idRemplacant() != null) {
            suppleances.verifierEligible(demande.getIdEmploye(), jours, request.idRemplacant(),
                    demande.getDateDebut(), demande.getDateFin());
            suppleances.proposer(demande.getId(), new CongeDtos.SuppleanceRequest(request.idRemplacant(),
                    "Remplacant propose a la soumission"));
        }
        return CongeDtos.DemandeDto.de(demande, type.getLibelle());
    }

    /**
     * Workflow : Manager (SOUMISE -> VALIDEE_MANAGER), RH (-> VALIDEE_RH ou
     * APPROUVEE selon la duree), Direction (VALIDEE_RH -> APPROUVEE).
     */
    @Transactional
    public CongeDtos.DemandeDto decision(Long id, CongeDtos.DecisionRequest request, boolean valider,
                                         UtilisateurConnecte utilisateur) {
        DemandeConge demande = demandes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Demande de conge introuvable."));
        TypeConge type = types.findById(demande.getIdTypeConge())
                .orElseThrow(() -> BusinessException.notFound("Type de conge introuvable."));
        String role = utilisateur.getRoleNom();
        String motif = request == null ? null : request.motif();

        if (!valider) {
            verifierRefusAutorise(demande, role, utilisateur, motif);
            libererReservation(demande, type, "Refus de la demande");
            demande.setStatut(StatutDemandeConge.REFUSEE);
            demande.setIdValidateur(utilisateur.getId());
            demande.setMotifDecision(motif == null ? null : motif.trim());
            demande.setDateDecision(LocalDateTime.now());
            return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
        }

        if ("MANAGER".equals(role)) {
            if (demande.getStatut() != StatutDemandeConge.SOUMISE) {
                throw new BusinessException("ETAPE_INVALIDE", "Cette demande n'est pas en attente de validation manager.");
            }
            if (!demandes.managerAffecteAEmploye(utilisateur.getId(), demande.getIdEmploye(),
                    demande.getDateDebut(), demande.getDateFin())) {
                throw BusinessException.forbidden(
                        "Un manager ne peut valider que les demandes des employes de son equipe.");
            }
            demande.setStatut(StatutDemandeConge.VALIDEE_MANAGER);
            demande.setIdValidateurManager(utilisateur.getId());
            demande.setDateValidationManager(LocalDateTime.now());
            demande.setIdValidateur(utilisateur.getId());
            demande.setMotifDecision(motif == null ? null : motif.trim());
            demande.setDateDecision(LocalDateTime.now());
            return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
        }

        if ("RH".equals(role) || "DIRECTION".equals(role)) {
            if (demande.getStatut() == StatutDemandeConge.VALIDEE_MANAGER) {
                verifierPeriodeBloquee(demande.getDateDebut(), demande.getDateFin(), "DIRECTION".equals(role));
                verifierJustificatif(type, demande);
                verifierEffectifMinimum(type, demande.getIdEmploye(), demande.getDateDebut(),
                        demande.getDateFin(), demande.getNombreJours(), demande.getIdRemplacant(),
                        demande.getId());
                demande.setIdValidateurRh(utilisateur.getId());
                demande.setDateValidationRh(LocalDateTime.now());
                if ("DIRECTION".equals(role)
                        || demande.getNombreJours().compareTo(BigDecimal.valueOf(validationDirectionJours())) <= 0) {
                    return approuver(demande, type, utilisateur, motif);
                }
                demande.setStatut(StatutDemandeConge.VALIDEE_RH);
                demande.setIdValidateur(utilisateur.getId());
                demande.setMotifDecision(motif == null ? null : motif.trim());
                demande.setDateDecision(LocalDateTime.now());
                return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
            }
            if (demande.getStatut() == StatutDemandeConge.VALIDEE_RH && "DIRECTION".equals(role)) {
                if (demande.getIdValidateurRh() == null) {
                    demande.setIdValidateurRh(utilisateur.getId());
                    demande.setDateValidationRh(LocalDateTime.now());
                }
                return approuver(demande, type, utilisateur, motif);
            }
            throw new BusinessException("ETAPE_INVALIDE",
                    "Cette demande n'est pas a l'etape de validation attendue pour votre role.");
        }
        throw BusinessException.forbidden("Vous ne pouvez pas valider cette demande.");
    }

    private CongeDtos.DemandeDto approuver(DemandeConge demande, TypeConge type,
                                           UtilisateurConnecte utilisateur, String motif) {
        if (type.isDecompteSolde()) {
            libererReservation(demande, type, "Reservation convertie en consommation");
            consommerFifo(demande, type);
        }
        demande.setStatut(StatutDemandeConge.APPROUVEE);
        demande.setIdValidateur(utilisateur.getId());
        demande.setMotifDecision(motif == null ? null : motif.trim());
        demande.setDateDecision(LocalDateTime.now());
        return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
    }

    private void verifierRefusAutorise(DemandeConge demande, String role,
                                       UtilisateurConnecte utilisateur, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("MOTIF_REQUIS", "Un motif est requis pour refuser une demande.");
        }
        if ("MANAGER".equals(role)
                && (demande.getStatut() != StatutDemandeConge.SOUMISE
                || !demandes.managerAffecteAEmploye(utilisateur.getId(), demande.getIdEmploye(),
                demande.getDateDebut(), demande.getDateFin()))) {
            throw BusinessException.forbidden("Un manager ne peut refuser que les demandes de son equipe en attente.");
        }
        if ("RH".equals(role) && !(demande.getStatut() == StatutDemandeConge.VALIDEE_MANAGER
                || demande.getStatut() == StatutDemandeConge.VALIDEE_RH)) {
            throw new BusinessException("ETAPE_INVALIDE", "Cette demande n'est pas en attente de validation RH.");
        }
        if ("DIRECTION".equals(role) && demande.getStatut() != StatutDemandeConge.VALIDEE_RH) {
            throw new BusinessException("ETAPE_INVALIDE", "Cette demande n'est pas en attente de validation Direction.");
        }
        if (!List.of("MANAGER", "RH", "DIRECTION").contains(role)) {
            throw BusinessException.forbidden("Vous ne pouvez pas refuser cette demande.");
        }
    }

    @Transactional
    public CongeDtos.DemandeDto annuler(Long id, String motif, UtilisateurConnecte utilisateur) {
        DemandeConge demande = demandes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Demande de conge introuvable."));
        TypeConge type = types.findById(demande.getIdTypeConge()).orElseThrow();
        if ("EMPLOYE".equals(utilisateur.getRoleNom())
                && idEmployeDe(utilisateur).filter(i -> i.equals(demande.getIdEmploye())).isEmpty()) {
            throw BusinessException.forbidden("Vous ne pouvez annuler que vos propres demandes.");
        }
        if (!List.of(StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
                StatutDemandeConge.VALIDEE_RH, StatutDemandeConge.APPROUVEE).contains(demande.getStatut())) {
            throw new BusinessException("ANNULATION_INTERDITE", "Cette demande ne peut plus etre annulee.");
        }
        if (!demande.getDateDebut().isAfter(LocalDate.now())) {
            throw new BusinessException("ANNULATION_INTERDITE",
                    "Une demande deja commencee doit etre ecourtee, pas annulee.");
        }
        if (demande.getStatut() == StatutDemandeConge.APPROUVEE && type.isDecompteSolde()) {
            restituer(demande, type, "Annulation avant le debut");
        } else if (type.isDecompteSolde()) {
            libererReservation(demande, type, "Annulation de la demande");
        }
        demande.setStatut(StatutDemandeConge.ANNULEE);
        demande.setMotifDecision(motif == null ? null : motif.trim());
        demande.setDateDecision(LocalDateTime.now());
        return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
    }

    @Transactional
    public CongeDtos.DemandeDto ecourter(Long id, CongeDtos.EcourtementRequest request,
                                         UtilisateurConnecte utilisateur) {
        DemandeConge demande = demandes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Demande de conge introuvable."));
        TypeConge type = types.findById(demande.getIdTypeConge()).orElseThrow();
        if (!(demande.getStatut() == StatutDemandeConge.APPROUVEE
                || demande.getStatut() == StatutDemandeConge.EN_COURS)) {
            throw new BusinessException("ECOURTEMENT_INTERDIT", "Seule une demande approuvee peut etre ecourtee.");
        }
        LocalDate dateFinReelle = request.dateFinReelle();
        if (!dateFinReelle.isAfter(demande.getDateDebut()) || !dateFinReelle.isBefore(demande.getDateFin())) {
            throw new BusinessException("DATES_INVALIDES",
                    "La date de fin reelle doit etre comprise entre le debut et la fin prevue.");
        }
        BigDecimal nouveauxJours = joursDecomptes(demande.getDateDebut(), dateFinReelle,
                demande.getDemiJourneeDebut(), null);
        BigDecimal restitution = demande.getNombreJours().subtract(nouveauxJours);
        if (restitution.signum() > 0 && type.isDecompteSolde()) {
            restituer(demande, type, restitution, "Retour anticipe du " + dateFinReelle);
        }
        demande.setDateFin(dateFinReelle);
        demande.setNombreJours(nouveauxJours);
        demande.setStatut(StatutDemandeConge.ECOURTEE);
        demande.setMotifDecision(request.motif() == null ? null : request.motif().trim());
        demande.setDateDecision(LocalDateTime.now());
        return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
    }

    @Transactional
    public CongeDtos.SoldeDto ajuster(CongeDtos.AjustementRequest request, Long idValidateur) {
        verifierEmploye(request.idEmploye());
        TypeConge type = types.findById(request.idTypeConge())
                .orElseThrow(() -> BusinessException.notFound("Type de conge introuvable."));
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("MOTIF_REQUIS", "Un motif est requis pour un ajustement.");
        }
        BigDecimal plafond = plafond();
        BigDecimal total = totalPourType(request.idEmploye(), type.getId());
        BigDecimal nouveauTotal = total.add(request.jours());
        if (nouveauTotal.signum() < 0 || nouveauTotal.compareTo(plafond) > 0) {
            throw new BusinessException("SOLDE_HORS_PLAFOND",
                    "L'ajustement depasse le plafond de " + plafond + " jours ou rend le solde negatif.");
        }
        SoldeConge solde = soldePour(request.idEmploye(), type, request.annee());
        solde.setJoursAcquis(solde.getJoursAcquis().add(request.jours()));
        soldes.save(solde);
        MouvementConge mouvement = mouvement(solde, null, TypeMouvementConge.AJUSTEMENT,
                request.jours(), LocalDate.now(), request.motif().trim());
        mouvements.save(mouvement);
        return soldeDto(solde, type, plafond, false);
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.MouvementDto> mouvements(Long idEmploye, Long idTypeConge) {
        verifierEmploye(idEmploye);
        return (idTypeConge == null
                ? mouvements.findByIdEmployeOrderByDateEffetDescDateCreationDesc(idEmploye)
                : mouvements.findByIdEmployeAndIdTypeCongeOrderByDateEffetAscDateCreationAsc(idEmploye, idTypeConge))
                .stream().map(CongeDtos.MouvementDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.PeriodeBloqueeDto> periodesBloquees() {
        return periodesBloquees.findAll().stream().map(CongeDtos.PeriodeBloqueeDto::de).toList();
    }

    @Transactional
    public CongeDtos.PeriodeBloqueeDto creerPeriodeBloquee(CongeDtos.PeriodeBloqueeRequest request) {
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La fin de la periode doit suivre son debut.");
        }
        PeriodeBloquee periode = new PeriodeBloquee();
        periode.setLibelle(request.libelle().trim());
        periode.setDateDebut(request.dateDebut());
        periode.setDateFin(request.dateFin());
        periode.setDerogationDirection(request.derogationDirection());
        periode.setActif(request.actif() == null || request.actif());
        return CongeDtos.PeriodeBloqueeDto.de(periodesBloquees.save(periode));
    }

    @Transactional
    public void supprimerPeriodeBloquee(Long id) {
        PeriodeBloquee periode = periodesBloquees.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Periode bloquee introuvable."));
        periodesBloquees.delete(periode);
    }

    // ------------------------------------------------------------------
    // Absences
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CongeDtos.AbsenceDto> absences(String statut) {
        return absences(parserStatutAbsence(statut));
    }

    private StatutAbsence parserStatutAbsence(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        try {
            return StatutAbsence.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_INVALIDE", "Statut d'absence inconnu : " + statut);
        }
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.AbsenceDto> absences(StatutAbsence statut) {
        return (statut == null ? absences.findAllByOrderByDateDebutDesc() : absences.findByStatutOrderByDateDebutDesc(statut))
                .stream().map(CongeDtos.AbsenceDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.AbsenceDto> absencesEmploye(Long idEmploye) {
        verifierEmploye(idEmploye);
        return absences.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(CongeDtos.AbsenceDto::de)
                .toList();
    }

    @Transactional
    public CongeDtos.AbsenceDto creerAbsence(CongeDtos.AbsenceRequest request) {
        verifierEmploye(request.idEmploye());
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin doit etre posterieure ou egale au debut.");
        }
        if (absences.existeChevauchement(request.idEmploye(), request.dateDebut(), request.dateFin(),
                List.of(StatutAbsence.A_JUSTIFIER, StatutAbsence.JUSTIFIEE))) {
            throw new BusinessException("CHEVAUCHEMENT", "Une absence existe deja sur cette periode.");
        }
        Absence absence = new Absence();
        absence.setIdEmploye(request.idEmploye());
        absence.setDateDebut(request.dateDebut());
        absence.setDateFin(request.dateFin());
        absence.setMotif(request.motif().trim());
        absence.setJustificatif(request.justificatif());
        return CongeDtos.AbsenceDto.de(absences.save(absence));
    }

    @Transactional
    public CongeDtos.AbsenceDto justifier(Long id, Long idValidateur, String justificatif) {
        Absence absence = absences.findById(id).orElseThrow(() -> BusinessException.notFound("Absence introuvable."));
        if (justificatif == null || justificatif.isBlank()) {
            throw new BusinessException("JUSTIFICATIF_REQUIS", "Un justificatif est requis.");
        }
        absence.setJustificatif(justificatif.trim());
        absence.setIdValidateur(idValidateur);
        absence.setStatut(StatutAbsence.JUSTIFIEE);
        return CongeDtos.AbsenceDto.de(absences.save(absence));
    }

    // ------------------------------------------------------------------
    // Lectures pour les autres modules et le planning
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CongeDtos.VariablePaieDto> variablesPaie() {
        Map<Long, String> noms = nomsTypes();
        return demandes.findByStatutInOrderByDateDebutAsc(statutsConsommes()).stream()
                .map(d -> new CongeDtos.VariablePaieDto(d.getIdEmploye(), d.getDateDebut(), d.getDateFin(),
                        d.getNombreJours(), noms.get(d.getIdTypeConge()), d.getMotif()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> planningConges() {
        Map<Long, String> noms = nomsTypes();
        return demandes.findByStatutInOrderByDateDebutAsc(statutsConsommes()).stream()
                .map(d -> CongeDtos.DemandeDto.de(d, noms.get(d.getIdTypeConge())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.PlanningDto> planningConges(Long idEmploye, LocalDate debut, LocalDate fin) {
        Map<Long, String> noms = nomsTypes();
        return demandes.findByStatutInOrderByDateDebutAsc(statutsConsommes()).stream()
                .filter(d -> d.getIdEmploye().equals(idEmploye))
                .filter(d -> !d.getDateDebut().isAfter(fin) && !d.getDateFin().isBefore(debut))
                .map(d -> new CongeDtos.PlanningDto(d.getIdEmploye(), d.getDateDebut(), d.getDateFin(),
                        d.getNombreJours(), noms.get(d.getIdTypeConge()), d.getMotif()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean estEnCongeValide(Long idEmploye, LocalDate date) {
        return !planningConges(idEmploye, date, date).isEmpty();
    }

    // ------------------------------------------------------------------
    // Acquisition et grand livre
    // ------------------------------------------------------------------

    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void acquerirMoisCourant() {
        LocalDate moisPrecedent = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        acquerirMois(moisPrecedent.getYear(), moisPrecedent.getMonthValue());
    }

    @Transactional
    public int acquerirMois(int annee, int mois) {
        LocalDate dateEffet = LocalDate.of(annee, mois, 1);
        BigDecimal plafond = plafond();
        int credits = 0;
        for (EmployeDto.Actif employe : employes.actifs()) {
            for (TypeConge type : types.findByActifTrueOrderByLibelleAsc()) {
                if (type.getJoursAcquisMois() == null || type.getJoursAcquisMois().signum() <= 0) {
                    continue;
                }
                if (mouvements.existsByIdEmployeAndIdTypeCongeAndTypeMouvementAndDateEffet(
                        employe.id(), type.getId(), TypeMouvementConge.ACQUISITION, dateEffet)) {
                    continue;
                }
                BigDecimal total = totalPourType(employe.id(), type.getId());
                BigDecimal credit = type.getJoursAcquisMois().min(plafond.subtract(total)).max(BigDecimal.ZERO);
                if (credit.signum() <= 0) {
                    continue;
                }
                SoldeConge solde = soldePour(employe.id(), type, annee);
                solde.setJoursAcquis(solde.getJoursAcquis().add(credit));
                soldes.save(solde);
                mouvements.save(mouvement(solde, null, TypeMouvementConge.ACQUISITION, credit,
                        dateEffet, "Acquisition mensuelle " + annee + "-" + String.format("%02d", mois)));
                credits++;
            }
        }
        return credits;
    }

    // ------------------------------------------------------------------
    // Solde et mouvements
    // ------------------------------------------------------------------

    private void reserver(DemandeConge demande, TypeConge type, BigDecimal jours) {
        BigDecimal plafond = plafond();
        BigDecimal total = totalPourType(demande.getIdEmploye(), type.getId());
        if (total.compareTo(plafond) > 0) {
            throw new BusinessException("PLAFOND_DEPASSE",
                    "Le solde cumule depasse le plafond de " + plafond + " jours.");
        }
        SoldeConge solde = soldePour(demande.getIdEmploye(), type, demande.getDateDebut().getYear());
        BigDecimal disponible = disponible(solde);
        if (disponible.compareTo(jours) < 0) {
            throw new BusinessException("SOLDE_INSUFFISANT",
                    "Le solde disponible (" + disponible.stripTrailingZeros().toPlainString()
                            + " j) est insuffisant pour cette demande (" + jours.stripTrailingZeros().toPlainString() + " j).");
        }
        solde.setJoursReserves(solde.getJoursReserves().add(jours));
        soldes.save(solde);
        mouvements.save(mouvement(solde, demande, TypeMouvementConge.RESERVATION,
                jours.negate(), demande.getDateDebut(), "Reservation a la soumission"));
    }

    private void libererReservation(DemandeConge demande, TypeConge type, String commentaire) {
        if (!type.isDecompteSolde()) {
            return;
        }
        SoldeConge solde = soldePour(demande.getIdEmploye(), type, demande.getDateDebut().getYear());
        BigDecimal liberation = demande.getNombreJours().min(solde.getJoursReserves());
        if (liberation.signum() <= 0) {
            return;
        }
        solde.setJoursReserves(solde.getJoursReserves().subtract(liberation));
        soldes.save(solde);
        mouvements.save(mouvement(solde, demande, TypeMouvementConge.LIBERATION,
                liberation, LocalDate.now(), commentaire));
    }

    private void consommerFifo(DemandeConge demande, TypeConge type) {
        BigDecimal reste = demande.getNombreJours();
        List<SoldeConge> lots = soldes.findByIdEmployeAndIdTypeCongeOrderByAnneeAsc(
                demande.getIdEmploye(), type.getId());
        BigDecimal totalDisponible = lots.stream()
                .map(s -> s.getJoursAcquis().subtract(s.getJoursPris()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDisponible.compareTo(reste) < 0) {
            throw new BusinessException("SOLDE_INSUFFISANT",
                    "Le solde disponible n'est plus suffisant pour approuver cette demande.");
        }
        for (SoldeConge lot : lots) {
            if (reste.signum() <= 0) {
                break;
            }
            BigDecimal dispoLot = lot.getJoursAcquis().subtract(lot.getJoursPris());
            if (dispoLot.signum() <= 0) {
                continue;
            }
            BigDecimal prise = dispoLot.min(reste);
            lot.setJoursPris(lot.getJoursPris().add(prise));
            soldes.save(lot);
            mouvements.save(mouvement(lot, demande, TypeMouvementConge.CONSOMMATION,
                    prise.negate(), demande.getDateDebut(), "Consommation FIFO"));
            reste = reste.subtract(prise);
        }
        if (reste.signum() > 0) {
            throw new BusinessException("SOLDE_INSUFFISANT", "Le solde disponible est insuffisant.");
        }
    }

    private void restituer(DemandeConge demande, TypeConge type, String commentaire) {
        restituer(demande, type, demande.getNombreJours(), commentaire);
    }

    private void restituer(DemandeConge demande, TypeConge type, BigDecimal jours, String commentaire) {
        if (jours.signum() <= 0) {
            return;
        }
        BigDecimal reste = jours;
        List<SoldeConge> lots = soldes.findByIdEmployeAndIdTypeCongeOrderByAnneeAsc(
                demande.getIdEmploye(), type.getId());
        for (int i = lots.size() - 1; i >= 0 && reste.signum() > 0; i--) {
            SoldeConge lot = lots.get(i);
            BigDecimal restitution = lot.getJoursPris().min(reste);
            if (restitution.signum() <= 0) {
                continue;
            }
            lot.setJoursPris(lot.getJoursPris().subtract(restitution));
            soldes.save(lot);
            mouvements.save(mouvement(lot, demande, TypeMouvementConge.LIBERATION,
                    restitution, LocalDate.now(), commentaire));
            reste = reste.subtract(restitution);
        }
    }

    private SoldeConge nouveauSolde(Long employe, TypeConge type, int annee) {
        DroitConge droit = droits.findByIdEmployeAndIdTypeCongeAndAnnee(employe, type.getId(), annee)
                .orElseGet(() -> droits.save(nouveauDroit(employe, type, annee)));
        SoldeConge solde = new SoldeConge();
        solde.setIdEmploye(employe);
        solde.setIdTypeConge(type.getId());
        solde.setAnnee(annee);
        solde.setIdDroitConge(droit.getId());
        solde.setJoursAcquis(droit.getJoursAutorises().add(droit.getReportEntrant()));
        return solde;
    }

    private DroitConge nouveauDroit(Long employe, TypeConge type, int annee) {
        DroitConge droit = new DroitConge();
        droit.setIdEmploye(employe);
        droit.setIdTypeConge(type.getId());
        droit.setAnnee(annee);
        droit.setJoursAutorises(BigDecimal.ZERO);
        return droit;
    }

    private SoldeConge soldePour(Long employe, TypeConge type, int annee) {
        return soldes.findByIdEmployeAndIdTypeCongeAndAnnee(employe, type.getId(), annee)
                .orElseGet(() -> soldes.save(nouveauSolde(employe, type, annee)));
    }

    private CongeDtos.SoldeDto soldeDto(SoldeConge s, TypeConge type, BigDecimal plafond, boolean alerte) {
        return new CongeDtos.SoldeDto(s.getId(), s.getIdEmploye(), s.getIdTypeConge(), type.getLibelle(),
                s.getAnnee(), s.getJoursAcquis(), s.getJoursPris(), s.getJoursReserves(),
                disponible(s), alerte, plafond);
    }

    private BigDecimal disponible(SoldeConge solde) {
        return solde.getJoursAcquis().subtract(solde.getJoursPris()).subtract(solde.getJoursReserves());
    }

    private BigDecimal totalPourType(Long idEmploye, Long idTypeConge) {
        return soldes.findByIdEmployeAndIdTypeCongeOrderByAnneeAsc(idEmploye, idTypeConge).stream()
                .map(s -> s.getJoursAcquis().subtract(s.getJoursPris()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPourEmploye(Long idEmploye) {
        return droits.findByIdEmployeOrderByAnneeAsc(idEmploye).stream()
                .map(d -> soldes.findByIdEmployeAndIdTypeCongeAndAnnee(
                        idEmploye, d.getIdTypeConge(), d.getAnnee())
                        .map(s -> s.getJoursAcquis().subtract(s.getJoursPris()))
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MouvementConge mouvement(SoldeConge solde, DemandeConge demande,
                                     TypeMouvementConge type, BigDecimal jours,
                                     LocalDate dateEffet, String commentaire) {
        MouvementConge mouvement = new MouvementConge();
        mouvement.setIdEmploye(solde.getIdEmploye());
        mouvement.setIdTypeConge(solde.getIdTypeConge());
        mouvement.setIdDroitConge(solde.getIdDroitConge());
        mouvement.setIdDemandeConge(demande == null ? null : demande.getId());
        mouvement.setTypeMouvement(type);
        mouvement.setJours(jours);
        mouvement.setDateEffet(dateEffet);
        mouvement.setCommentaire(commentaire);
        return mouvement;
    }

    // ------------------------------------------------------------------
    // Regles de calcul
    // ------------------------------------------------------------------

    private BigDecimal joursDecomptes(LocalDate debut, LocalDate fin,
                                      DemiJournee demiDebut, DemiJournee demiFin) {
        boolean ouvrables = MODE_OUVRABLES.equalsIgnoreCase(parametres.texte("MODE_DECOMPTE", MODE_OUVRABLES));
        long total = 0;
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            if (estJourDecompte(date, ouvrables)) {
                total++;
            }
        }
        BigDecimal jours = BigDecimal.valueOf(total);
        if (demiDebut == DemiJournee.APRES_MIDI && estJourDecompte(debut, ouvrables)) {
            jours = jours.subtract(new BigDecimal("0.5"));
        }
        if (demiFin == DemiJournee.MATIN && estJourDecompte(fin, ouvrables)) {
            jours = jours.subtract(new BigDecimal("0.5"));
        }
        return jours.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean estJourDecompte(LocalDate date, boolean ouvrables) {
        DayOfWeek jour = date.getDayOfWeek();
        if (jour == DayOfWeek.SUNDAY) {
            return false;
        }
        if (!ouvrables && jour == DayOfWeek.SATURDAY) {
            return false;
        }
        return !calendrier.estFerie(date);
    }

    private void verifierPrevenance(TypeConge type, BigDecimal jours, LocalDate debut) {
        int delaiType = type.getDelaiPrevenanceJours();
        int seuilDuree = parametres.entier("PREVENANCE_DUREE_JOURS", 3);
        int delai = delaiType > 0 ? delaiType : parametres.entier("PREVENANCE_JOURS", 7);
        if (delai > 0 && jours.compareTo(BigDecimal.valueOf(seuilDuree)) > 0
                && debut.isBefore(LocalDate.now().plusDays(delai))) {
            throw new BusinessException("PREVENANCE_INSUFFISANTE",
                    "Un conge de plus de " + seuilDuree + " jours doit etre demande au moins "
                            + delai + " jours calendaires avant le debut.");
        }
    }

    private void verifierPeriodeBloquee(LocalDate debut, LocalDate fin, boolean derogationDirection) {
        if (derogationDirection) {
            return;
        }
        List<PeriodeBloquee> periodes = periodesBloquees.chevauchant(debut, fin);
        if (!periodes.isEmpty()) {
            PeriodeBloquee periode = periodes.getFirst();
            throw new BusinessException("PERIODE_BLOQUEE",
                    "La periode recoupe un blocage RH : " + periode.getLibelle()
                            + " (du " + periode.getDateDebut() + " au " + periode.getDateFin() + ").");
        }
    }

    private void verifierDelaiDeclaration(TypeConge type, LocalDate debut) {
        if (!"MALADIE".equalsIgnoreCase(type.getCode())) {
            return;
        }
        int heures = parametres.entier("MALADIE_DECLARATION_HEURES", 48);
        if (LocalDateTime.now().isAfter(debut.atStartOfDay().plusHours(heures))) {
            throw new BusinessException("DELAI_DECLARATION_DEPASSE",
                    "Une maladie doit etre declaree dans les " + heures + " heures suivant le debut.");
        }
    }

    private void verifierJustificatif(TypeConge type, DemandeConge demande) {
        if (type.isJustificatifObligatoire() && demande.getJustificatifDocumentId() == null) {
            throw new BusinessException("JUSTIFICATIF_REQUIS",
                    "Un justificatif est obligatoire pour ce type de conge avant validation RH.");
        }
    }

    private void verifierEffectifMinimum(TypeConge type, Long idEmploye, LocalDate debut,
                                         LocalDate fin, BigDecimal jours, Long idRemplacant,
                                         Long idDemandeIgnoree) {
        if (List.of("MATERNITE", "PATERNITE").contains(type.getCode())) {
            return;
        }
        Long idService = employes.serviceDe(idEmploye).orElse(null);
        if (idService == null) {
            return;
        }
        List<EmployeDto.Actif> equipe = employes.actifsDuService(idService);
        int taille = equipe.size();
        if (taille <= 1) {
            return;
        }
        int minimum = effectifMinimum(taille);
        List<Long> idsEquipe = equipe.stream().map(EmployeDto.Actif::id).toList();
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            if (!estJourDecompte(date, MODE_OUVRABLES.equalsIgnoreCase(
                    parametres.texte("MODE_DECOMPTE", MODE_OUVRABLES)))) {
                continue;
            }
            long absents = 1;
            for (DemandeConge autre : demandes
                    .findByIdEmployeInAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                            idsEquipe, statutsActifs(), date, date)) {
                if (idDemandeIgnoree == null || !idDemandeIgnoree.equals(autre.getId())) {
                    absents++;
                }
            }
            for (Absence absence : absences
                    .findByIdEmployeInAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                            idsEquipe, List.of(StatutAbsence.A_JUSTIFIER, StatutAbsence.JUSTIFIEE), date, date)) {
                absents++;
            }
            int presents = (int) Math.max(0, taille - absents);
            if (presents < minimum && idRemplacant == null) {
                throw new BusinessException("EFFECTIF_MINIMUM",
                        "Le service conserverait " + presents + " presents le " + date
                                + " (minimum " + minimum + "). Un remplacant est requis.");
            }
            if (presents < minimum && idRemplacant != null) {
                suppleances.verifierEligible(idEmploye, jours, idRemplacant, debut, fin);
            }
        }
    }

    private int effectifMinimum(int taille) {
        String absolu = parametres.texte("EFFECTIF_MIN_SERVICE", "");
        if (absolu != null && !absolu.isBlank()) {
            try {
                int valeur = Integer.parseInt(absolu.trim());
                if (valeur > 0) {
                    return Math.min(valeur, taille);
                }
            } catch (NumberFormatException ignored) {
                // Repli sur le pourcentage ci-dessous.
            }
        }
        int pourcentage = parametres.entier("EFFECTIF_MIN_POURCENT", 60);
        return Math.max(1, (int) Math.ceil(taille * Math.max(0, Math.min(100, pourcentage)) / 100.0));
    }

    private List<StatutDemandeConge> statutsActifs() {
        return List.of(StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
                StatutDemandeConge.VALIDEE_RH, StatutDemandeConge.APPROUVEE,
                StatutDemandeConge.EN_COURS);
    }

    private void verifierChevauchements(Long idEmploye, LocalDate debut, LocalDate fin) {
        if (demandes.existeChevauchement(idEmploye, debut, fin,
                List.of(StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
                        StatutDemandeConge.VALIDEE_RH, StatutDemandeConge.APPROUVEE,
                        StatutDemandeConge.EN_COURS))) {
            throw new BusinessException("CHEVAUCHEMENT", "Une demande de conge existe deja sur cette periode.");
        }
        if (absences.existeChevauchement(idEmploye, debut, fin,
                List.of(StatutAbsence.A_JUSTIFIER, StatutAbsence.JUSTIFIEE))) {
            throw new BusinessException("CHEVAUCHEMENT", "Une absence existe deja sur cette periode.");
        }
    }

    private BigDecimal plafond() {
        return parametres.decimal("PLAFOND_SOLDE", new BigDecimal("90"));
    }

    private BigDecimal seuilAlerte() {
        return parametres.decimal("ALERTE_SOLDE", new BigDecimal("72"));
    }

    private int validationDirectionJours() {
        return parametres.entier("VALIDATION_DIRECTION_JOURS", 15);
    }

    private List<StatutDemandeConge> statutsConsommes() {
        return List.of(StatutDemandeConge.APPROUVEE, StatutDemandeConge.EN_COURS,
                StatutDemandeConge.TERMINEE, StatutDemandeConge.ECOURTEE);
    }

    private Map<Long, String> nomsTypes() {
        return types.findAll().stream().collect(Collectors.toMap(TypeConge::getId, TypeConge::getLibelle));
    }

    private java.util.Optional<Long> idEmployeDe(UtilisateurConnecte utilisateur) {
        return employes.idParUtilisateur(utilisateur.getId());
    }

    private void verifierEmploye(Long id) {
        if (!employes.existe(id)) throw BusinessException.notFound("Employe introuvable.");
    }
}
