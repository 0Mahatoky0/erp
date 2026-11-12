package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.dto.DocumentEmbaucheRequest;
import com.tovo.erp.employe.dto.DossierEmbaucheRequest;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.model.ConfidentialiteDocument;
import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.PosteDetailVue;
import com.tovo.erp.employe.model.StatutDocument;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.repository.DocumentEmployeRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.employe.repository.PosteDetailVueRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Recherche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consultation des employes (RM-057 a RM-060) et gestion du dossier cree
 * a l'acceptation d'une proposition d'embauche (RG-B3-06 a RG-B3-08).
 */
@Service
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final DocumentEmployeRepository documentRepository;
    private final PosteDetailVueRepository postes;

    public EmployeService(EmployeRepository employeRepository,
                          DocumentEmployeRepository documentRepository,
                          PosteDetailVueRepository postes) {
        this.employeRepository = employeRepository;
        this.documentRepository = documentRepository;
        this.postes = postes;
    }

    /** Vrai si le dossier salarie existe. */
    @Transactional(readOnly = true)
    public boolean existe(Long idEmploye) {
        return idEmploye != null && employeRepository.existsById(idEmploye);
    }

    /** Identifiant du dossier salarie rattache a un compte utilisateur. */
    @Transactional(readOnly = true)
    public Optional<Long> idParUtilisateur(Long idUtilisateur) {
        return employeRepository.findFirstByIdUtilisateur(idUtilisateur).map(Employe::getId);
    }

    /** Salaries actifs, references minimales pour les traitements transverses. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Actif> actifs() {
        return employeRepository.findByStatutOrderByNomAscPrenomAsc(StatutEmploye.ACTIF).stream()
                .map(EmployeDto.Actif::de)
                .toList();
    }

    /** Service de rattachement d'un salarie, sans exposer les entites aux autres modules. */
    @Transactional(readOnly = true)
    public Optional<Long> serviceDe(Long idEmploye) {
        return employeRepository.findById(idEmploye)
                .map(Employe::getIdPoste)
                .flatMap(idPoste -> postes.findById(idPoste))
                .map(PosteDetailVue::getIdService);
    }

    /** Poste de rattachement d'un salarie. */
    @Transactional(readOnly = true)
    public Optional<Long> posteDe(Long idEmploye) {
        return employeRepository.findById(idEmploye).map(Employe::getIdPoste);
    }

    /** Salaries actifs d'un service, avec leur poste et leur anciennete. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Actif> actifsDuService(Long idService) {
        if (idService == null) {
            return List.of();
        }
        List<Employe> liste = employeRepository.findByStatutAndIdService(StatutEmploye.ACTIF, idService);
        Map<Long, PosteDetailVue> details = detailsPostes(liste);
        return liste.stream().map(e -> EmployeDto.Actif.de(e, details.get(e.getIdPoste()))).toList();
    }

    /** Salaries actifs occupant un poste donne. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Actif> actifsDuPoste(Long idPoste) {
        if (idPoste == null) {
            return List.of();
        }
        List<Employe> liste = employeRepository.findByStatutAndIdPoste(StatutEmploye.ACTIF, idPoste);
        Map<Long, PosteDetailVue> details = detailsPostes(liste);
        return liste.stream().map(e -> EmployeDto.Actif.de(e, details.get(e.getIdPoste()))).toList();
    }

    /** Liste des employes filtres par statut, service et recherche textuelle. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Liste> lister(String statut, Long idService, String recherche) {
        StatutEmploye filtre = statut == null || statut.isBlank()
                ? null : StatutEmploye.valueOf(statut.toUpperCase(java.util.Locale.ROOT));
        List<Employe> employes = employeRepository.rechercher(filtre, idService, Recherche.motif(recherche));
        Map<Long, PosteDetailVue> details = detailsPostes(employes);
        return employes.stream()
                .map(e -> EmployeDto.Liste.de(e, details.get(e.getIdPoste())))
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
        employe.setIdPoste(idPoste);
        return versDto(employeRepository.save(employe));
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

    /**
     * Attribue le matricule au salarie une seule fois (RG-B4-06). Un matricule
     * deja present (reembauche) n'est jamais remplace.
     */
    @Transactional
    public String attribuerMatricule(Long idEmploye, java.util.function.Supplier<String> generateur) {
        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas."));
        if (employe.getMatricule() == null || employe.getMatricule().isBlank()) {
            employe.setMatricule(generateur.get());
            employeRepository.save(employe);
        }
        return employe.getMatricule();
    }

    /** Met a jour le statut administratif du salarie (activation, depart...). */
    @Transactional
    public EmployeEmbaucheDto changerStatut(Long idEmploye, StatutEmploye statut) {
        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'employe " + idEmploye + " n'existe pas."));
        employe.setStatut(statut);
        return versDto(employeRepository.save(employe));
    }

    /** Active le salarie sans exposer l'enumeration de statut aux autres modules. */
    @Transactional
    public void activer(Long idEmploye) {
        changerStatut(idEmploye, StatutEmploye.ACTIF);
    }

    /** Desactive le salarie sans exposer l'enumeration de statut aux autres modules. */
    @Transactional
    public void desactiver(Long idEmploye) {
        changerStatut(idEmploye, StatutEmploye.INACTIF);
    }

    /** Marque une non-presentation a la prise de poste (RG-B4-17). */
    @Transactional
    public void marquerNonPrisPoste(Long idEmploye) {
        changerStatut(idEmploye, StatutEmploye.NON_PRIS_POSTE);
    }

    /** Identifiants des salaries encore en pre-embauche, en une seule requete. */
    @Transactional(readOnly = true)
    public Set<Long> idsEnPreEmbauche(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return employeRepository.findByIdInAndStatut(ids, StatutEmploye.PRE_EMBAUCHE).stream()
                .map(Employe::getId)
                .collect(Collectors.toSet());
    }

    /** Enregistre le PDF contractuel sans faire dependre le module contrat de l'enumeration. */
    @Transactional
    public DocumentEmbaucheDto enregistrerDocumentContrat(Long idEmploye, String titre,
                                                          String nomFichier, String typeMime,
                                                          byte[] contenu, Long idContrat,
                                                          LocalDate dateEmission) {
        return enregistrerDocument(idEmploye, TypeDocumentEmploye.CONTRAT, titre,
                nomFichier, typeMime, contenu, idContrat, dateEmission, null);
    }

    /** Dossiers par identifiant, en une seule requete inter-modules. */
    @Transactional(readOnly = true)
    public Map<Long, EmployeEmbaucheDto> dossiersParIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Employe> employes = employeRepository.findAllById(ids);
        Map<Long, PosteDetailVue> postesParId = detailsPostes(employes);
        Map<Long, EmployeEmbaucheDto> resultat = new HashMap<>();
        for (Employe employe : employes) {
            resultat.put(employe.getId(), EmployeEmbaucheDto.de(employe, postesParId.get(employe.getIdPoste())));
        }
        return resultat;
    }

    public EmployeEmbaucheDto dossier(Long idEmploye) {
        return versDto(employeRepository.findById(idEmploye)
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
        return employeRepository.findFirstByIdCandidat(idCandidat).map(this::versDto);
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

    private EmployeEmbaucheDto versDto(Employe employe) {
        PosteDetailVue poste = employe.getIdPoste() == null ? null
                : postes.findById(employe.getIdPoste()).orElse(null);
        return EmployeEmbaucheDto.de(employe, poste);
    }

    private Map<Long, PosteDetailVue> detailsPostes(Collection<Employe> employes) {
        Set<Long> ids = employes.stream()
                .map(Employe::getIdPoste)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PosteDetailVue> details = new HashMap<>();
        for (PosteDetailVue poste : postes.findAllById(ids)) {
            details.put(poste.getId(), poste);
        }
        return details;
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
