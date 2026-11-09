package com.tovo.erp.embauche.controller;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.embauche.service.ContratCandidatService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Contrats et signatures vus par le candidat connecte (RG-B4-05). */
@RestController
@RequestMapping("/api/contrats")
@PreAuthorize("hasRole('CANDIDAT')")
public class ContratCandidatController {

    private final ContratCandidatService contrats;
    private final UtilisateurCourant utilisateurCourant;

    public ContratCandidatController(ContratCandidatService contrats,
                                     UtilisateurCourant utilisateurCourant) {
        this.contrats = contrats;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/mes-contrats")
    public List<ContratDtos.ContratDto> mesContrats() {
        return contrats.mesContrats(utilisateurCourant.exiger().getId());
    }

    @GetMapping("/mes-contrats/{id}")
    public ContratDtos.ContratDto monContrat(@PathVariable Long id) {
        return contrats.monContrat(id, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/mes-contrats/{id}/signer")
    public ContratDtos.ContratDto signerMonContrat(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.SignatureSalarieRequest demande,
            HttpServletRequest request) {
        return contrats.signer(id, utilisateurCourant.exiger().getId(), demande,
                request.getRemoteAddr());
    }

    @PostMapping("/{id}/signer-salarie")
    public ContratDtos.ContratDto signerSalarie(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.SignatureSalarieRequest demande,
            HttpServletRequest request) {
        return contrats.signer(id, utilisateurCourant.exiger().getId(), demande,
                request.getRemoteAddr());
    }
}
