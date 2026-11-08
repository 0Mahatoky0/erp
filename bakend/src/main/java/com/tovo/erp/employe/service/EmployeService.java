package com.tovo.erp.employe.service;

import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.dto.DocumentEmbaucheRequest;
import com.tovo.erp.employe.dto.DossierEmbaucheRequest;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.ConfidentialiteDocument;
import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutDocument;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.repository.DocumentEmployeRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.iam.dto.EmployeDto;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Recherche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Consultation des employes (RM-057 a RM-060) et gestion du dossier cree
 * a l'acceptation d'une proposition d'embauche (RG-B3-06 a RG-B3-08).
 */
@Service
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final DocumentEmployeRepository documentRepository;
    private final PosteRepository posteRepository;

    public EmployeService(EmployeRepository employeRepository,
                          DocumentEmployeRepository documentRepository,
                          PosteRepository posteRepository) {
        this.employeRepository = employeRepository;
        this.documentRepository = documentRepository;
        this.posteRepository = posteRepository;
    }

    /** Liste des employes filtres par statut, service et recherche textuelle. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Liste> lister(StatutEmploye statut, Long idService, String recherche) {
        return employeRepository.rechercher(statut, idService, Recherche.motif(recherche)).stream()
                .map(EmployeDto.Liste::de)
                .toList();
    }

    /**
     * Cree le dossier PRE_EMBAUCHE ou reprend un dossier existant (RG-B3-08).
     * <p>Un employe encore ACTIF n'est jamais duplique : l'acceptation est refusee.
     */
    @Transactional
    public EmployeEmbaucheDto creerOuReactiverDossier(DossierEmbaucheRequest demande,
                                                       Long idCandidat,
                                                       Long idPoste,
                                                       LocalDate dateEmbauche) {
        String email = normaliserEmail(demande.email());
        String cin = normaliserCin(demande.cin());
        Employe existant = trouverDoublon(demande, email, cin);
        if (existant != null && existant.getStatut() == StatutEmploye.ACTIF) {
            throw new BusinessException("EMPLOYE_DEJA_ACTIF",
                    "Un employe actif existe deja pour " + email + " (matricule "
                            + existant.getMatricule() + ").");
        }

        Employe employe = existant == null ? new Employe() : existant;
        employe.setNom(normaliser(demande.nom()));
        employe.setPrenom(normaliser(demande.prenom()));
        employe.setEmail(email);
        employe.setTelephone(normaliser(demande.telephone()));
        employe.setIdCandidat(idCandidat);
        employe.setCin(cin);
        employe.setDateNaissance(demande.dateNaissance());
        employe.setLieuNaissance(normaliser(demande.lieuNaissance()));
        employe.setAdresse(normaliser(demande.adresse()));
        employe.setNationalite(normaliser(demande.nationalite()));
        employe.setSituationFamiliale(normaliser(demande.situationFamiliale()));
        employe.setDateEmbauche(dateEmbauche);
        if (demande.datePremiereEmbauche() != null) {
            employe.setDatePremiereEmbauche(demande.datePremiereEmbauche());
        } else if (employe.getDatePremiereEmbauche() == null) {
            employe.setDatePremiereEmbauche(dateEmbauche);
        }
        employe.setStatut(StatutEmploye.PRE_EMBAUCHE);
        if (idPoste != null) {
            Poste poste = posteRepository.findById(idPoste)
                    .orElseThrow(() -> BusinessException.notFound("Le poste " + idPoste + " n'existe pas."));
            employe.setPoste(poste);
        }
        return EmployeEmbaucheDto.de(employeRepository.save(employe));
    }

    /** Depose une piece dans le dossier salarie a partir d'un contenu base64. */
    @Transactional
    public DocumentEmbaucheDto enregistrerDocument(Long idEmploye, DocumentEmbaucheRequest demande) {
        byte[] contenu;
        try {
            contenu = Base64.getDecoder().decode(demande.contenuBase64());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("DOCUMENT_INVALIDE",
                    "Le contenu du document n'est pas un base64 valide.");
        }
        return enregistrerDocument(idEmploye, demande.typeDocument(), demande.titre(),
                demande.nomFichier(), demande.typeMime(), contenu, null,
                demande.dateEmission(), demande.dateValidite());
    }

    @Transactional
    public DocumentEmbaucheDto enregistrerDocument(Long idEmploye,
                                                   TypeDocumentEmploye type,
                                                   String titre,
                                                   String nomFichier,
                                                   String typeMime,
                                                   byte[] contenu,
                                                   Long idContrat,
                                                   LocalDate dateEmission,
                                                   LocalDate dateValidite) {
        if (contenu == null || contenu.length == 0) {
            throw new BusinessException("DOCUMENT_VIDE", "Le document ne contient aucune donnee.");
        }
        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound("L'employe " + idEmploye + " n'existe pas."));
        DocumentEmploye document = new DocumentEmploye();
        document.setEmploye(employe);
        document.setIdContrat(idContrat);
        document.setTypeDocument(type);
        document.setTitre(titre);
        document.setNomFichier(nomFichier);
        document.setTypeMime(typeMime == null || typeMime.isBlank() ? "application/pdf" : typeMime);
        document.setContenu(contenu);
        document.setHashSha256(empreinte(contenu));
        document.setStatut(StatutDocument.VALIDE);
        document.setConfidentialite(ConfidentialiteDocument.CONFIDENTIEL);
        document.setDateEmission(dateEmission);
        document.setDateValidite(dateValidite);
        return DocumentEmbaucheDto.de(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public EmployeEmbaucheDto dossier(Long idEmploye) {
        return EmployeEmbaucheDto.de(employeRepository.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas.")));
    }

    @Transactional(readOnly = true)
    public List<DocumentEmbaucheDto> documents(Long idEmploye) {
        return documentRepository.findByEmployeIdOrderByDateCreationDesc(idEmploye).stream()
                .map(DocumentEmbaucheDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentEmbaucheFichier contenuDocument(Long idDocument) {
        DocumentEmploye document = documentRepository.findById(idDocument)
                .orElseThrow(() -> BusinessException.notFound("Le document " + idDocument + " n'existe pas."));
        return new DocumentEmbaucheFichier(document.getNomFichier(), document.getTypeMime(),
                document.getContenu());
    }

    @Transactional(readOnly = true)
    public Optional<EmployeEmbaucheDto> parIdCandidat(Long idCandidat) {
        return employeRepository.findFirstByIdCandidat(idCandidat).map(EmployeEmbaucheDto::de);
    }

    private Employe trouverDoublon(DossierEmbaucheRequest demande, String email, String cin) {
        if (email != null && !email.isBlank()) {
            Optional<Employe> parEmail = employeRepository.findFirstByEmailIgnoreCase(email);
            if (parEmail.isPresent()) {
                return parEmail.get();
            }
        }
        if (cin != null && !cin.isBlank()) {
            Optional<Employe> parCin = employeRepository.findFirstByCinIgnoreCase(cin);
            if (parCin.isPresent()) {
                return parCin.get();
            }
        }
        if (demande.dateNaissance() != null && demande.nom() != null && demande.prenom() != null) {
            return employeRepository.findFirstByNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissance(
                    normaliser(demande.nom()), normaliser(demande.prenom()), demande.dateNaissance())
                    .orElse(null);
        }
        return null;
    }

    private String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }

    private static String normaliser(String valeur) {
        return valeur == null ? null : valeur.trim();
    }

    private static String normaliserEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normaliserCin(String cin) {
        return cin == null ? null : cin.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
