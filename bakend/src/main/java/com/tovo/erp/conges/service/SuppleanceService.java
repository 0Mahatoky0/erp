package com.tovo.erp.conges.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.model.*;
import com.tovo.erp.conges.repository.*;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Matrice de remplacement et file FIFO des suppleances (RG-B7-22 a RG-B7-28). */
@Service
public class SuppleanceService {

    private static final List<StatutDemandeConge> STATUTS_INDISPONIBLES = List.of(
            StatutDemandeConge.SOUMISE, StatutDemandeConge.VALIDEE_MANAGER,
            StatutDemandeConge.VALIDEE_RH, StatutDemandeConge.APPROUVEE,
            StatutDemandeConge.EN_COURS);
    private static final List<StatutAbsence> STATUTS_ABSENCE = List.of(
            StatutAbsence.A_JUSTIFIER, StatutAbsence.JUSTIFIEE);
    private static final List<StatutSuppleance> STATUTS_SUPPLEANCE_ACTIVE = List.of(
            StatutSuppleance.PROPOSEE, StatutSuppleance.ACCEPTEE, StatutSuppleance.EN_COURS);

    private final MatriceRemplacementRepository matrices;
    private final SuppleanceRepository suppleances;
    private final DemandeCongeRepository demandes;
    private final AbsenceRepository absences;
    private final EmployeService employes;
    private final ParametreCongeService parametres;

