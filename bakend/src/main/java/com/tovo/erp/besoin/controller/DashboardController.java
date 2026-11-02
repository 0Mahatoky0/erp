package com.tovo.erp.web.controller;

import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.service.DashboardService;
import com.tovo.erp.web.dto.DashboardDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord du role connecte. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UtilisateurCourant utilisateurCourant;

    public DashboardController(DashboardService dashboardService, UtilisateurCourant utilisateurCourant) {
        this.dashboardService = dashboardService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<DashboardDto.Resume> synthese(@RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(dashboardService.synthese(utilisateurCourant.exiger(), exercice));
    }
}
