package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.dto.AuthDto;
import com.tovo.erp.iam.service.AuthService;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.audit.JournalAuditService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidats")
public class CandidatController {

    private final CandidatureService candidatures;
    private final AuthService authService;
    private final JournalAuditService audit;

    public CandidatController(CandidatureService candidatures, AuthService authService,
                              JournalAuditService audit) {
        this.candidatures = candidatures;
        this.authService = authService;
        this.audit = audit;
    }

    @PostMapping("/inscription")
    public ResponseEntity<AuthDto.Reponse> inscrire(@Valid @RequestBody AuthDto.Inscription demande) {
        AuthDto.Reponse reponse = authService.inscrireCandidat(demande);
        candidatures.enregistrerProfilCandidat(reponse.profil().id(), demande.nom(), demande.prenom(),
                demande.email(), demande.telephone(), demande.adresse(), demande.consentementDonnees());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/{idCandidat}/anonymiser")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ResponseEntity<Void> anonymiser(@PathVariable Long idCandidat) {
        candidatures.anonymiser(idCandidat);
        audit.enregistrer("ANONYMISATION", "CANDIDAT", idCandidat, null, null,
                "Droit a l'effacement (RGPD)", null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidatDto(idCandidat);
    }
}
