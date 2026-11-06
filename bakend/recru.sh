#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Génère les controllers spécifiques du module recrutement et met de côté
# l'ancien RecrutementController.
#
# Usage (à lancer depuis la racine du projet Maven/Gradle) :
#   chmod +x creer_controllers_recrutement.sh
#   ./creer_controllers_recrutement.sh
#
# Option : passer un autre dossier cible
#   ./creer_controllers_recrutement.sh chemin/vers/controller
# ---------------------------------------------------------------------------
set -euo pipefail

DEFAULT_DIR="src/main/java/com/tovo/erp/recrutement/controller"
DIR="${1:-$DEFAULT_DIR}"

if [ ! -d "$DIR" ]; then
  echo "Dossier introuvable : $DIR"
  echo "Lance le script depuis la racine du projet, ou passe le dossier en argument."
  exit 1
fi

# --- Sauvegarde de l'ancien controller (sorti de la compilation) -----------
OLD="$DIR/RecrutementController.java"
if [ -f "$OLD" ]; then
  mv "$OLD" "$DIR/RecrutementController.java.bak"
  echo "Ancien controller sauvegardé : RecrutementController.java.bak"
fi

# ---------------------------------------------------------------------------
# 1. OffreEmploiController
# ---------------------------------------------------------------------------
cat > "$DIR/OffreEmploiController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.OffreEmploiDto;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.model.OffreEmploi;
import com.tovo.erp.recrutement.model.OffreStatut;
import com.tovo.erp.recrutement.repository.OffreEmploiRepository;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.recrutement.service.OffreEmploiService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recrutement/offres")
public class OffreEmploiController {

    private final OffreEmploiService offres;
    private final CandidatureService candidatures;
    private final OffreEmploiRepository offreRepository;
    private final UtilisateurCourant utilisateurCourant;

    public OffreEmploiController(OffreEmploiService offres, CandidatureService candidatures,
                                 OffreEmploiRepository offreRepository, UtilisateurCourant utilisateurCourant) {
        this.offres = offres;
        this.candidatures = candidatures;
        this.offreRepository = offreRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<OffreEmploiDto> creerOffre(@Valid @RequestBody OffreEmploi offre) {
        exigerRh();
        offre.setIdUtilisateurCreateur(utilisateurCourant.exiger().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OffreEmploiDto.de(offres.creer(offre)));
    }

    @GetMapping
    public List<OffreEmploiDto> offres(@RequestParam(required = false) OffreStatut statut) {
        List<OffreEmploi> result;
        if (utilisateurCourant.optionnel() == null) {
            result = offreRepository.findByStatut(OffreStatut.PUBLIEE);
        } else {
            result = statut == null ? offreRepository.findAll() : offreRepository.findByStatut(statut);
        }
        return result.stream().map(OffreEmploiDto::de).collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public OffreEmploiDto modifierOffre(@PathVariable Long id, @Valid @RequestBody OffreEmploi offre) {
        exigerRh();
        return OffreEmploiDto.de(offres.modifier(id, offre));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerOffre(@PathVariable Long id) {
        exigerRh();
        offres.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publier")
    public OffreEmploiDto publier(@PathVariable Long id) {
        exigerRh();
        return OffreEmploiDto.de(offres.publier(id, utilisateurCourant.exiger().getId()));
    }

    @PostMapping("/{id}/cloturer")
    public OffreEmploiDto cloturer(@PathVariable Long id) {
        exigerRh();
        return OffreEmploiDto.de(offres.cloturer(id, utilisateurCourant.exiger().getId()));
    }

    @PostMapping("/{id}/annuler")
    public OffreEmploiDto annuler(@PathVariable Long id) {
        exigerRh();
        return OffreEmploiDto.de(offres.annuler(id, utilisateurCourant.exiger().getId()));
    }

    /** Sous-ressource d'une offre : ses candidatures. */
    @GetMapping("/{idOffre}/candidatures")
    public List<CandidatureDto> candidaturesDeLOffre(@PathVariable Long idOffre,
                                                     @RequestParam(required = false) CandidatureStatut statut) {
        return candidatures.parOffre(idOffre, statut).stream()
                .map(CandidatureDto::de).collect(Collectors.toList());
    }

    private void exigerRh() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
    }
}
EOF
echo "Créé : OffreEmploiController.java"

# ---------------------------------------------------------------------------
# 2. CandidatureController
# ---------------------------------------------------------------------------
cat > "$DIR/CandidatureController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.HistoriqueCandidatureDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.HistoriqueCandidatureRepository;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidatures")
public class CandidatureController {

    private final CandidatureService candidatures;
    private final CandidatureRepository candidatureRepository;
    private final HistoriqueCandidatureRepository historiqueRepository;
    private final UtilisateurCourant utilisateurCourant;

    public CandidatureController(CandidatureService candidatures, CandidatureRepository candidatureRepository,
                                 HistoriqueCandidatureRepository historiqueRepository,
                                 UtilisateurCourant utilisateurCourant) {
        this.candidatures = candidatures;
        this.candidatureRepository = candidatureRepository;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<CandidatureDto> postuler(@Valid @RequestBody RecrutementRequests.CandidatureRequest request) {
        Candidature candidature = candidatures.postuler(
                request.idOffre(), request.candidat().toEntity(), request.messageCandidat(), request.source());
        return ResponseEntity.status(HttpStatus.CREATED).body(CandidatureDto.de(candidature));
    }

    @GetMapping("/{id}")
    public CandidatureDto candidature(@PathVariable Long id) {
        return CandidatureDto.de(candidatureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + id)));
    }

    @PatchMapping("/{id}/statut")
    public CandidatureDto statut(@PathVariable Long id, @Valid @RequestBody RecrutementRequests.StatutRequest request) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return CandidatureDto.de(candidatures.changerStatut(
                id, request.statut(), utilisateurCourant.exiger().getId(), request.commentaire()));
    }

