package com.tovo.erp.temps.controller;

import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.service.TempsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/temps")
public class TempsController {

    private final TempsService temps;

    public TempsController(TempsService temps) {
        this.temps = temps;
    }

    @GetMapping("/employes/{idEmploye}/plannings")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<TempsDtos.PlanningDto> plannings(@PathVariable Long idEmploye) {
        return temps.plannings(idEmploye);
    }

    @PostMapping("/plannings")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.PlanningDto creerPlanning(@Valid @RequestBody TempsDtos.PlanningRequest demande) {
        return temps.creerPlanning(demande);
    }

    @PostMapping("/pointages/entree")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.PointageDto entree(@Valid @RequestBody TempsDtos.PointageRequest demande) {
        return temps.pointerEntree(demande);
    }

    @PostMapping("/pointages/sortie")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.PointageDto sortie(@Valid @RequestBody TempsDtos.PointageRequest demande) {
        return temps.pointerSortie(demande);
    }

    @GetMapping("/employes/{idEmploye}/pointages")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.PointageMoisDto pointages(
            @PathVariable Long idEmploye,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return temps.pointagesDuMois(idEmploye, mois == null ? YearMonth.now() : mois);
    }
}
