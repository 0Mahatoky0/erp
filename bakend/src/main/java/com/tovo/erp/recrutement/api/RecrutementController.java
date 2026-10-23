package com.tovo.erp.recrutement.api;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.dto.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.service.*;
import com.tovo.erp.domain.enums.RoleNom;
import com.tovo.erp.security.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

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
    private final UtilisateurCourant utilisateurCourant;

    public RecrutementController(CandidatureService candidatures, DocumentCandidatureService documents,
                                 EntretienTestService entretienTests, EvaluationDecisionService evaluations,
                                 OffreEmploiService offres, CandidatureRepository candidatureRepository,
                                 HistoriqueCandidatureRepository historiqueRepository,
                                 OffreEmploiRepository offreRepository, EvaluationCandidatRepository evaluationRepository,
                                 EntretienRepository entretienRepository, TestRecrutementRepository testRepository,
                                 PassageTestRepository passageRepository, UtilisateurCourant utilisateurCourant) {
        this.candidatures = candidatures; this.documents = documents; this.entretienTests = entretienTests;
        this.evaluations = evaluations; this.offres = offres; this.candidatureRepository = candidatureRepository;
        this.historiqueRepository = historiqueRepository;
        this.offreRepository = offreRepository; this.evaluationRepository = evaluationRepository;
        this.entretienRepository = entretienRepository; this.testRepository = testRepository;
        this.passageRepository = passageRepository; this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/offres")
    public ResponseEntity<OffreEmploiDto> creerOffre(@Valid @RequestBody OffreEmploi offre) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        offre.setIdUtilisateurCreateur(utilisateurCourant.exiger().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OffreEmploiDto.de(offres.creer(offre)));
    }

    @GetMapping("/offres")
    public List<OffreEmploiDto> offres(@RequestParam(required = false) OffreStatut statut) {
        List<OffreEmploi> result;
        if (utilisateurCourant.optionnel() == null) {
            result = offreRepository.findByStatut(OffreStatut.PUBLIEE);
        } else {
            result = statut == null ? offreRepository.findAll() : offreRepository.findByStatut(statut);
        }
        return result.stream().map(OffreEmploiDto::de).collect(Collectors.toList());
    }

    @PutMapping("/offres/{id}")
    public OffreEmploiDto modifierOffre(@PathVariable Long id, @Valid @RequestBody OffreEmploi offre) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return OffreEmploiDto.de(offres.modifier(id, offre));
    }

    @DeleteMapping("/offres/{id}")
    public ResponseEntity<Void> supprimerOffre(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        offres.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/offres/{id}/publier")
    public OffreEmploiDto publier(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return OffreEmploiDto.de(offres.publier(id, utilisateurCourant.exiger().getId()));
    }

    @PostMapping("/offres/{id}/cloturer")
    public OffreEmploiDto cloturer(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return OffreEmploiDto.de(offres.cloturer(id, utilisateurCourant.exiger().getId()));
    }

    @PostMapping("/offres/{id}/annuler")
    public OffreEmploiDto annuler(@PathVariable Long id, @RequestParam Long idUtilisateur) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return OffreEmploiDto.de(offres.annuler(id, utilisateurCourant.exiger().getId()));
    }

    @PostMapping("/candidatures")
    public ResponseEntity<CandidatureDto> postuler(@Valid @RequestBody RecrutementRequests.CandidatureRequest request) {
        Candidature candidature = candidatures.postuler(
                request.idOffre(), request.candidat().toEntity(), request.messageCandidat(), request.source());
        return ResponseEntity.status(HttpStatus.CREATED).body(CandidatureDto.de(candidature));
    }

    @GetMapping("/offres/{idOffre}/candidatures")
    public List<CandidatureDto> lister(@PathVariable Long idOffre, @RequestParam(required = false) CandidatureStatut statut) {
        return candidatures.parOffre(idOffre, statut).stream()
                .map(CandidatureDto::de).collect(Collectors.toList());
    }

    @GetMapping("/candidats/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidat(idCandidat).stream()
                .map(CandidatureDto::de).collect(Collectors.toList());
    }

    @GetMapping("/candidatures/{id}")
    public CandidatureDto candidature(@PathVariable Long id) {
        return CandidatureDto.de(candidatureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + id)));
    }

    @PatchMapping("/candidatures/{id}/statut")
    public CandidatureDto statut(@PathVariable Long id, @Valid @RequestBody RecrutementRequests.StatutRequest request) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return CandidatureDto.de(candidatures.changerStatut(
                id, request.statut(), utilisateurCourant.exiger().getId(), request.commentaire()));
    }

    @DeleteMapping("/candidatures/{id}")
    public CandidatureDto retirer(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return CandidatureDto.de(candidatures.changerStatut(
                id, CandidatureStatut.RETIREE, utilisateurCourant.exiger().getId(), "Retrait de candidature"));
    }

    @PostMapping(value = "/candidatures/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentCandidatureDto> document(@PathVariable Long id, @RequestParam TypeDocument type,
                                                           @RequestPart MultipartFile fichier) throws java.io.IOException {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentCandidatureDto.de(documents.deposer(id, type, fichier)));
    }

    @GetMapping("/candidatures/{id}/documents")
    public List<DocumentCandidatureDto> documents(@PathVariable Long id) {
        return documents.lister(id).stream().map(DocumentCandidatureDto::de).toList();
    }

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienDto> entretien(@Valid @RequestBody RecrutementRequests.EntretienRequest r) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        Entretien e = new Entretien(); e.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        e.setIdUtilisateurOrganisateur(utilisateurCourant.exiger().getId()); e.setTypeEntretien(r.typeEntretien());
        e.setDateDebut(r.dateDebut()); e.setDateFin(r.dateFin()); e.setLieu(r.lieu()); e.setLienVisio(r.lienVisio());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntretienDto.de(entretienTests.planifierEntretien(e)));
    }

    @GetMapping("/candidatures/{id}/entretiens")
    public List<EntretienDto> entretiens(@PathVariable Long id) {
        return entretienRepository.findByCandidatureIdCandidature(id).stream().map(EntretienDto::de).toList();
    }

    @PostMapping("/tests")
    public ResponseEntity<TestRecrutementDto> test(@Valid @RequestBody TestRecrutement test) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestRecrutementDto.de(entretienTests.creerTest(test)));
    }

    @GetMapping("/offres/{idOffre}/tests")
    public List<TestRecrutementDto> tests(@PathVariable Long idOffre) {
        return testRepository.findByOffreIdOffre(idOffre).stream().map(TestRecrutementDto::de).toList();
    }

    @PostMapping("/passages-tests")
    public ResponseEntity<PassageTestDto> passageTest(@Valid @RequestBody PassageTest passage) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(PassageTestDto.de(entretienTests.planifierPassage(passage)));
    }

    @GetMapping("/candidatures/{id}/passages-tests")
    public List<PassageTestDto> passagesTests(@PathVariable Long id) {
        return passageRepository.findByCandidatureIdCandidature(id).stream().map(PassageTestDto::de).toList();
    }

    @GetMapping("/candidatures/{id}/historique")
    public List<HistoriqueCandidatureDto> historique(@PathVariable Long id) {
        return historiqueRepository.findByCandidatureIdCandidatureOrderByDateActionAsc(id)
                .stream().map(HistoriqueCandidatureDto::de).toList();
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationCandidatDto> evaluation(@Valid @RequestBody EvaluationCandidat evaluation) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(EvaluationCandidatDto.de(evaluations.evaluer(evaluation)));
    }

    @GetMapping("/candidatures/{id}/evaluations")
    public List<EvaluationCandidatDto> evaluations(@PathVariable Long id) {
        return evaluationRepository.findByCandidatureIdCandidature(id).stream().map(EvaluationCandidatDto::de).toList();
    }

    @PostMapping("/decisions")
    public ResponseEntity<DecisionCandidatureDto> decision(@Valid @RequestBody RecrutementRequests.DecisionRequest r) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        DecisionCandidature d = new DecisionCandidature();
        d.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        d.setIdUtilisateur(utilisateurCourant.exiger().getId()); d.setDecision(r.decision()); d.setMotif(r.motif());
        return ResponseEntity.status(HttpStatus.CREATED).body(DecisionCandidatureDto.de(evaluations.decider(d)));
    }
}