    @DeleteMapping("/{id}")
    public CandidatureDto retirer(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return CandidatureDto.de(candidatures.changerStatut(
                id, CandidatureStatut.RETIREE, utilisateurCourant.exiger().getId(), "Retrait de candidature"));
    }

    @GetMapping("/{id}/historique")
    public List<HistoriqueCandidatureDto> historique(@PathVariable Long id) {
        return historiqueRepository.findByCandidatureIdCandidatureOrderByDateActionAsc(id)
                .stream().map(HistoriqueCandidatureDto::de).toList();
    }
}
EOF
echo "Créé : CandidatureController.java"

# ---------------------------------------------------------------------------
# 3. CandidatController
# ---------------------------------------------------------------------------
cat > "$DIR/CandidatController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidats")
public class CandidatController {

    private final CandidatureService candidatures;

    public CandidatController(CandidatureService candidatures) {
        this.candidatures = candidatures;
    }

    @GetMapping("/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidat(idCandidat).stream().map(CandidatureDto::de).toList();
    }
}
EOF
echo "Créé : CandidatController.java"

# ---------------------------------------------------------------------------
# 4. DocumentCandidatureController
# ---------------------------------------------------------------------------
cat > "$DIR/DocumentCandidatureController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.DocumentCandidatureDto;
import com.tovo.erp.recrutement.model.TypeDocument;
import com.tovo.erp.recrutement.service.DocumentCandidatureService;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidatures/{id}/documents")
public class DocumentCandidatureController {

    private final DocumentCandidatureService documents;
    private final UtilisateurCourant utilisateurCourant;

    public DocumentCandidatureController(DocumentCandidatureService documents,
                                         UtilisateurCourant utilisateurCourant) {
        this.documents = documents;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentCandidatureDto> deposer(@PathVariable Long id, @RequestParam TypeDocument type,
                                                          @RequestPart MultipartFile fichier) throws IOException {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentCandidatureDto.de(documents.deposer(id, type, fichier)));
    }