    public SuppleanceService(MatriceRemplacementRepository matrices, SuppleanceRepository suppleances,
                             DemandeCongeRepository demandes, AbsenceRepository absences,
                             EmployeService employes, ParametreCongeService parametres) {
        this.matrices = matrices;
        this.suppleances = suppleances;
        this.demandes = demandes;
        this.absences = absences;
        this.employes = employes;
        this.parametres = parametres;
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.MatriceRemplacementDto> matrice() {
        return matrices.findAll().stream().map(CongeDtos.MatriceRemplacementDto::de).toList();
    }

    @Transactional
    public CongeDtos.MatriceRemplacementDto enregistrerMatrice(CongeDtos.MatriceRemplacementRequest request) {
        if (request.idPosteRemplace() == null && request.idEmployeRemplace() == null) {
            throw new BusinessException("CIBLE_REQUISE", "Indiquez un poste ou un employe a remplacer.");
        }
        if (request.idPosteRemplacant() == null && request.idEmployeRemplacant() == null) {
            throw new BusinessException("REMPLACANT_REQUIS", "Indiquez un poste ou un employe remplacant.");
        }
        if (request.dureeMinJours() != null && request.dureeMaxJours() != null
                && request.dureeMinJours() > request.dureeMaxJours()) {
            throw new BusinessException("DUREE_INVALIDE", "La duree minimale depasse la duree maximale.");
        }
        MatriceRemplacement matrice = new MatriceRemplacement();
        matrice.setIdPosteRemplace(request.idPosteRemplace());
        matrice.setIdEmployeRemplace(request.idEmployeRemplace());
        matrice.setIdPosteRemplacant(request.idPosteRemplacant());
        matrice.setIdEmployeRemplacant(request.idEmployeRemplacant());
        matrice.setNiveau(request.niveau() == null ? NiveauRemplacement.PRIMAIRE : request.niveau());
        matrice.setDureeMinJours(request.dureeMinJours());
        matrice.setDureeMaxJours(request.dureeMaxJours());
        matrice.setCompetencesRequises(request.competencesRequises());
        matrice.setHabilitations(request.habilitations());
        matrice.setActif(request.actif() == null || request.actif());
        return CongeDtos.MatriceRemplacementDto.de(matrices.save(matrice));
    }

    @Transactional(readOnly = true)
    public boolean existeActivePourDemande(Long idDemande) {
        return suppleances.findByIdDemandeCongeOrderByDateCreationDesc(idDemande).stream()
                .anyMatch(s -> s.getStatut().estActive());
    }

    @Transactional(readOnly = true)
    public void verifierEligible(Long idEmployeAbsent, BigDecimal dureeJours, Long idRemplacant,
                                 LocalDate debut, LocalDate fin) {
        if (idRemplacant == null) {
            throw new BusinessException("REMPLACANT_REQUIS",
                    "Un remplacant est requis pour maintenir l'effectif minimum du service.");
        }
        if (candidats(idEmployeAbsent, dureeJours, debut, fin).stream()
                .noneMatch(c -> c.employe().id().equals(idRemplacant))) {
            throw new BusinessException("REMPLACANT_INELIGIBLE",
                    "Le remplacant propose ne respecte pas la matrice, la duree ou ses disponibilites.");
        }
    }

    @Transactional
    public CongeDtos.SuppleanceDto proposer(Long idDemande, CongeDtos.SuppleanceRequest request) {
        DemandeConge demande = demandes.findById(idDemande)
                .orElseThrow(() -> BusinessException.notFound("Demande de conge introuvable."));
        if (!demande.getStatut().estActive()) {
            throw new BusinessException("SUPPLEANCE_INTERDITE",
                    "Une suppleance ne peut etre proposee que pour une demande active.");
        }
        if (existeActivePourDemande(idDemande)) {
            throw new BusinessException("SUPPLEANCE_EXISTANTE", "Une suppleance active existe deja.");
        }
        List<Candidat> eligibles = candidats(demande.getIdEmploye(), demande.getNombreJours(),
                demande.getDateDebut(), demande.getDateFin());
        Candidat choisi;
        if (request != null && request.idRemplacant() != null) {
            choisi = eligibles.stream()
                    .filter(c -> c.employe().id().equals(request.idRemplacant()))
                    .findFirst()
                    .orElseGet(() -> candidatForce(request.idRemplacant(), demande));
        } else {
            choisi = eligibles.stream().findFirst().orElse(null);
        }
        if (choisi == null) {
            throw new BusinessException("AUCUN_REMPLACANT",
                    "Aucun remplacant eligible : alerte manager/RH, demande a maintenir bloquee.");
        }
        return creer(demande, choisi.employe(), request == null ? null : request.motif());
    }

    @Transactional
    public CongeDtos.SuppleanceDto repondre(Long id, CongeDtos.ReponseSuppleanceRequest request) {
        Suppleance suppleance = suppleances.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Suppleance introuvable."));
        if (suppleance.getStatut() != StatutSuppleance.PROPOSEE) {
            throw new BusinessException("SUPPLEANCE_INVALIDE", "Cette suppleance n'est plus en attente de reponse.");
        }
        StatutSuppleance cible;
        try {
            cible = StatutSuppleance.valueOf(request.statut().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_INVALIDE", "Statut de suppleance inconnu.");
        }
        if (cible != StatutSuppleance.ACCEPTEE && cible != StatutSuppleance.REFUSEE) {
            throw new BusinessException("STATUT_INVALIDE", "Reponse attendue : ACCEPTEE ou REFUSEE.");
        }
        if (cible == StatutSuppleance.ACCEPTEE) {
            verifierDisponible(suppleance.getIdEmployeRemplacant(), suppleance.getDateDebut(),
                    suppleance.getDateFin(), suppleance.getId());
        }
        suppleance.setStatut(cible);
        suppleance.setMotif(request.motif() == null ? null : request.motif().trim());
        suppleance.setDateReponse(LocalDateTime.now());
        Suppleance enregistree = suppleances.save(suppleance);
        if (cible == StatutSuppleance.ACCEPTEE && enregistree.getIdDemandeConge() != null) {
            Long idRemplacant = enregistree.getIdEmployeRemplacant();
            demandes.findById(enregistree.getIdDemandeConge()).ifPresent(d -> {
                d.setIdRemplacant(idRemplacant);
                demandes.save(d);
            });
        }
        return CongeDtos.SuppleanceDto.de(enregistree);
    }

    @Transactional
    public CongeDtos.SuppleanceDto terminer(Long id) {
        Suppleance suppleance = suppleances.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Suppleance introuvable."));
        if (suppleance.getStatut() != StatutSuppleance.ACCEPTEE
                && suppleance.getStatut() != StatutSuppleance.EN_COURS) {
            throw new BusinessException("SUPPLEANCE_INVALIDE", "Seule une suppleance acceptee peut etre terminee.");
        }
        suppleance.setStatut(StatutSuppleance.TERMINEE);
        suppleance.setDateReponse(LocalDateTime.now());
        return CongeDtos.SuppleanceDto.de(suppleances.save(suppleance));
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.SuppleanceDto> toutes() {
        return suppleances.findAllByOrderByDateDebutDesc().stream()
                .map(CongeDtos.SuppleanceDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.SuppleanceDto> deDemande(Long idDemande) {
        return suppleances.findByIdDemandeCongeOrderByDateCreationDesc(idDemande).stream()
                .map(CongeDtos.SuppleanceDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.SuppleanceDto> deRemplacant(Long idEmploye) {
        return suppleances.findByIdEmployeRemplacantAndStatutIn(idEmploye, STATUTS_SUPPLEANCE_ACTIVE)
                .stream().map(CongeDtos.SuppleanceDto::de).toList();
    }

    private CongeDtos.SuppleanceDto creer(DemandeConge demande, EmployeDto.Actif remplacant, String motif) {
        Suppleance suppleance = new Suppleance();
        suppleance.setIdEmployeAbsent(demande.getIdEmploye());
        suppleance.setIdEmployeRemplacant(remplacant.id());
        suppleance.setIdDemandeConge(demande.getId());
        suppleance.setDateDebut(demande.getDateDebut());
        suppleance.setDateFin(demande.getDateFin());
        suppleance.setMotif(motif == null ? null : motif.trim());
        suppleance.setStatut(StatutSuppleance.PROPOSEE);
        return CongeDtos.SuppleanceDto.de(suppleances.save(suppleance));
    }

    private Candidat candidatForce(Long idRemplacant, DemandeConge demande) {
        EmployeDto.Actif employe = employes.actifs().stream()
                .filter(e -> e.id().equals(idRemplacant))
                .findFirst()
                .orElseThrow(() -> new BusinessException("REMPLACANT_INELIGIBLE",
                        "Le remplacant force n'est pas un salarie actif."));
        verifierDisponible(idRemplacant, demande.getDateDebut(), demande.getDateFin(), null);
        return new Candidat(employe, NiveauRemplacement.EXCEPTIONNEL,
                suppleances.derniereDateFin(idRemplacant, STATUTS_SUPPLEANCE_ACTIVE).orElse(null));
    }

    private List<Candidat> candidats(Long idEmployeAbsent, BigDecimal dureeJours,
                                     LocalDate debut, LocalDate fin) {
        Long idPoste = employes.posteDe(idEmployeAbsent).orElse(null);
        List<MatriceRemplacement> lignes = matrices.pourRemplace(idPoste, idEmployeAbsent);
        double duree = dureeJours == null ? 0 : dureeJours.doubleValue();
        Map<Long, Candidat> retenus = new LinkedHashMap<>();
        for (MatriceRemplacement ligne : lignes) {
            if (duree > 0 && ligne.getDureeMinJours() != null && duree < ligne.getDureeMinJours()) {
                continue;
            }
            if (duree > 0 && ligne.getDureeMaxJours() != null && duree > ligne.getDureeMaxJours()) {
                continue;
            }
            for (EmployeDto.Actif candidat : candidatsDeLaLigne(ligne)) {
                if (candidat.id().equals(idEmployeAbsent) || !disponible(candidat.id(), debut, fin)) {
                    continue;
                }
                Candidat existant = retenus.get(candidat.id());
                LocalDate derniere = suppleances.derniereDateFin(candidat.id(), STATUTS_SUPPLEANCE_ACTIVE)
                        .orElse(null);
                Candidat nouveau = new Candidat(candidat, ligne.getNiveau(), derniere);
                if (existant == null || nouveau.niveau().ordinal() < existant.niveau().ordinal()) {
                    retenus.put(candidat.id(), nouveau);
                }
            }
        }
        return new ArrayList<>(retenus.values()).stream()
                .sorted(Comparator.comparing((Candidat c) -> c.niveau().ordinal())
                        .thenComparing(Candidat::derniereSuppleance,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(c -> c.employe().dateEmbauche(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.employe().id()))
                .toList();
    }

    private List<EmployeDto.Actif> candidatsDeLaLigne(MatriceRemplacement ligne) {
        if (ligne.getIdEmployeRemplacant() != null) {
            return employes.actifs().stream()
                    .filter(e -> e.id().equals(ligne.getIdEmployeRemplacant()))
                    .toList();
        }
        return employes.actifsDuPoste(ligne.getIdPosteRemplacant());
    }

    private boolean disponible(Long idRemplacant, LocalDate debut, LocalDate fin) {
        try {
            verifierDisponible(idRemplacant, debut, fin, null);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void verifierDisponible(Long idRemplacant, LocalDate debut, LocalDate fin, Long idSuppleanceIgnoree) {
        long chevauchements = suppleances.countChevauchement(idRemplacant, debut, fin, STATUTS_SUPPLEANCE_ACTIVE);
        if (idSuppleanceIgnoree != null) {
            Optional<Suppleance> ignoree = suppleances.findById(idSuppleanceIgnoree);
            if (ignoree.isPresent() && ignoree.get().getStatut().estActive()
                    && !ignoree.get().getDateFin().isBefore(debut)
                    && !ignoree.get().getDateDebut().isAfter(fin)) {
                chevauchements--;
            }
        }
        int maximum = Math.max(1, parametres.entier("SUPPLEANCE_MAX_SIMULTANEE", 1));
        if (chevauchements >= maximum) {
            throw new BusinessException("REMPLACANT_OCCUPE",
                    "Le remplacant a deja une suppleance sur cette periode.");
        }
        if (demandes.existeChevauchement(idRemplacant, debut, fin, STATUTS_INDISPONIBLES)) {
            throw new BusinessException("REMPLACANT_INDISPONIBLE",
                    "Le remplacant est en conge sur cette periode.");
        }
        if (absences.existeChevauchement(idRemplacant, debut, fin, STATUTS_ABSENCE)) {
            throw new BusinessException("REMPLACANT_INDISPONIBLE",
                    "Le remplacant est absent sur cette periode.");
        }
    }

    private record Candidat(EmployeDto.Actif employe, NiveauRemplacement niveau,
                            LocalDate derniereSuppleance) {
    }
}
