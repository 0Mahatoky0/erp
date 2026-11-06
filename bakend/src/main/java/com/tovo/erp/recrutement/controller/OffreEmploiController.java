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