    @GetMapping
    public List<DocumentCandidatureDto> lister(@PathVariable Long id) {
        return documents.lister(id).stream().map(DocumentCandidatureDto::de).toList();
    }
}
EOF
echo "Créé : DocumentCandidatureController.java"

# ---------------------------------------------------------------------------
# 5. EntretienController
# ---------------------------------------------------------------------------
cat > "$DIR/EntretienController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.EntretienDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.Entretien;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.EntretienRepository;
import com.tovo.erp.recrutement.service.EntretienTestService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class EntretienController {

    private final EntretienTestService entretienTests;
    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurCourant utilisateurCourant;

    public EntretienController(EntretienTestService entretienTests, EntretienRepository entretienRepository,
                               CandidatureRepository candidatureRepository, UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.entretienRepository = entretienRepository;
        this.candidatureRepository = candidatureRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienDto> planifier(@Valid @RequestBody RecrutementRequests.EntretienRequest r) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        Entretien e = new Entretien();
        e.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        e.setIdUtilisateurOrganisateur(utilisateurCourant.exiger().getId());
        e.setTypeEntretien(r.typeEntretien());
        e.setDateDebut(r.dateDebut());
        e.setDateFin(r.dateFin());
        e.setLieu(r.lieu());
        e.setLienVisio(r.lienVisio());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntretienDto.de(entretienTests.planifierEntretien(e)));
    }

    @GetMapping("/candidatures/{id}/entretiens")
    public List<EntretienDto> parCandidature(@PathVariable Long id) {
        return entretienRepository.findByCandidatureIdCandidature(id).stream().map(EntretienDto::de).toList();
    }
}
EOF
echo "Créé : EntretienController.java"

# ---------------------------------------------------------------------------
# 6. TestRecrutementController
# ---------------------------------------------------------------------------
cat > "$DIR/TestRecrutementController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.PassageTestDto;
import com.tovo.erp.recrutement.dto.TestRecrutementDto;
import com.tovo.erp.recrutement.model.PassageTest;
import com.tovo.erp.recrutement.model.TestRecrutement;
import com.tovo.erp.recrutement.repository.PassageTestRepository;
import com.tovo.erp.recrutement.repository.TestRecrutementRepository;
import com.tovo.erp.recrutement.service.EntretienTestService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class TestRecrutementController {

    private final EntretienTestService entretienTests;
    private final TestRecrutementRepository testRepository;
    private final PassageTestRepository passageRepository;
    private final UtilisateurCourant utilisateurCourant;

    public TestRecrutementController(EntretienTestService entretienTests, TestRecrutementRepository testRepository,
                                     PassageTestRepository passageRepository,
                                     UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.testRepository = testRepository;
        this.passageRepository = passageRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/tests")
    public ResponseEntity<TestRecrutementDto> creerTest(@Valid @RequestBody TestRecrutement test) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestRecrutementDto.de(entretienTests.creerTest(test)));
    }

    @GetMapping("/offres/{idOffre}/tests")
    public List<TestRecrutementDto> testsParOffre(@PathVariable Long idOffre) {
        return testRepository.findByOffreIdOffre(idOffre).stream().map(TestRecrutementDto::de).toList();
    }

    @PostMapping("/passages-tests")
    public ResponseEntity<PassageTestDto> planifierPassage(@Valid @RequestBody PassageTest passage) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PassageTestDto.de(entretienTests.planifierPassage(passage)));
    }

    @GetMapping("/candidatures/{id}/passages-tests")
    public List<PassageTestDto> passagesParCandidature(@PathVariable Long id) {
        return passageRepository.findByCandidatureIdCandidature(id).stream().map(PassageTestDto::de).toList();
    }
}
EOF
echo "Créé : TestRecrutementController.java"

