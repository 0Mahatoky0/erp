package com.tovo.erp.demande.api;

import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.demande.service.ValidationService;
import com.tovo.erp.demande.dto.DemandeDto;
import com.tovo.erp.demande.dto.ValidationDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Circuit de validation (RM-032 a RM-040).
 *
 * <p>Chaque validateur traite la file de travail de son etape : RH, FINANCE ou DIRECTION.
 */
@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private final ValidationService validationService;
    private final UtilisateurCourant utilisateurCourant;

    public ValidationController(ValidationService validationService, UtilisateurCourant utilisateurCourant) {
        this.validationService = validationService;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Lignes qui attendent la decision de l'utilisateur connecte. */
    @GetMapping("/file")
    public ResponseEntity<List<DemandeDto.LigneResume>> file() {
        return ResponseEntity.ok(validationService.fileAttente(utilisateurCourant.exiger()));
    }

    /** Enregistre une decision (approbation, refus ou demande de correction). */
    @PostMapping("/lignes/{idLigne}/decision")
    public ResponseEntity<DemandeDto.LigneResume> decider(@PathVariable Long idLigne,
                                                          @Valid @RequestBody ValidationDto.DecisionRequest decision) {
        return ResponseEntity.ok(validationService.decider(idLigne, decision, utilisateurCourant.exiger()));
    }
}
