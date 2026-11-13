package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.AlerteDto;
import com.tovo.erp.employe.model.Alerte;
import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.Mission;
import com.tovo.erp.employe.model.NiveauAlerte;
import com.tovo.erp.employe.model.StatutDocument;
import com.tovo.erp.employe.model.StatutMission;
import com.tovo.erp.employe.model.TypeAlerte;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.repository.AlerteRepository;
import com.tovo.erp.employe.repository.DocumentEmployeRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.employe.repository.MissionRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Calcule et stocke les alertes du dossier administratif : pieces expirant ou
 * expirees, pieces obligatoires manquantes, approche de l'age de retraite et
 * missions en retard (bloc 5). Le calcul est idempotent et declenche par une
 * tache planifiee ; l'ecran RH ne fait que lire les alertes.
 */
@Service
public class AlerteService {

    private static final Set<TypeAlerte> ALERTES_CALCULEES = EnumSet.of(
            TypeAlerte.DOCUMENT_EXPIRE,
            TypeAlerte.DOCUMENT_EXPIRANT,
            TypeAlerte.DOCUMENT_MANQUANT,
            TypeAlerte.RETRAITE_PROCHE,
            TypeAlerte.MISSION_EN_RETARD);

    private static final List<TypeDocumentEmploye> PIECES_OBLIGATOIRES = List.of(
            TypeDocumentEmploye.PIECE_IDENTITE,
            TypeDocumentEmploye.DIPLOME,
            TypeDocumentEmploye.RIB,
            TypeDocumentEmploye.CONTRAT);

    private final AlerteRepository alertes;
    private final EmployeRepository employes;
    private final DocumentEmployeRepository documents;
    private final MissionRepository missions;

    private final int joursAlerteDocument;
    private final int joursAlerteRetraite;
    private final int ageRetraite;

    public AlerteService(AlerteRepository alertes,
                         EmployeRepository employes,
                         DocumentEmployeRepository documents,
                         MissionRepository missions,
                         @Value("${erp.employe.alerte-document-jours:30}") int joursAlerteDocument,
                         @Value("${erp.employe.alerte-retraite-jours:180}") int joursAlerteRetraite,
                         @Value("${erp.employe.age-retraite:60}") int ageRetraite) {
        this.alertes = alertes;
        this.employes = employes;
        this.documents = documents;
        this.missions = missions;
        this.joursAlerteDocument = joursAlerteDocument;
        this.joursAlerteRetraite = joursAlerteRetraite;
        this.ageRetraite = ageRetraite;
    }

