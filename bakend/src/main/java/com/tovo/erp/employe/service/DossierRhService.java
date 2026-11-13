package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.AlerteDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheRequest;
import com.tovo.erp.employe.dto.DossierRhDto;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutDocument;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.repository.DocumentEmployeRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Point d'entree RH du dossier administratif : agrege les donnees du module
 * employe, calcule la completude, rafraichit les alertes et journalise chaque
 * consultation ou modification. Le front complete la fiche avec l'API contrat.
 */
@Service
public class DossierRhService {

    private static final Set<TypeDocumentEmploye> PIECES_OBLIGATOIRES = Set.of(
            TypeDocumentEmploye.PIECE_IDENTITE,
            TypeDocumentEmploye.DIPLOME,
            TypeDocumentEmploye.RIB,
            TypeDocumentEmploye.CONTRAT);

    private final EmployeRepository employes;
    private final DocumentEmployeRepository documents;
    private final EmployeService employeService;
    private final AlerteService alertes;
    private final MissionService missions;
    private final DemandeModificationService demandes;
    private final JournalAuditService audit;

    public DossierRhService(EmployeRepository employes,
                            DocumentEmployeRepository documents,
                            EmployeService employeService,
                            AlerteService alertes,
                            MissionService missions,
                            DemandeModificationService demandes,
                            JournalAuditService audit) {
        this.employes = employes;
        this.documents = documents;
        this.employeService = employeService;
        this.alertes = alertes;
        this.missions = missions;
        this.demandes = demandes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EmployeDto.Liste> lister(String statut, Long idService, String recherche) {
        return employeService.lister(statut, idService, recherche);
    }

    @Transactional
    public DossierRhDto dossierComplet(Long idEmploye) {
        Employe employe = exigerEmploye(idEmploye);
        EmployeEmbaucheDto fiche = employeService.dossier(idEmploye);
        List<DocumentEmbaucheDto> pieces = employeService.documents(idEmploye);
        List<AlerteDto> alertesCalculees = alertes.recalculer(idEmploye);
        List<String> manquantes = piecesManquantes(employe, pieces);
        int completude = completude(employe, pieces);

        audit.enregistrer("CONSULTATION_DOSSIER", "employe", idEmploye, null, null);

        return new DossierRhDto(fiche, pieces, alertesCalculees,
                missions.lister(idEmploye), demandes.lister(idEmploye),
                completude, manquantes);
    }

    @Transactional
    public DocumentEmbaucheDto ajouterDocument(Long idEmploye, DocumentEmbaucheRequest requete) {
        exigerEmploye(idEmploye);
        DocumentEmbaucheDto document = employeService.enregistrerDocument(idEmploye, requete);
        alertes.recalculer(idEmploye);
        audit.enregistrer("AJOUT_DOCUMENT", "employe", idEmploye, null,
                document.typeDocument() + " : " + document.titre());
        return document;
    }

    @Transactional
    public DocumentEmbaucheDto changerStatutDocument(Long idEmploye, Long idDocument,
                                                     String statut) {
        exigerEmploye(idEmploye);
        StatutDocument cible;
        try {
            cible = StatutDocument.valueOf(statut == null ? "" : statut.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("STATUT_DOCUMENT_INVALIDE",
                    "Le statut de document « " + statut + " » n'existe pas.");
        }
        DocumentEmploye document = documents.findById(idDocument)
                .orElseThrow(() -> BusinessException.notFound(
                        "Le document " + idDocument + " n'existe pas."));
        if (document.getEmploye() == null || !idEmploye.equals(document.getEmploye().getId())) {
            throw BusinessException.notFound("Le document " + idDocument
                    + " n'appartient pas au dossier " + idEmploye + ".");
        }
        StatutDocument avant = document.getStatut();
        document.setStatut(cible);
        DocumentEmploye enregistre = documents.save(document);
        alertes.recalculer(idEmploye);
        audit.enregistrer("STATUT_DOCUMENT", "document_employe", idDocument,
                avant.name(), cible.name());
        return DocumentEmbaucheDto.de(enregistre);
    }

    public int completude(Employe employe, List<DocumentEmbaucheDto> pieces) {
        int score = 0;
        for (TypeDocumentEmploye type : PIECES_OBLIGATOIRES) {
            boolean presente = pieces.stream().anyMatch(piece -> piece.typeDocument() == type
                    && piece.statut() != StatutDocument.REFUSE
                    && piece.statut() != StatutDocument.ARCHIVE);
            if (presente) {
                score += 20;
            }
        }
        if (employe.getEmail() != null && !employe.getEmail().isBlank()) {
            score += 5;
        }
        if (employe.getTelephone() != null && !employe.getTelephone().isBlank()) {
            score += 5;
        }
        if (employe.getAdresse() != null && !employe.getAdresse().isBlank()) {
            score += 5;
        }
        if (employe.getDateNaissance() != null) {
            score += 5;
        }
        return Math.min(score, 100);
    }

    public List<String> piecesManquantes(Employe employe, List<DocumentEmbaucheDto> pieces) {
        List<String> manquantes = new ArrayList<>();
        for (TypeDocumentEmploye type : PIECES_OBLIGATOIRES) {
            boolean presente = pieces.stream().anyMatch(piece -> piece.typeDocument() == type
                    && piece.statut() != StatutDocument.REFUSE
                    && piece.statut() != StatutDocument.ARCHIVE);
            if (!presente) {
                manquantes.add(libelle(type));
            }
        }
        return manquantes;
    }

    private String libelle(TypeDocumentEmploye type) {
        return switch (type) {
            case PIECE_IDENTITE -> "Piece d'identite";
            case DIPLOME -> "Diplome";
            case RIB -> "RIB";
            case CONTRAT -> "Contrat";
            case FICHE_POSTE -> "Fiche de poste";
            case CERTIFICAT_MEDICAL -> "Certificat medical";
            case ATTESTATION -> "Attestation";
            case AUTRE -> "Autre piece";
        };
    }

    private Employe exigerEmploye(Long idEmploye) {
        return employes.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas."));
    }
}
