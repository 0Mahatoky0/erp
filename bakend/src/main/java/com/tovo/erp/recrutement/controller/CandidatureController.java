package com.tovo.erp.recrutement.controller;

import com.tovo.erp.shared.Roles;
import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto;
import com.tovo.erp.recrutement.dto.HistoriqueCandidatureDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.Candidature;
import com.tovo.erp.recrutement.model.CandidatureStatut;
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
    private final HistoriqueCandidatureRepository historiqueRepository;
    private final UtilisateurCourant utilisateurCourant;

    public CandidatureController(CandidatureService candidatures,
                                 HistoriqueCandidatureRepository historiqueRepository,
                                 UtilisateurCourant utilisateurCourant) {
        this.candidatures = candidatures;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<CandidatureDto> postuler(@Valid @RequestBody RecrutementRequests.CandidatureRequest request) {
        Candidature candidature = candidatures.postuler(
                request.idOffre(), utilisateurCourant.exiger(), request.messageCandidat(), request.source());
        return ResponseEntity.status(HttpStatus.CREATED).body(CandidatureDto.de(candidature));
    }

    @GetMapping(value = "/public")
    public List<CandidatureDto> publicParEmail(@RequestParam String email) {
        return candidatures.listerParEmailPublic(email);
    }

    @GetMapping("/mes-candidatures")
    public List<CandidatureDto> mesCandidatures() {
        utilisateurCourant.exigerRole(Roles.CANDIDAT);
        var utilisateur = utilisateurCourant.exiger();
        return candidatures.listerParUtilisateurConnecte(utilisateur.getId(), utilisateur.getEmail());
    }

    @GetMapping
    public List<CandidatureDto> lister(@RequestParam(required = false) CandidatureStatut statut) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return candidatures.listerParStatut(statut);
    }

    @GetMapping("/{id:[0-9]+}")
    public CandidatureDto candidature(@PathVariable Long id) {
        return candidatures.detail(id);
    }

    @GetMapping("/{id:[0-9]+}/embauche")
    public CandidatureEmbaucheDto dossierEmbauche(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return candidatures.detailsPourEmbauche(id);
    }

    @PatchMapping("/{id:[0-9]+}/statut")
    public CandidatureDto statut(@PathVariable Long id, @Valid @RequestBody RecrutementRequests.StatutRequest request) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return candidatures.changerStatut(
                id, request.statut(), utilisateurCourant.exiger().getId(), request.commentaire());
    }

    @DeleteMapping("/{id:[0-9]+}")
    public CandidatureDto retirer(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return candidatures.changerStatut(
                id, CandidatureStatut.RETIREE, utilisateurCourant.exiger().getId(), "Retrait de candidature");
    }

    @GetMapping("/{id:[0-9]+}/historique")
    public List<HistoriqueCandidatureDto> historique(@PathVariable Long id) {
        return historiqueRepository.findByCandidatureIdCandidatureOrderByDateActionAsc(id)
                .stream().map(HistoriqueCandidatureDto::de).toList();
    }
}
