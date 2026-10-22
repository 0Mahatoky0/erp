package com.tovo.erp.recrutement.api;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class RecrutementController {
    private final CandidatureService candidatures;
    private final DocumentCandidatureService documents;
    private final EntretienTestService entretienTests;
    private final EvaluationDecisionService evaluations;
    private final OffreEmploiService offres;
    private final CandidatureRepository candidatureRepository;
    private final HistoriqueCandidatureRepository historiqueRepository;
    private final OffreEmploiRepository offreRepository;
    private final EvaluationCandidatRepository evaluationRepository;
    private final EntretienRepository entretienRepository;
    private final TestRecrutementRepository testRepository;
    private final PassageTestRepository passageRepository;

    public RecrutementController(CandidatureService candidatures, DocumentCandidatureService documents,
                                 EntretienTestService entretienTests, EvaluationDecisionService evaluations,
                                 OffreEmploiService offres, CandidatureRepository candidatureRepository,
                                 HistoriqueCandidatureRepository historiqueRepository,
                                 OffreEmploiRepository offreRepository, EvaluationCandidatRepository evaluationRepository,
                                 EntretienRepository entretienRepository, TestRecrutementRepository testRepository,
                                 PassageTestRepository passageRepository) {
        this.candidatures = candidatures; this.documents = documents; this.entretienTests = entretienTests;
        this.evaluations = evaluations; this.offres = offres; this.candidatureRepository = candidatureRepository;
        this.historiqueRepository = historiqueRepository;
        this.offreRepository = offreRepository; this.evaluationRepository = evaluationRepository;
        this.entretienRepository = entretienRepository; this.testRepository = testRepository;
        this.passageRepository = passageRepository;
    }

    @PostMapping("/offres")
    public ResponseEntity<OffreEmploi> creerOffre(@Valid @RequestBody OffreEmploi offre) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offres.creer(offre));
    }

    @GetMapping("/offres")
    public List<OffreEmploi> offres(@RequestParam(required = false) OffreStatut statut) {
        return statut == null ? offreRepository.findAll() : offreRepository.findByStatut(statut);
    }

    @PutMapping("/offres/{id}")
    public OffreEmploi modifierOffre(@PathVariable Long id, @Valid @RequestBody OffreEmploi offre) {
        return offres.modifier(id, offre);
    }

    @DeleteMapping("/offres/{id}")
    public ResponseEntity<Void> supprimerOffre(@PathVariable Long id) {
        offres.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/offres/{id}/publier")
    public OffreEmploi publier(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        return offres.publier(id, idUtilisateur);
    }

    @PostMapping("/offres/{id}/cloturer")
    public OffreEmploi cloturer(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        return offres.cloturer(id, idUtilisateur);
    }

    @PostMapping("/offres/{id}/annuler")
    public OffreEmploi annuler(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        return offres.annuler(id, idUtilisateur);
    }

    @PostMapping("/candidatures")
    public ResponseEntity<Candidature> postuler(@Valid @RequestBody RecrutementRequests.CandidatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidatures.postuler(
                request.idOffre(), request.candidat().toEntity(), request.messageCandidat(), request.source()));
    }

    @GetMapping("/offres/{idOffre}/candidatures")
    public List<Candidature> lister(@PathVariable Long idOffre, @RequestParam(required = false) CandidatureStatut statut) {
        return candidatures.parOffre(idOffre, statut);
    }

    @GetMapping("/candidats/{idCandidat}/candidatures")
    public List<Candidature> parCandidat(@PathVariable Long idCandidat) { return candidatures.parCandidat(idCandidat); }

    @PatchMapping("/candidatures/{id}/statut")
    public Candidature statut(@PathVariable Long id, @Valid @RequestBody RecrutementRequests.StatutRequest request) {
        return candidatures.changerStatut(id, request.statut(), request.idUtilisateur(), request.commentaire());
    }

    @DeleteMapping("/candidatures/{id}")
    public Candidature retirer(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        return candidatures.changerStatut(id, CandidatureStatut.RETIREE, idUtilisateur, "Retrait de candidature");
    }

    @PostMapping(value = "/candidatures/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentCandidature> document(@PathVariable Long id, @RequestParam TypeDocument type,
                                                        @RequestPart MultipartFile fichier) throws java.io.IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(documents.deposer(id, type, fichier));
    }

    @GetMapping("/candidatures/{id}/documents")
    public List<DocumentCandidature> documents(@PathVariable Long id) { return documents.lister(id); }

    @PostMapping("/entretiens")
    public ResponseEntity<Entretien> entretien(@Valid @RequestBody RecrutementRequests.EntretienRequest r) {
        Entretien e = new Entretien(); e.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        e.setIdUtilisateurOrganisateur(r.idUtilisateurOrganisateur()); e.setTypeEntretien(r.typeEntretien());
        e.setDateDebut(r.dateDebut()); e.setDateFin(r.dateFin()); e.setLieu(r.lieu()); e.setLienVisio(r.lienVisio());
        return ResponseEntity.status(HttpStatus.CREATED).body(entretienTests.planifierEntretien(e));
    }

    @GetMapping("/candidatures/{id}/entretiens")
    public List<Entretien> entretiens(@PathVariable Long id) {
        return entretienRepository.findByCandidatureIdCandidature(id);
    }

    @PostMapping("/tests")
    public ResponseEntity<TestRecrutement> test(@Valid @RequestBody TestRecrutement test) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entretienTests.creerTest(test));
    }

    @GetMapping("/offres/{idOffre}/tests")
    public List<TestRecrutement> tests(@PathVariable Long idOffre) {
        return testRepository.findByOffreIdOffre(idOffre);
    }

    @PostMapping("/passages-tests")
    public ResponseEntity<PassageTest> passageTest(@Valid @RequestBody PassageTest passage) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entretienTests.planifierPassage(passage));
    }

    @GetMapping("/candidatures/{id}/passages-tests")
    public List<PassageTest> passagesTests(@PathVariable Long id) {
        return passageRepository.findByCandidatureIdCandidature(id);
    }

    @GetMapping("/candidatures/{id}/historique")
    public List<HistoriqueCandidature> historique(@PathVariable Long id) {
        return historiqueRepository.findByCandidatureIdCandidatureOrderByDateActionAsc(id);
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationCandidat> evaluation(@Valid @RequestBody EvaluationCandidat evaluation) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluations.evaluer(evaluation));
    }

    @GetMapping("/candidatures/{id}/evaluations")
    public List<EvaluationCandidat> evaluations(@PathVariable Long id) {
        return evaluationRepository.findByCandidatureIdCandidature(id);
    }

    @PostMapping("/decisions")
    public ResponseEntity<DecisionCandidature> decision(@Valid @RequestBody RecrutementRequests.DecisionRequest r) {
        DecisionCandidature d = new DecisionCandidature();
        d.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        d.setIdUtilisateur(r.idUtilisateur()); d.setDecision(r.decision()); d.setMotif(r.motif());
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluations.decider(d));
    }
}