    @Transactional(readOnly = true)
    public List<AlerteDto> lister(Long idEmploye) {
        exigerEmploye(idEmploye);
        return alertes.findByIdEmployeOrderByNiveauDescDateEcheanceAsc(idEmploye).stream()
                .map(AlerteDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlerteDto> listerToutes() {
        return alertes.findAllByOrderByDateCreationDesc().stream()
                .map(AlerteDto::de)
                .toList();
    }

    @Transactional
    public AlerteDto marquerLue(Long idAlerte) {
        Alerte alerte = alertes.findById(idAlerte)
                .orElseThrow(() -> BusinessException.notFound("L'alerte " + idAlerte + " n'existe pas."));
        alerte.setLue(true);
        return AlerteDto.de(alertes.save(alerte));
    }

    @Transactional
    public List<AlerteDto> recalculer(Long idEmploye) {
        Employe employe = employes.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas."));
        alertes.deleteByIdEmployeAndTypeAlerteIn(idEmploye, ALERTES_CALCULEES);
        alertes.flush();

        LocalDate aujourdhui = LocalDate.now();
        List<Alerte> nouvelles = new ArrayList<>();
        List<DocumentEmploye> pieces = documents.findByEmployeIdOrderByDateCreationDesc(idEmploye);

        for (DocumentEmploye piece : pieces) {
            if (piece.getStatut() == StatutDocument.ARCHIVE || piece.getDateValidite() == null) {
                continue;
            }
            if (piece.getDateValidite().isBefore(aujourdhui)) {
                if (piece.getStatut() != StatutDocument.EXPIRE) {
                    piece.setStatut(StatutDocument.EXPIRE);
                    documents.save(piece);
                }
                nouvelles.add(alerte(employe.getId(), TypeAlerte.DOCUMENT_EXPIRE,
                        NiveauAlerte.CRITIQUE,
                        "Le document « " + piece.getTitre() + " » est expire depuis le "
                                + piece.getDateValidite() + ".",
                        piece.getDateValidite()));
            } else if (!piece.getDateValidite().isAfter(aujourdhui.plusDays(joursAlerteDocument))) {
                nouvelles.add(alerte(employe.getId(), TypeAlerte.DOCUMENT_EXPIRANT,
                        NiveauAlerte.AVERTISSEMENT,
                        "Le document « " + piece.getTitre() + " » expire le "
                                + piece.getDateValidite() + ".",
                        piece.getDateValidite()));
            }
        }

        for (TypeDocumentEmploye type : PIECES_OBLIGATOIRES) {
            boolean presente = pieces.stream().anyMatch(piece -> piece.getTypeDocument() == type
                    && piece.getStatut() != StatutDocument.ARCHIVE
                    && piece.getStatut() != StatutDocument.REFUSE);
            if (!presente) {
                nouvelles.add(alerte(employe.getId(), TypeAlerte.DOCUMENT_MANQUANT,
                        NiveauAlerte.INFO,
                        "Piece obligatoire manquante : " + libelle(type) + ".",
                        aujourdhui));
            }
        }

        if (employe.getDateNaissance() != null) {
            LocalDate retraite = employe.getDateNaissance().plusYears(ageRetraite);
            if (!retraite.isAfter(aujourdhui.plusDays(joursAlerteRetraite))) {
                nouvelles.add(alerte(employe.getId(), TypeAlerte.RETRAITE_PROCHE,
                        retraite.isBefore(aujourdhui) ? NiveauAlerte.CRITIQUE : NiveauAlerte.AVERTISSEMENT,
                        "Age de retraite (" + ageRetraite + " ans) atteint le " + retraite + ".",
                        retraite));
            }
        }

        for (Mission mission : missions.findByIdEmployeOrderByDateDebutDesc(idEmploye)) {
            if (mission.getDateFinPrevue() != null
                    && mission.getDateFinPrevue().isBefore(aujourdhui)
                    && mission.getStatut() != StatutMission.TERMINEE
                    && mission.getStatut() != StatutMission.ANNULEE) {
                nouvelles.add(alerte(employe.getId(), TypeAlerte.MISSION_EN_RETARD,
                        NiveauAlerte.AVERTISSEMENT,
                        "La mission « " + mission.getTitre() + " » devait se terminer le "
                                + mission.getDateFinPrevue() + ".",
                        mission.getDateFinPrevue()));
            }
        }

        return alertes.saveAll(nouvelles).stream().map(AlerteDto::de).toList();
    }

    @Transactional
    public int recalculerToutes() {
        int total = 0;
        for (Employe employe : employes.findAll()) {
            total += recalculer(employe.getId()).size();
        }
        return total;
    }

    private Alerte alerte(Long idEmploye, TypeAlerte type, NiveauAlerte niveau,
                          String message, LocalDate echeance) {
        Alerte alerte = new Alerte();
        alerte.setIdEmploye(idEmploye);
        alerte.setTypeAlerte(type);
        alerte.setNiveau(niveau);
        alerte.setMessage(message);
        alerte.setDateEcheance(echeance);
        return alerte;
    }

    private void exigerEmploye(Long idEmploye) {
        if (idEmploye == null || !employes.existsById(idEmploye)) {
            throw BusinessException.notFound("L'employe " + idEmploye + " n'existe pas.");
        }
    }

    private String libelle(TypeDocumentEmploye type) {
        return switch (type) {
            case PIECE_IDENTITE -> "piece d'identite";
            case DIPLOME -> "diplome";
            case RIB -> "RIB";
            case CONTRAT -> "contrat";
            case FICHE_POSTE -> "fiche de poste";
            case CERTIFICAT_MEDICAL -> "certificat medical";
            case ATTESTATION -> "attestation";
            case AUTRE -> "autre piece";
        };
    }
}
