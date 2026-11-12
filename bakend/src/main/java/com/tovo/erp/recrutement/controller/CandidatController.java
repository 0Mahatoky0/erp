package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.dto.AuthDto;
import com.tovo.erp.iam.service.AuthService;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import jakarta.validation.Valid;
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

    public CandidatController(CandidatureService candidatures, AuthService authService) {
        this.candidatures = candidatures;
        this.authService = authService;
    }

    @PostMapping("/inscription")
    public ResponseEntity<AuthDto.Reponse> inscrire(@Valid @RequestBody AuthDto.Inscription demande) {
        AuthDto.Reponse reponse = authService.inscrireCandidat(demande);
        candidatures.enregistrerProfilCandidat(reponse.profil().id(), demande.nom(), demande.prenom(),
                demande.email(), demande.telephone(), demande.adresse());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidatDto(idCandidat);
    }
}
