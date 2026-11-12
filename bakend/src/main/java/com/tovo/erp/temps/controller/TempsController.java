package com.tovo.erp.temps.controller;

import com.tovo.erp.shared.UtilisateurCourant;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/temps")
public class TempsController {

    private final TempsService temps;
    private final UtilisateurCourant utilisateurCourant;

    public TempsController(TempsService temps, UtilisateurCourant utilisateurCourant) {
        this.temps = temps;
        this.utilisateurCourant = utilisateurCourant;
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

    @PostMapping("/employes/{idEmploye}/jours")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.JourTravailleDto calculerJour(
            @PathVariable Long idEmploye,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return temps.calculerJour(idEmploye, date);
    }

    @PostMapping("/employes/{idEmploye}/jours/{date}/valider")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.JourTravailleDto validerJour(
            @PathVariable Long idEmploye,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return temps.validerJour(idEmploye, date);
    }

    @PostMapping("/anomalies/{idAnomalie}/traitement")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.AnomalieDto traiterAnomalie(@PathVariable Long idAnomalie,
                                                 @Valid @RequestBody TempsDtos.AnomalieTraitementRequest demande) {
        utilisateurCourant.exigerRole("RH", "DIRECTION", "MANAGER");
        return temps.traiterAnomalie(idAnomalie, demande.statut(), demande.resolution(),
                utilisateurCourant.exiger().getId());
    }

    @PostMapping("/employes/{idEmploye}/heures-supplementaires/calcul")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.HeureSupplementaireDto calculerSemaine(
            @PathVariable Long idEmploye,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return temps.calculerSemaine(idEmploye, semaine);
    }

    @GetMapping("/periodes-cloturees")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<TempsDtos.PeriodeCloseDto> periodesCloturees() {
        return temps.periodesCloturees();
    }

    @PostMapping("/periodes-cloturees")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public TempsDtos.PeriodeCloseDto cloturer(@Valid @RequestBody TempsDtos.ClotureRequest demande) {
        return temps.cloturer(demande, utilisateurCourant.exiger().getId());
    }

    @GetMapping("/employes/{idEmploye}/heures-supplementaires")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public TempsDtos.HeureSupplementaireDto heuresSupplementaires(
            @PathVariable Long idEmploye,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine) {
        return temps.heuresSupplementaires(idEmploye, semaine);
    }
}
