package com.tovo.erp.embauche.controller;

import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.service.EmbaucheService;
import com.tovo.erp.embauche.service.PropositionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acces public par token : le candidat consulte la proposition et repond
 * sans compte applicatif. Le token est a usage unique pour l'acceptation.
 */
@RestController
@RequestMapping("/api/embauche/propositions/token")
public class PropositionPubliqueController {

    private final PropositionService propositions;
    private final EmbaucheService embauche;

    public PropositionPubliqueController(PropositionService propositions, EmbaucheService embauche) {
        this.propositions = propositions;
        this.embauche = embauche;
    }

    @GetMapping("/{token}")
    public PropositionDtos.VuePublique consulter(@PathVariable String token) {
        return propositions.vuePublique(token);
    }

    @PostMapping("/{token}/accepter")
    public PropositionDtos.ResultatAcceptation accepter(@PathVariable String token,
                                                        @RequestBody(required = false)
                                                        PropositionDtos.AcceptationPublique demande,
                                                        HttpServletRequest request) {
        return embauche.accepterPublique(token, request.getRemoteAddr(), null);
    }

    @PostMapping("/{token}/refuser")
    public PropositionDtos.Detail refuser(@PathVariable String token,
                                          @Valid @RequestBody PropositionDtos.Refus demande,
                                          HttpServletRequest request) {
        return propositions.refuserPublique(token, demande.motif(), request.getRemoteAddr());
    }

    @PostMapping("/{token}/contre-proposer")
    public PropositionDtos.Detail contreProposer(@PathVariable String token,
                                                 @Valid @RequestBody PropositionDtos.ContreProposition demande,
                                                 HttpServletRequest request) {
        return propositions.contreProposerPublique(token, demande.message(), request.getRemoteAddr());
    }
}
