package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.dto.OffreEmploiDto;
import com.tovo.erp.recrutement.dto.OffreIndicateursDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.recrutement.service.OffreEmploiService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/offres")
public class OffreEmploiController {

    private final OffreEmploiService offres;
    private final CandidatureService candidatures;
    private final UtilisateurCourant utilisateurCourant;

    public OffreEmploiController(OffreEmploiService offres, CandidatureService candidatures,
                                 UtilisateurCourant utilisateurCourant) {
        this.offres = offres;
        this.candidatures = candidatures;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<OffreEmploiDto> creerOffre(@Valid @RequestBody OffreEmploiDto.Request request) {
        exigerRh();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offres.creerDepuis(request, utilisateurCourant.exiger().getId()));
    }

    @GetMapping
    public List<OffreEmploiDto> offres(@RequestParam(required = false) String statut) {
        return offres.lister(statut, utilisateurCourant.optionnel() == null);
    }

    @PutMapping("/{id}")
    public OffreEmploiDto modifierOffre(@PathVariable Long id,
                                        @Valid @RequestBody OffreEmploiDto.Request request) {
        exigerRh();
        return offres.modifierDepuis(id, request);
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
        return offres.publierDto(id, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/{id}/cloturer")
    public OffreEmploiDto cloturer(@PathVariable Long id) {
        exigerRh();
        return offres.cloturerDto(id, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/{id}/annuler")
    public OffreEmploiDto annuler(@PathVariable Long id) {
        exigerRh();
        return offres.annulerDto(id, utilisateurCourant.exiger().getId());
    }

    @GetMapping("/{idOffre}/indicateurs")
    public OffreIndicateursDto indicateurs(@PathVariable Long idOffre) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        return offres.indicateurs(idOffre);
    }

    /** Sous-ressource d'une offre : ses candidatures. */
    @GetMapping("/{idOffre}/candidatures")
    public List<CandidatureDto> candidaturesDeLOffre(@PathVariable Long idOffre,
                                                     @RequestParam(required = false) String statut) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        return candidatures.parOffreDto(idOffre, statut);
    }

    private void exigerRh() {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
    }
}
