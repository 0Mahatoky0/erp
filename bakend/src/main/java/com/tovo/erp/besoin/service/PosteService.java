package com.tovo.erp.service;

import com.tovo.erp.common.BusinessException;
import com.tovo.erp.common.Recherche;
import com.tovo.erp.domain.entity.Poste;
import com.tovo.erp.demande.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.repository.PosteRepository;
import com.tovo.erp.repository.ServiceRepository;
import com.tovo.erp.web.dto.PosteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogue des postes officiels (RM-006 a RM-011).
 *
 * <p>Un poste deja reference par l'historique n'est jamais supprime : il est desactive.
 */
@Service
public class PosteService {

    private final PosteRepository posteRepository;
    private final ServiceRepository serviceRepository;
    private final DemandeRecrutementPosteRepository ligneRepository;

    public PosteService(PosteRepository posteRepository,
                        ServiceRepository serviceRepository,
                        DemandeRecrutementPosteRepository ligneRepository) {
        this.posteRepository = posteRepository;
        this.serviceRepository = serviceRepository;
        this.ligneRepository = ligneRepository;
    }

    /** Liste des postes filtres par service, etat actif et recherche textuelle. */
    @Transactional(readOnly = true)
    public List<PosteDto.Liste> lister(Long idService, Boolean actif, String recherche) {
        return posteRepository.rechercher(idService, actif, Recherche.motif(recherche)).stream()
                .map(poste -> PosteDto.Liste.de(poste, ligneRepository.countByPosteId(poste.getId())))
                .toList();
    }

    /** Cree un poste officiel (role RH). */
    @Transactional
    public PosteDto.Liste creer(PosteDto.Creation demande) {
        com.tovo.erp.domain.entity.Service service = serviceRepository.findById(demande.idService())
                .orElseThrow(() -> BusinessException.notFound(
                        "Le service " + demande.idService() + " n'existe pas."));
        controlerDoublon(service, demande.intitule(), null);
        controlerSalaires(demande.salaireMin(), demande.salaireMax());

        Poste poste = new Poste();
        poste.setService(service);
        poste.setIntitule(demande.intitule().trim());
        poste.setDescription(demande.description());
        poste.setSalaireMin(demande.salaireMin());
        poste.setSalaireMax(demande.salaireMax());
        poste.setActif(true);
        posteRepository.save(poste);
        return PosteDto.Liste.de(poste, 0);
    }

    /** Modifie un poste officiel et gere son activation (RM-011). */
    @Transactional
    public PosteDto.Liste modifier(Long id, PosteDto.Modification demande) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Le poste " + id + " n'existe pas."));
        controlerDoublon(poste.getService(), demande.intitule(), poste.getId());
        controlerSalaires(demande.salaireMin(), demande.salaireMax());

        poste.setIntitule(demande.intitule().trim());
        poste.setDescription(demande.description());
        poste.setSalaireMin(demande.salaireMin());
        poste.setSalaireMax(demande.salaireMax());
        poste.setActif(demande.actif());
        posteRepository.save(poste);
        return PosteDto.Liste.de(poste, ligneRepository.countByPosteId(poste.getId()));
    }

    /** Desactive un poste : il reste visible dans l'historique (RM-011). */
    @Transactional
    public PosteDto.Liste desactiver(Long id) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Le poste " + id + " n'existe pas."));
        poste.setActif(false);
        posteRepository.save(poste);
        return PosteDto.Liste.de(poste, ligneRepository.countByPosteId(poste.getId()));
    }

    /**
     * RM-010 : refuse la creation d'un poste equivalent dans le meme service.
     * La comparaison ignore la casse et les espaces superflus.
     */
    private void controlerDoublon(com.tovo.erp.domain.entity.Service service, String intitule, Long idPosteExclu) {
        List<Poste> equivalents = posteRepository.findEquivalents(service.getId(), intitule.trim(), idPosteExclu);
        if (!equivalents.isEmpty()) {
            throw new BusinessException("POSTE_DOUBLON",
                    "Un poste intitule '" + intitule.trim() + "' existe deja dans le service "
                            + service.getNom() + ".");
        }
    }

    private void controlerSalaires(BigDecimal min, BigDecimal max) {
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire minimum doit etre positif ou nul.");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire maximum doit etre positif ou nul.");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessException("SALAIRE_INVALIDE",
                    "Le salaire maximum doit etre superieur ou egal au salaire minimum.");
        }
    }

}
