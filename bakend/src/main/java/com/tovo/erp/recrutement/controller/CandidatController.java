package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.iam.dto.AuthDto;
import com.tovo.erp.iam.service.AuthService;
import com.tovo.erp.recrutement.model.Candidat;
import com.tovo.erp.recrutement.repository.CandidatRepository;
import com.tovo.erp.recrutement.service.CandidatureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidats")
public class CandidatController {

    private final CandidatureService candidatures;
    private final AuthService authService;
    private final CandidatRepository candidatRepository;

    public CandidatController(CandidatureService candidatures, AuthService authService,
                              CandidatRepository candidatRepository) {
        this.candidatures = candidatures;
        this.authService = authService;
        this.candidatRepository = candidatRepository;
    }

    @PostMapping("/inscription")
    public ResponseEntity<com.tovo.erp.iam.dto.AuthDto.Reponse> inscrire(
            @Valid @RequestBody AuthDto.Inscription demande) {
        AuthDto.Reponse reponse = authService.inscrireCandidat(demande);
        Long idUtilisateur = reponse.profil().id();
        Candidat candidat = candidatRepository.findByIdUtilisateur(idUtilisateur).orElse(null);
        if (candidat == null) {
            candidat = new Candidat();
        }
        candidat.setIdUtilisateur(idUtilisateur);
        candidat.setNom(demande.nom().trim());
        candidat.setPrenom(demande.prenom().trim());
        candidat.setEmail(demande.email().trim());
        candidat.setTelephone(demande.telephone());
        candidat.setAdresse(demande.adresse());
        candidatRepository.save(candidat);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidat(idCandidat).stream().map(CandidatureDto::de).toList();
    }
}
