package com.tovo.erp.temps.controller;

import com.tovo.erp.temps.dto.TempsDtos.AffectationRegimeDto;
import com.tovo.erp.temps.dto.TempsDtos.AffectationRegimeRequest;
import com.tovo.erp.temps.dto.TempsDtos.HoraireDto;
import com.tovo.erp.temps.dto.TempsDtos.HoraireRequest;
import com.tovo.erp.temps.dto.TempsDtos.RegimeDto;
import com.tovo.erp.temps.dto.TempsDtos.RegimeRequest;
import com.tovo.erp.temps.service.RegimeTravailService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Regimes, horaires types et affectations (bloc 6). */
@RestController
@RequestMapping("/api/temps")
public class RegimeTravailController {

    private final RegimeTravailService regimes;

    public RegimeTravailController(RegimeTravailService regimes) {
        this.regimes = regimes;
    }

    @GetMapping("/regimes")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<RegimeDto> lister() {
        return regimes.lister();
    }

    @GetMapping("/regimes/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RegimeDto regime(@PathVariable Long id) {
        return regimes.regime(id);
    }

    @PostMapping("/regimes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RegimeDto creer(@Valid @RequestBody RegimeRequest requete) {
        return regimes.creer(requete);
    }

    @PutMapping("/regimes/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RegimeDto modifier(@PathVariable Long id, @Valid @RequestBody RegimeRequest requete) {
        return regimes.modifier(id, requete);
    }

    @DeleteMapping("/regimes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public void supprimer(@PathVariable Long id) {
        regimes.supprimer(id);
    }

    @GetMapping("/regimes/{id}/horaires")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<HoraireDto> horaires(@PathVariable Long id) {
        return regimes.horaires(id);
    }

    @PostMapping("/regimes/{id}/horaires")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public HoraireDto ajouterHoraire(@PathVariable Long id, @Valid @RequestBody HoraireRequest requete) {
        return regimes.ajouterHoraire(id, requete);
    }

    @DeleteMapping("/horaires/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public void supprimerHoraire(@PathVariable Long id) {
        regimes.supprimerHoraire(id);
    }

    @GetMapping("/employes/{idEmploye}/affectations-regime")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<AffectationRegimeDto> affectations(@PathVariable Long idEmploye) {
        return regimes.affectations(idEmploye);
    }

    @GetMapping("/employes/{idEmploye}/regime-courant")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public AffectationRegimeDto regimeCourant(
            @PathVariable Long idEmploye,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return regimes.regimeCourant(idEmploye, date);
    }

    @PostMapping("/employes/{idEmploye}/affectations-regime")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public AffectationRegimeDto affecter(@PathVariable Long idEmploye,
                                         @Valid @RequestBody AffectationRegimeRequest requete) {
        return regimes.affecter(idEmploye, requete);
    }
}