# ---------------------------------------------------------------------------
# 7. EvaluationCandidatController
# ---------------------------------------------------------------------------
cat > "$DIR/EvaluationCandidatController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.EvaluationCandidatDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.EvaluationCandidat;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.EntretienRepository;
import com.tovo.erp.recrutement.repository.EvaluationCandidatRepository;
import com.tovo.erp.recrutement.repository.PassageTestRepository;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class EvaluationCandidatController {

    private final EvaluationDecisionService evaluations;
    private final EvaluationCandidatRepository evaluationRepository;
    private final CandidatureRepository candidatureRepository;
    private final EntretienRepository entretienRepository;
    private final PassageTestRepository passageRepository;
    private final UtilisateurCourant utilisateurCourant;

    public EvaluationCandidatController(EvaluationDecisionService evaluations,
                                        EvaluationCandidatRepository evaluationRepository,
                                        CandidatureRepository candidatureRepository,
                                        EntretienRepository entretienRepository,
                                        PassageTestRepository passageRepository,
                                        UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.evaluationRepository = evaluationRepository;
        this.candidatureRepository = candidatureRepository;
        this.entretienRepository = entretienRepository;
        this.passageRepository = passageRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationCandidatDto> evaluer(
            @Valid @RequestBody RecrutementRequests.EvaluationRequest request) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        EvaluationCandidat evaluation = new EvaluationCandidat();
        evaluation.setCandidature(candidatureRepository.getReferenceById(request.idCandidature()));
        evaluation.setIdUtilisateurEvaluateur(utilisateurCourant.exiger().getId());
        evaluation.setTypeEvaluation(request.typeEvaluation());
        evaluation.setNote(request.note());
        evaluation.setNoteMax(request.noteMax());
        evaluation.setAppreciation(request.appreciation());
        evaluation.setCommentaire(request.commentaire());
        if (request.idEntretien() != null) {
            evaluation.setEntretien(entretienRepository.getReferenceById(request.idEntretien()));
        }
        if (request.idPassage() != null) {
            evaluation.setPassage(passageRepository.getReferenceById(request.idPassage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EvaluationCandidatDto.de(evaluations.evaluer(evaluation)));
    }

    @GetMapping("/candidatures/{id}/evaluations")
    public List<EvaluationCandidatDto> parCandidature(@PathVariable Long id) {
        return evaluationRepository.findByCandidatureIdCandidature(id).stream()
                .map(EvaluationCandidatDto::de).toList();
    }
}
EOF
echo "Créé : EvaluationCandidatController.java"

# ---------------------------------------------------------------------------
# 8. DecisionCandidatureController
# ---------------------------------------------------------------------------
cat > "$DIR/DecisionCandidatureController.java" <<'EOF'
package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.DecisionCandidatureDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.DecisionCandidature;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.service.EvaluationDecisionService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recrutement/decisions")
public class DecisionCandidatureController {

    private final EvaluationDecisionService evaluations;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurCourant utilisateurCourant;

    public DecisionCandidatureController(EvaluationDecisionService evaluations,
                                         CandidatureRepository candidatureRepository,
                                         UtilisateurCourant utilisateurCourant) {
        this.evaluations = evaluations;
        this.candidatureRepository = candidatureRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<DecisionCandidatureDto> decider(@Valid @RequestBody RecrutementRequests.DecisionRequest r) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        DecisionCandidature d = new DecisionCandidature();
        d.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        d.setIdUtilisateur(utilisateurCourant.exiger().getId());
        d.setDecision(r.decision());
        d.setMotif(r.motif());
        return ResponseEntity.status(HttpStatus.CREATED).body(DecisionCandidatureDto.de(evaluations.decider(d)));
    }
}
EOF
echo "Créé : DecisionCandidatureController.java"

echo ""
echo "Terminé : 8 controllers créés dans $DIR"
echo "Compile maintenant (mvn clean compile / gradle build) et vérifie les URL avec tes tests."