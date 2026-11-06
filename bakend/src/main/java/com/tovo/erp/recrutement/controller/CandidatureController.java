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
