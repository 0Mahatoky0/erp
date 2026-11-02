package com.tovo.erp.web.controller;

import com.tovo.erp.domain.enums.RoleNom;
import com.tovo.erp.security.UtilisateurCourant;
import com.tovo.erp.service.BudgetService;
import com.tovo.erp.web.dto.BudgetDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Budgets annuels alloues aux services (RM-048 a RM-051). */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UtilisateurCourant utilisateurCourant;

    public BudgetController(BudgetService budgetService, UtilisateurCourant utilisateurCourant) {
        this.budgetService = budgetService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<List<BudgetDto.Liste>> lister(
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(budgetService.lister(idService, exercice));
    }

    @GetMapping("/{idService}/{exercice}")
    public ResponseEntity<BudgetDto.Liste> obtenir(@PathVariable Long idService, @PathVariable Integer exercice) {
        return ResponseEntity.ok(budgetService.obtenir(idService, exercice));
    }

    /** L'allocation d'un budget est reservee au role FINANCE. */
    @PutMapping
    public ResponseEntity<BudgetDto.Liste> definir(@Valid @RequestBody BudgetDto.MiseAJour demande) {
        utilisateurCourant.exigerRole(RoleNom.FINANCE);
        return ResponseEntity.ok(budgetService.definirBudget(demande));
    }
}
