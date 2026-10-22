package com.tovo.erp.web.controller;

import com.tovo.erp.domain.enums.StatutDemande;
import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.service.DemandeService;
import com.tovo.erp.web.dto.DemandeDto;
import jakarta.validation.Valid;
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

/**
 * Demandes de recrutement et leurs lignes (RM-012 a RM-031, RM-041 a RM-047).
 */
@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService demandeService;
    private final UtilisateurCourant utilisateurCourant;

    public DemandeController(DemandeService demandeService, UtilisateurCourant utilisateurCourant) {
        this.demandeService = demandeService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<List<DemandeDto.Resume>> lister(
            @RequestParam(required = false) StatutDemande statut,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Boolean mesDemandes) {
        return ResponseEntity.ok(demandeService.lister(utilisateurCourant.exiger(), statut, recherche, mesDemandes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeDto.Detail> detail(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.detail(id, utilisateurCourant.exiger()));
    }

    @PostMapping
    public ResponseEntity<DemandeDto.Resume> creer(@Valid @RequestBody DemandeDto.Creation demande) {
        return ResponseEntity.ok(demandeService.creer(utilisateurCourant.exiger(), demande));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeDto.Resume> modifier(@PathVariable Long id,
                                                     @Valid @RequestBody DemandeDto.Modification demande) {
        return ResponseEntity.ok(demandeService.modifierEntete(id, utilisateurCourant.exiger(), demande));
    }

    @PostMapping("/{id}/lignes")
    public ResponseEntity<DemandeDto.LigneResume> ajouterLigne(@PathVariable Long id,
                                                                @Valid @RequestBody DemandeDto.LigneCreation ligne) {
        return ResponseEntity.ok(demandeService.ajouterLigne(id, utilisateurCourant.exiger(), ligne));
    }

    @PutMapping("/{id}/lignes/{idLigne}")
    public ResponseEntity<DemandeDto.LigneResume> modifierLigne(@PathVariable Long id,
                                                                @PathVariable Long idLigne,
                                                                @Valid @RequestBody DemandeDto.LigneModification ligne) {
        return ResponseEntity.ok(demandeService.modifierLigne(id, idLigne, utilisateurCourant.exiger(), ligne));
    }

    @DeleteMapping("/{id}/lignes/{idLigne}")
    public ResponseEntity<Void> supprimerLigne(@PathVariable Long id, @PathVariable Long idLigne) {
        demandeService.supprimerLigne(id, idLigne, utilisateurCourant.exiger());
        return ResponseEntity.noContent().build();
    }

    /** Soumission de la demande : toutes les lignes entrent dans le circuit de validation. */
    @PostMapping("/{id}/soumettre")
    public ResponseEntity<DemandeDto.Resume> soumettre(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.soumettre(id, utilisateurCourant.exiger()));
    }

    /** Resoumission d'une ligne apres correction (RM-038). */
    @PostMapping("/{id}/lignes/{idLigne}/resoumettre")
    public ResponseEntity<DemandeDto.Resume> resoumettre(@PathVariable Long id, @PathVariable Long idLigne) {
        return ResponseEntity.ok(demandeService.resoumettreLigne(id, idLigne, utilisateurCourant.exiger()));
    }

    @PostMapping("/{id}/lignes/{idLigne}/annuler")
    public ResponseEntity<DemandeDto.Resume> annulerLigne(@PathVariable Long id, @PathVariable Long idLigne) {
        return ResponseEntity.ok(demandeService.annulerLigne(id, idLigne, utilisateurCourant.exiger()));
    }
}
