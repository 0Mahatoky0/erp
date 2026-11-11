package com.tovo.erp.conges.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.model.*;
import com.tovo.erp.conges.repository.*;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CongeService {
    private final TypeCongeRepository types;
    private final SoldeCongeRepository soldes;
    private final DemandeCongeRepository demandes;
    private final AbsenceRepository absences;
    private final EmployeRepository employes;

    public CongeService(TypeCongeRepository types, SoldeCongeRepository soldes,
                        DemandeCongeRepository demandes, AbsenceRepository absences,
                        EmployeRepository employes) {
        this.types = types;
        this.soldes = soldes;
        this.demandes = demandes;
        this.absences = absences;
        this.employes = employes;
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.TypeDto> types() {
        return types.findByActifTrueOrderByLibelleAsc().stream().map(CongeDtos.TypeDto::de).toList();
    }

    @Transactional
    public List<CongeDtos.SoldeDto> soldes(Long idEmploye, int annee) {
        verifierEmploye(idEmploye);
        Map<Long, SoldeConge> existants = soldes.findByIdEmployeAndAnnee(idEmploye, annee).stream()
                .collect(Collectors.toMap(SoldeConge::getIdTypeConge, Function.identity()));
        return types.findByActifTrueOrderByLibelleAsc().stream().map(type -> {
            SoldeConge solde = existants.get(type.getId());
            if (solde == null) {
                solde = nouveauSolde(idEmploye, type, annee);
                solde = soldes.save(solde);
            }
            return soldeDto(solde, type);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> demandes(StatutDemandeConge statut) {
        Map<Long, String> noms = types.findAll().stream().collect(Collectors.toMap(TypeConge::getId, TypeConge::getLibelle));
        var liste = statut == null ? demandes.findAllByOrderByDateCreationDesc()
                : demandes.findByStatutOrderByDateCreationDesc(statut);
        return liste.stream().map(d -> CongeDtos.DemandeDto.de(d, noms.get(d.getIdTypeConge()))).toList();
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.DemandeDto> demandesEmploye(Long idEmploye) {
        verifierEmploye(idEmploye);
        Map<Long, String> noms = types.findAll().stream()
                .collect(Collectors.toMap(TypeConge::getId, TypeConge::getLibelle));
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
        long jours = joursOuvres(request.dateDebut(), request.dateFin());
        if (jours == 0) throw new BusinessException("DUREE_INVALIDE", "La periode ne contient aucun jour ouvrable.");
        if (demandes.existeChevauchement(request.idEmploye(), request.dateDebut(), request.dateFin(),
                List.of(StatutDemandeConge.EN_ATTENTE, StatutDemandeConge.VALIDEE))) {
            throw new BusinessException("CHEVAUCHEMENT", "Une demande de conge existe deja sur cette periode.");
        }
        SoldeConge solde = soldes.findByIdEmployeAndIdTypeCongeAndAnnee(request.idEmploye(), type.getId(), request.dateDebut().getYear())
                .orElseGet(() -> soldes.save(nouveauSolde(request.idEmploye(), type, request.dateDebut().getYear())));
        BigDecimal nombre = BigDecimal.valueOf(jours);
        if (type.getJoursAcquisMois().signum() > 0
                && solde.getJoursAcquis().subtract(solde.getJoursPris()).compareTo(nombre) < 0) {
            throw new BusinessException("SOLDE_INSUFFISANT", "Le solde disponible est insuffisant pour cette demande.");
        }
        DemandeConge demande = new DemandeConge();
        demande.setIdEmploye(request.idEmploye());
        demande.setIdTypeConge(type.getId());
        demande.setDateDebut(request.dateDebut());
        demande.setDateFin(request.dateFin());
        demande.setNombreJours(nombre);
        demande.setMotif(request.motif().trim());
        return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
    }

    @Transactional
    public CongeDtos.DemandeDto decision(Long id, CongeDtos.DecisionRequest request, boolean valider) {
        DemandeConge demande = demandes.findById(id).orElseThrow(() -> BusinessException.notFound("Demande de conge introuvable."));
        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new BusinessException("DEMANDE_DEJA_TRAITEE", "Cette demande a deja ete traitee.");
        }
        TypeConge type = types.findById(demande.getIdTypeConge()).orElseThrow();
        demande.setStatut(valider ? StatutDemandeConge.VALIDEE : StatutDemandeConge.REFUSEE);
        demande.setIdValidateur(request == null ? null : request.idValidateur());
        demande.setMotifDecision(request == null ? null : request.motif());
        demande.setDateDecision(LocalDateTime.now());
        if (valider && type.getJoursAcquisMois().signum() > 0) {
            SoldeConge solde = soldes.findByIdEmployeAndIdTypeCongeAndAnnee(
                    demande.getIdEmploye(), type.getId(), demande.getDateDebut().getYear())
                    .orElseGet(() -> soldes.save(nouveauSolde(demande.getIdEmploye(), type, demande.getDateDebut().getYear())));
            solde.setJoursPris(solde.getJoursPris().add(demande.getNombreJours()));
            soldes.save(solde);
        }
        return CongeDtos.DemandeDto.de(demandes.save(demande), type.getLibelle());
    }

    @Transactional(readOnly = true)
    public List<CongeDtos.AbsenceDto> absences(StatutAbsence statut) {
        return (statut == null ? absences.findAllByOrderByDateDebutDesc() : absences.findByStatutOrderByDateDebutDesc(statut))
                .stream().map(CongeDtos.AbsenceDto::de).toList();
    }

    @Transactional
    public CongeDtos.AbsenceDto creerAbsence(CongeDtos.AbsenceRequest request) {
        verifierEmploye(request.idEmploye());
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin doit etre posterieure ou egale au debut.");
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

    private SoldeConge nouveauSolde(Long employe, TypeConge type, int annee) {
        SoldeConge solde = new SoldeConge();
        solde.setIdEmploye(employe);
        solde.setIdTypeConge(type.getId());
        solde.setAnnee(annee);
        int mois = annee < LocalDate.now().getYear() ? 12 : LocalDate.now().getMonthValue();
        solde.setJoursAcquis(type.getJoursAcquisMois().multiply(BigDecimal.valueOf(mois)).setScale(2, RoundingMode.HALF_UP));
        return solde;
    }

    private CongeDtos.SoldeDto soldeDto(SoldeConge s, TypeConge type) {
        return new CongeDtos.SoldeDto(s.getId(), s.getIdEmploye(), s.getIdTypeConge(), type.getLibelle(),
                s.getAnnee(), s.getJoursAcquis(), s.getJoursPris(), s.getJoursAcquis().subtract(s.getJoursPris()));
    }

    private void verifierEmploye(Long id) {
        if (!employes.existsById(id)) throw BusinessException.notFound("Employe introuvable.");
    }

    private long joursOuvres(LocalDate debut, LocalDate fin) {
        long total = 0;
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) total++;
        }
        return total;
    }
}
