package com.tovo.erp.evaluation.service;

import com.tovo.erp.evaluation.dto.EvaluationDtos;
import com.tovo.erp.evaluation.model.Competence;
import com.tovo.erp.evaluation.model.EmployeCompetence;
import com.tovo.erp.evaluation.model.PosteCompetence;
import com.tovo.erp.evaluation.repository.CompetenceRepository;
import com.tovo.erp.evaluation.repository.EmployeCompetenceRepository;
import com.tovo.erp.evaluation.repository.PosteCompetenceRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service("evaluationReferentielService")
public class ReferentielService {
    private final CompetenceRepository competences;
    private final PosteCompetenceRepository postesCompetences;
    private final EmployeCompetenceRepository employesCompetences;
    private final JournalAuditService audit;

    public ReferentielService(CompetenceRepository competences,
                              PosteCompetenceRepository postesCompetences,
                              EmployeCompetenceRepository employesCompetences,
                              JournalAuditService audit) {
        this.competences = competences;
        this.postesCompetences = postesCompetences;
        this.employesCompetences = employesCompetences;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.CompetenceDto> competences() {
        return competences.findAllByActifTrueOrderByLibelleAsc().stream()
                .map(EvaluationDtos.CompetenceDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.CompetenceDto creerCompetence(EvaluationDtos.CompetenceRequest request) {
        if (competences.findByCode(request.code()).isPresent()) {
            throw new BusinessException("COMPETENCE_CODE_EXISTANT", "Ce code de compétence existe déjà.");
        }
        Competence competence = new Competence();
        competence.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        competence.setLibelle(request.libelle());
        competence.setDescription(request.description());
        competence.setCategorie(request.categorie().trim().toUpperCase(Locale.ROOT));
        competence.setEchelle(request.echelle());
        competence.setDureeValiditeMois(request.dureeValiditeMois());
        competence.setActif(request.actif() == null || request.actif());
        EvaluationDtos.CompetenceDto cree = EvaluationDtos.CompetenceDto.de(competences.save(competence));
        audit.enregistrer("COMPETENCE_CREE", "competence", cree.id(), null, cree.toString());
        return cree;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.PosteCompetenceDto> posteCompetences(Long idPoste) {
        return postesCompetences.findByIdPosteOrderByPoidsDesc(idPoste).stream()
                .map(EvaluationDtos.PosteCompetenceDto::de).toList();
    }

    @Transactional
    public EvaluationDtos.PosteCompetenceDto enregistrerPosteCompetence(
            EvaluationDtos.PosteCompetenceRequest request) {
        competence(request.idCompetence());
        PosteCompetence posteCompetence = postesCompetences.findByIdPosteOrderByPoidsDesc(request.idPoste())
                .stream().filter(p -> p.getIdCompetence().equals(request.idCompetence()))
                .findFirst().orElseGet(PosteCompetence::new);
        posteCompetence.setIdPoste(request.idPoste());
        posteCompetence.setIdCompetence(request.idCompetence());
        posteCompetence.setNiveauRequis(request.niveauRequis());
        posteCompetence.setPoids(request.poids());
        EvaluationDtos.PosteCompetenceDto enregistre =
                EvaluationDtos.PosteCompetenceDto.de(postesCompetences.save(posteCompetence));
        audit.enregistrer("POSTE_COMPETENCE", "poste_competence", enregistre.id(), null, enregistre.toString());
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<EvaluationDtos.EmployeCompetenceDto> employeCompetences(Long idEmploye) {
        return employesCompetences.findByIdEmployeOrderByDateEvaluationDesc(idEmploye).stream()
                .map(EvaluationDtos.EmployeCompetenceDto::de).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> niveauxAcquis(Long idEmploye) {
        Map<Long, Integer> niveaux = new HashMap<>();
        employesCompetences.findByIdEmployeOrderByDateEvaluationDesc(idEmploye).forEach(niveau ->
                niveaux.putIfAbsent(niveau.getIdCompetence(), niveau.getNiveauAcquis()));
        return niveaux;
    }

    @Transactional
    public EvaluationDtos.EmployeCompetenceDto enregistrerNiveau(
            EvaluationDtos.EmployeCompetenceRequest request, Long idAuteur) {
        competence(request.idCompetence());
        String source = request.source().trim().toUpperCase(Locale.ROOT);
        try {
            EmployeCompetence.Source.valueOf(source);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("SOURCE_COMPETENCE_INVALIDE", "Source de compétence invalide.");
        }
        EmployeCompetence niveau = new EmployeCompetence();
        niveau.setIdEmploye(request.idEmploye());
        niveau.setIdCompetence(request.idCompetence());
        niveau.setNiveauAcquis(request.niveauAcquis());
        niveau.setDateEvaluation(request.dateEvaluation());
        niveau.setDateValidite(request.dateValidite());
        niveau.setSource(source);
        niveau.setEvaluationId(request.evaluationId());
        niveau.setFormationId(request.formationId());
        niveau.setValideParId(request.valideParId() == null ? idAuteur : request.valideParId());
        EvaluationDtos.EmployeCompetenceDto cree =
                EvaluationDtos.EmployeCompetenceDto.de(employesCompetences.save(niveau));
        audit.enregistrer("NIVEAU_COMPETENCE", "employe_competence", cree.id(), request.justificatif(),
                cree.toString());
        return cree;
    }

    @Transactional(readOnly = true)
    public Competence competence(Long idCompetence) {
        return competences.findById(idCompetence)
                .orElseThrow(() -> BusinessException.notFound("Compétence introuvable."));
    }
}
