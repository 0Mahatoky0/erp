package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.AffectationStatut;
import com.tovo.erp.contrat.model.Avenant;
import com.tovo.erp.contrat.model.AvenantEtat;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.MotifAffectation;
import com.tovo.erp.contrat.model.TypeAvenant;
import com.tovo.erp.contrat.repository.AffectationRepository;
import com.tovo.erp.contrat.repository.AvenantRepository;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Avenants aux contrats signes (RG-B4-04, RG-B4-05).
 * <p>Un avenant ne modifie jamais le contrat d'origine : il conserve les
 * valeurs avant/apres et n'applique les changements qu'a sa date d'effet.
 */
@Service
public class AvenantService {

    private final AvenantRepository avenants;
    private final ContratRepository contrats;
    private final AffectationRepository affectations;

    public AvenantService(AvenantRepository avenants, ContratRepository contrats,
                          AffectationRepository affectations) {
        this.avenants = avenants;
        this.contrats = contrats;
        this.affectations = affectations;
    }

    @Transactional
    public ContratDtos.AvenantDto creer(Long idContrat, ContratDtos.AvenantRequest demande) {
        if (demande == null || demande.type() == null || demande.dateEffet() == null) {
            throw new BusinessException("AVENANT_INCOMPLET",
                    "Le type et la date d'effet de l'avenant sont obligatoires.");
        }
        Contrat contrat = contratModifiable(idContrat);
        if (demande.dateEffet().isBefore(contrat.getDateDebut())) {
            throw new BusinessException("AVENANT_DATE_INVALIDE",
                    "La date d'effet ne peut pas preceder le debut du contrat.");
        }
        validerValeurs(contrat, demande);

        long numero = avenants.findByIdContratOrderByIdAsc(idContrat).size() + 1;
        Avenant avenant = new Avenant();
        avenant.setIdContrat(idContrat);
        avenant.setNumero("AV-" + contrat.getNumero() + "-" + String.format("%02d", numero));
        avenant.setType(demande.type());
        avenant.setDateEffet(demande.dateEffet());
        avenant.setAnciennesValeurs(demande.anciennesValeurs() == null
                ? valeursActuelles(contrat, demande.type()) : demande.anciennesValeurs());
        avenant.setNouvellesValeurs(demande.nouvellesValeurs() == null
                ? valeursDemandees(demande) : demande.nouvellesValeurs());
        avenant.setNouveauSalaire(demande.nouveauSalaire());
        avenant.setNouvelleDateFin(demande.nouvelleDateFin());
        avenant.setNouveauxHoraires(demande.nouveauxHoraires());
        avenant.setNouveauLieu(demande.nouveauLieu());
        avenant.setNouveauPosteId(demande.nouveauPosteId());
        avenant.setNouveauDepartementId(demande.nouveauDepartementId());
        avenant.setNouveauServiceId(demande.nouveauServiceId());
        avenant.setNouveauManagerId(demande.nouveauManagerId());
        avenant.setIdDocument(demande.idDocument());
        avenant.setEtat(AvenantEtat.CREE);
        return ContratDtos.AvenantDto.de(avenants.save(avenant));
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.AvenantDto> avenantsDe(Long idContrat) {
        return avenants.findByIdContratOrderByIdAsc(idContrat).stream()
                .map(ContratDtos.AvenantDto::de)
                .toList();
    }

    /**
     * Signature de l'avenant. Les changements ne sont appliques que lorsque la
     * date d'effet est atteinte (RG-B4-04).
     */
    @Transactional
    public ContratDtos.AvenantDto signer(Long idAvenant, Long idSignataire) {
        Avenant avenant = avenants.findById(idAvenant)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'avenant " + idAvenant + " n'existe pas."));
        if (avenant.getEtat() == AvenantEtat.SIGNE || avenant.getEtat() == AvenantEtat.VALIDE) {
            return ContratDtos.AvenantDto.de(avenant);
        }
        Contrat contrat = contratModifiable(avenant.getIdContrat());
        avenant.setEtat(AvenantEtat.SIGNE);
        avenant.setDateSignature(LocalDateTime.now());
        avenant.setSignataireId(idSignataire);
        if (!avenant.getDateEffet().isAfter(LocalDate.now())) {
            appliquer(contrat, avenant);
            avenant.setDateApplication(LocalDateTime.now());
            contrats.save(contrat);
        }
        return ContratDtos.AvenantDto.de(avenants.save(avenant));
    }

    /** Applique les avenants signes dont la date d'effet est arrivee. */
    @Transactional
    public int appliquerEcheances() {
        LocalDate aujourdHui = LocalDate.now();
        int appliques = 0;
        for (Avenant avenant : avenants.findAll()) {
            if (avenant.getEtat() != AvenantEtat.SIGNE
                    || avenant.getDateApplication() != null
                    || avenant.getDateEffet().isAfter(aujourdHui)) {
                continue;
            }
            Contrat contrat = contrats.findById(avenant.getIdContrat()).orElse(null);
            if (contrat == null) {
                continue;
            }
            appliquer(contrat, avenant);
            avenant.setDateApplication(LocalDateTime.now());
            contrats.save(contrat);
            avenants.save(avenant);
            appliques++;
        }
        return appliques;
    }

    @Transactional
    public ContratDtos.AvenantDto valider(Long idAvenant) {
        Avenant avenant = avenants.findById(idAvenant)
                .orElseThrow(() -> BusinessException.notFound(
                        "L'avenant " + idAvenant + " n'existe pas."));
        if (avenant.getEtat() != AvenantEtat.SIGNE) {
            throw new BusinessException("AVENANT_NON_SIGNE",
                    "Seul un avenant signe peut etre valide.");
        }
        avenant.setEtat(AvenantEtat.VALIDE);
        return ContratDtos.AvenantDto.de(avenants.save(avenant));
    }

    private Contrat contratModifiable(Long idContrat) {
        Contrat contrat = contrats.findById(idContrat)
                .orElseThrow(() -> BusinessException.notFound(
                        "Le contrat " + idContrat + " n'existe pas."));
        if (contrat.getEtat() != ContratEtat.SIGNE && contrat.getEtat() != ContratEtat.VALIDE) {
            throw new BusinessException("CONTRAT_NON_MODIFIABLE",
                    "Un avenant ne peut concerner qu'un contrat signe ou valide.");
        }
        return contrat;
    }

    private void validerValeurs(Contrat contrat, ContratDtos.AvenantRequest demande) {
        switch (demande.type()) {
            case SALAIRE -> {
                if (demande.nouveauSalaire() == null) {
                    throw new BusinessException("AVENANT_INCOMPLET",
                            "Le nouveau salaire est obligatoire pour un avenant de salaire.");
                }
            }
            case DUREE -> {
                if (demande.nouvelleDateFin() == null) {
                    throw new BusinessException("AVENANT_INCOMPLET",
                            "La nouvelle date de fin est obligatoire pour un avenant de duree.");
                }
                if (demande.nouvelleDateFin().isBefore(contrat.getDateDebut())) {
                    throw new BusinessException("AVENANT_DATE_INVALIDE",
                            "La nouvelle date de fin ne peut pas preceder le debut du contrat.");
                }
            }
            case HORAIRES -> {
                if (demande.nouveauxHoraires() == null || demande.nouveauxHoraires().isBlank()) {
                    throw new BusinessException("AVENANT_INCOMPLET",
                            "Les nouveaux horaires sont obligatoires pour un avenant d'horaires.");
                }
            }
            case LIEU -> {
                if (demande.nouveauLieu() == null || demande.nouveauLieu().isBlank()) {
                    throw new BusinessException("AVENANT_INCOMPLET",
                            "Le nouveau lieu est obligatoire pour un avenant de lieu.");
                }
            }
            case POSTE -> {
                if (demande.nouveauPosteId() == null) {
                    throw new BusinessException("AVENANT_INCOMPLET",
                            "Le nouveau poste est obligatoire pour un avenant de poste.");
                }
            }
            default -> {
            }
        }
    }

    private void appliquer(Contrat contrat, Avenant avenant) {
        switch (avenant.getType()) {
            case SALAIRE -> contrat.setSalaireBase(avenant.getNouveauSalaire());
            case DUREE -> contrat.setDateFinPrevue(avenant.getNouvelleDateFin());
            case HORAIRES -> contrat.setHoraires(avenant.getNouveauxHoraires());
            case LIEU -> contrat.setLieuTravail(avenant.getNouveauLieu());
            case POSTE -> appliquerPoste(contrat, avenant);
            default -> {
            }
        }
    }

    /** Cree l'affectation correspondant au nouveau poste et clot la precedente. */
    private void appliquerPoste(Contrat contrat, Avenant avenant) {
        List<Affectation> existantes =
                affectations.findByIdContratOrderByDateDebutDesc(contrat.getId());
        Affectation modele = existantes.isEmpty() ? null : existantes.get(0);
        for (Affectation ancienne : existantes) {
            if (!ancienne.isPrincipale()
                    || ancienne.getStatut() == AffectationStatut.ANNULEE
                    || ancienne.getStatut() == AffectationStatut.TERMINEE) {
                continue;
            }
            ancienne.setDateFin(avenant.getDateEffet().minusDays(1));
            if (!avenant.getDateEffet().isAfter(LocalDate.now())) {
                ancienne.setStatut(AffectationStatut.TERMINEE);
                ancienne.setPrincipale(false);
            }
            affectations.save(ancienne);
        }
        Affectation nouvelle = new Affectation();
        nouvelle.setIdEmploye(contrat.getIdEmploye());
        nouvelle.setIdContrat(contrat.getId());
        nouvelle.setIdPoste(avenant.getNouveauPosteId());
        nouvelle.setIdDepartement(avenant.getNouveauDepartementId() != null
                ? avenant.getNouveauDepartementId()
                : (modele == null ? null : modele.getIdDepartement()));
        nouvelle.setIdService(avenant.getNouveauServiceId() != null
                ? avenant.getNouveauServiceId()
                : (modele == null ? null : modele.getIdService()));
        nouvelle.setIdManager(avenant.getNouveauManagerId() != null
                ? avenant.getNouveauManagerId()
                : (modele == null ? null : modele.getIdManager()));
        nouvelle.setLieuTravail(avenant.getNouveauLieu() != null
                ? avenant.getNouveauLieu()
                : (modele == null ? contrat.getLieuTravail() : modele.getLieuTravail()));
        nouvelle.setHoraires(avenant.getNouveauxHoraires() != null
                ? avenant.getNouveauxHoraires()
                : (modele == null ? contrat.getHoraires() : modele.getHoraires()));
        nouvelle.setDateDebut(avenant.getDateEffet());
        nouvelle.setTauxActivite(modele == null ? 100 : modele.getTauxActivite());
        nouvelle.setPrincipale(true);
        nouvelle.setMotif(MotifAffectation.MUTATION);
        nouvelle.setStatut(avenant.getDateEffet().isAfter(LocalDate.now())
                ? AffectationStatut.PLANIFIEE : AffectationStatut.ACTIVE);
        affectations.save(nouvelle);
    }

    private String valeursActuelles(Contrat contrat, TypeAvenant type) {
        return switch (type) {
            case SALAIRE -> "salaireBase=" + contrat.getSalaireBase();
            case DUREE -> "dateFinPrevue=" + contrat.getDateFinPrevue();
            case HORAIRES -> "horaires=" + contrat.getHoraires();
            case LIEU -> "lieuTravail=" + contrat.getLieuTravail();
            case POSTE -> "poste actuel (affectation courante)";
            case AUTRE -> "valeurs actuelles conservees";
        };
    }

    private String valeursDemandees(ContratDtos.AvenantRequest demande) {
        return switch (demande.type()) {
            case SALAIRE -> "salaireBase=" + demande.nouveauSalaire();
            case DUREE -> "dateFinPrevue=" + demande.nouvelleDateFin();
            case HORAIRES -> "horaires=" + demande.nouveauxHoraires();
            case LIEU -> "lieuTravail=" + demande.nouveauLieu();
            case POSTE -> demande.nouvellesValeurs() == null
                    ? "posteId=" + demande.nouveauPosteId() : demande.nouvellesValeurs();
            case AUTRE -> demande.nouvellesValeurs() == null
                    ? "modification diverse" : demande.nouvellesValeurs();
        };
    }
}
