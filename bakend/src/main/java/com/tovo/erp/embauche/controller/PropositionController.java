package com.tovo.erp.embauche.controller;

import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.service.PropositionService;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestion interne des propositions d'embauche (section 5.2 et 5.3). */
@RestController
@RequestMapping("/api/embauche/propositions")
public class PropositionController {

    private final PropositionService propositions;
    private final UtilisateurCourant utilisateurCourant;

    public PropositionController(PropositionService propositions,
                                 UtilisateurCourant utilisateurCourant) {
        this.propositions = propositions;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public PropositionDtos.Detail creer(@Valid @RequestBody PropositionDtos.Creation demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return propositions.creer(utilisateurCourant.exiger(), demande);
    }

    @GetMapping
    public List<PropositionDtos.Resume> lister(@RequestParam(required = false) PropositionStatut statut) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return propositions.lister(statut);
    }

    @GetMapping("/{id}")
    public PropositionDtos.Detail detail(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return propositions.detail(id);
    }

    @PutMapping("/{id}")
    public PropositionDtos.Detail modifier(@PathVariable Long id,
                                           @Valid @RequestBody PropositionDtos.Creation demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return propositions.modifier(id, utilisateurCourant.exiger(), demande);
    }

    @PostMapping("/{id}/valider")
    public PropositionDtos.Detail valider(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.FINANCE, RoleNom.DIRECTION);
        return propositions.valider(id, utilisateurCourant.exiger());
    }

    @PostMapping("/{id}/envoyer")
    public PropositionDtos.Detail envoyer(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return propositions.envoyer(id, utilisateurCourant.exiger());
    }

    @PostMapping("/{id}/retirer")
    public PropositionDtos.Detail retirer(@PathVariable Long id,
                                          @RequestBody(required = false) PropositionDtos.Retrait demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        String motif = demande == null ? null : demande.motif();
        return propositions.retirer(id, utilisateurCourant.exiger(), motif);
    }

    @PostMapping("/{id}/prolonger")
    public PropositionDtos.Detail prolonger(@PathVariable Long id,
                                            @Valid @RequestBody PropositionDtos.Prolongation demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return propositions.prolonger(id, utilisateurCourant.exiger(), demande.jours());
    }

    @PostMapping("/{id}/repondre")
    public PropositionDtos.Detail repondre(@PathVariable Long id,
                                           @Valid @RequestBody PropositionDtos.ReponseInterne demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return propositions.repondrePourCandidat(id, utilisateurCourant.exiger(), demande);
    }
}
