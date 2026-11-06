package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Avenant;
import com.tovo.erp.contrat.model.AvenantEtat;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.TypeAvenant;
import com.tovo.erp.contrat.repository.AvenantRepository;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Avenants aux contrats signes (RG-B4-05).
 * <p>Un avenant ne modifie jamais le contrat d'origine : il conserve les
 * valeurs avant/apres et applique les changements a sa signature.
 */
@Service
public class AvenantService {

    private final AvenantRepository avenants;
    private final ContratRepository contrats;

    public AvenantService(AvenantRepository avenants, ContratRepository contrats) {
        this.avenants = avenants;
        this.contrats = contrats;
    }

    @Transactional
    public ContratDtos.AvenantDto creer(Long idContrat, ContratDtos.AvenantRequest demande) {
        Contrat contrat = contratModifiable(idContrat);
        if (demande.dateEffet().isBefore(contrat.getDateDebut())) {
            throw new BusinessException("AVENANT_DATE_INVALIDE",
                    "La date d'effet ne peut pas precéder le debut du contrat.");
        }
        validerValeurs(demande);

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
        appliquer(contrat, avenant);
        contrats.save(contrat);
        return ContratDtos.AvenantDto.de(avenants.save(avenant));
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

    private void validerValeurs(ContratDtos.AvenantRequest demande) {
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
            default -> {
            }
        }
    }

    private void appliquer(Contrat contrat, Avenant avenant) {
        switch (avenant.getType()) {
            case SALAIRE -> contrat.setSalaireBase(avenant.getNouveauSalaire());
            case DUREE -> {
                if (avenant.getNouvelleDateFin().isBefore(contrat.getDateDebut())) {
                    throw new BusinessException("AVENANT_DATE_INVALIDE",
                            "La nouvelle date de fin ne peut pas preceder le debut du contrat.");
                }
                contrat.setDateFinPrevue(avenant.getNouvelleDateFin());
            }
            case HORAIRES -> contrat.setHoraires(avenant.getNouveauxHoraires());
            case LIEU -> contrat.setLieuTravail(avenant.getNouveauLieu());
            default -> {
            }
        }
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
                    ? "nouveau poste a preciser" : demande.nouvellesValeurs();
            case AUTRE -> demande.nouvellesValeurs() == null
                    ? "modification diverse" : demande.nouvellesValeurs();
        };
    }
}
