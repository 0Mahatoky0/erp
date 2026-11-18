package com.tovo.erp.evaluation.service;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.model.CampagneEvaluation;
import com.tovo.erp.evaluation.model.Objectif;
import com.tovo.erp.evaluation.repository.CampagneEvaluationRepository;
import com.tovo.erp.evaluation.repository.ObjectifRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CampagneService {
    private static final int MIN_OBJECTIFS = 3;
    private static final int MAX_OBJECTIFS = 7;

    private final CampagneEvaluationRepository campagnes;
    private final ObjectifRepository objectifs;
    private final EmployeService employes;
    private final JournalAuditService audit;

    public CampagneService(CampagneEvaluationRepository campagnes, ObjectifRepository objectifs,
                           EmployeService employes, JournalAuditService audit) {
        this.campagnes = campagnes;
        this.objectifs = objectifs;
        this.employes = employes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.CampagneDto> campagnes() {
        return campagnes.findAllByOrderByDateOuvertureDesc().stream()
                .map(EvaluationDtos.CampagneDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.CampagneDto creerCampagne(EvaluationDtos.CampagneRequest request) {
        CampagneEvaluation campagne = new CampagneEvaluation();
        campagne.setLibelle(request.libelle());
        campagne.setType(request.type().trim().toUpperCase());
        campagne.setPeriodeDebut(request.periodeDebut());
        campagne.setPeriodeFin(request.periodeFin());
        campagne.setDateOuverture(request.dateOuverture());
        campagne.setDateCloture(request.dateCloture());
        campagne.setGrilleJson(request.grilleJson());
        campagne.setPonderationsJson(request.ponderationsJson());
        campagne.setPopulation(request.population());
        campagne.setStatut(CampagneEvaluation.Statut.PREPAREE.name());
        EvaluationDtos.CampagneDto cree = EvaluationDtos.CampagneDto.de(campagnes.save(campagne));
        audit.enregistrer("CAMPAGNE_CREE", "campagne_evaluation", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.CampagneDto changerStatut(Long idCampagne, String statut) {
        CampagneEvaluation campagne = campagne(idCampagne);
        campagne.setStatut(statut.trim().toUpperCase());
        EvaluationDtos.CampagneDto modifiee = EvaluationDtos.CampagneDto.de(campagnes.save(campagne));
        audit.enregistrer("CAMPAGNE_STATUT", "campagne_evaluation", idCampagne, null, modifiee.statut());
        return modifiee;
    }

    @Transactional(readOnly = true)
    public CampagneEvaluation campagne(Long idCampagne) {
        return campagnes.findById(idCampagne)
                .orElseThrow(() -> BusinessException.notFound("Campagne d'évaluation introuvable."));
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.ObjectifDto> objectifs(Long idEmploye, Long idCampagne) {
        List<Objectif> historique = objectifs.findByIdEmployeAndIdCampagneOrderByVersionDescIdAsc(idEmploye, idCampagne);
        Set<Long> remplaces = historique.stream().map(Objectif::getParentObjectifId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return historique.stream().filter(o -> !remplaces.contains(o.getId()))
                .map(EvaluationDtos.ObjectifDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.ObjectifDto creerObjectif(EvaluationDtos.ObjectifRequest request) {
        if (!employes.existe(request.idEmploye())) {
            throw BusinessException.notFound("Employé introuvable.");
        }
        campagne(request.idCampagne());
        if (!"PLUS_EST_MIEUX".equals(request.sens()) && !"MOINS_EST_MIEUX".equals(request.sens())) {
            throw new BusinessException("SENS_OBJECTIF_INVALIDE", "Sens d'objectif invalide.");
        }
        Objectif objectif = new Objectif();
        appliquer(request, objectif);
        EvaluationDtos.ObjectifDto cree = EvaluationDtos.ObjectifDto.de(objectifs.save(objectif));
        audit.enregistrer("OBJECTIF_CREE", "objectif", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional
    public EvaluationDtos.ObjectifAvenantResult avenant(Long idObjectif,
                                                                EvaluationDtos.ObjectifAvenantRequest request) {
        Objectif precedent = objectifs.findById(idObjectif)
                .orElseThrow(() -> BusinessException.notFound("Objectif introuvable."));
        if ("VALIDE".equals(precedent.getStatut()) == false) {
            throw new BusinessException("OBJECTIF_NON_VALIDE", "Seul un objectif validé peut faire l'objet d'un avenant.");
        }
        Objectif avenant = new Objectif();
        appliquer(new EvaluationDtos.ObjectifRequest(precedent.getIdEmploye(), precedent.getIdCampagne(),
                precedent.getId(), precedent.getLibelle(), precedent.getDescription(), precedent.getIndicateur(),
                precedent.getUnite(), precedent.getValeurCible(),
                request.valeurRealisee() == null ? precedent.getValeurRealisee() : request.valeurRealisee(),
                precedent.getSens(), request.poids() == null ? precedent.getPoids() : request.poids(),
                request.dateEcheance() == null ? precedent.getDateEcheance() : request.dateEcheance()), avenant);
        avenant.setVersion(precedent.getVersion() + 1);
        avenant.setMotifAvenant(request.motif());
        EvaluationDtos.ObjectifDto cree = EvaluationDtos.ObjectifDto.de(objectifs.save(avenant));
        audit.enregistrer("OBJECTIF_AVENANT", "objectif", precedent.getId(), precedent.toString(), cree.toString());
        return new EvaluationDtos.ObjectifAvenantResult(cree, EvaluationDtos.ObjectifDto.de(precedent));
    }

    @Transactional
    public EvaluationDtos.ObjectifValidationDto validerObjectifs(Long idEmploye, Long idCampagne) {
        List<Objectif> courants = objectifsCourants(idEmploye, idCampagne);
        BigDecimal somme = courants.stream().map(Objectif::getPoids)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean valide = courants.size() >= MIN_OBJECTIFS && courants.size() <= MAX_OBJECTIFS
                && somme.compareTo(BigDecimal.valueOf(100)) == 0;
        if (!valide) {
            return new EvaluationDtos.ObjectifValidationDto(idEmploye, idCampagne, courants.size(), somme, false);
        }
        courants.forEach(o -> o.setStatut(Objectif.Statut.VALIDE.name()));
        objectifs.saveAll(courants);
        audit.enregistrer("OBJECTIFS_VALIDE", "objectif", idEmploye, null, somme.toPlainString());
        return new EvaluationDtos.ObjectifValidationDto(idEmploye, idCampagne, courants.size(), somme, true);
    }

    @Transactional(readOnly = true)
    public List<Objectif> objectifsCourants(Long idEmploye, Long idCampagne) {
        List<Objectif> historique = objectifs.findByIdEmployeAndIdCampagneOrderByVersionDescIdAsc(idEmploye, idCampagne);
        Set<Long> remplaces = historique.stream().map(Objectif::getParentObjectifId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return historique.stream().filter(o -> !remplaces.contains(o.getId())).toList();
    }

    private void appliquer(EvaluationDtos.ObjectifRequest request, Objectif objectif) {
        objectif.setIdEmploye(request.idEmploye());
        objectif.setIdCampagne(request.idCampagne());
        objectif.setParentObjectifId(request.parentObjectifId());
        objectif.setLibelle(request.libelle());
        objectif.setDescription(request.description());
        objectif.setIndicateur(request.indicateur());
        objectif.setUnite(request.unite());
        objectif.setValeurCible(request.valeurCible());
        objectif.setValeurRealisee(request.valeurRealisee());
        objectif.setSens(request.sens());
        objectif.setPoids(request.poids());
        objectif.setDateEcheance(request.dateEcheance());
        objectif.setStatut(Objectif.Statut.DEFINI.name());
    }
}
