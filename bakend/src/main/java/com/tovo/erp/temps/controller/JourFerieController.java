package com.tovo.erp.temps.controller;

import com.tovo.erp.temps.dto.TempsDtos.JourFerieDto;
import com.tovo.erp.temps.dto.TempsDtos.JourFerieRequest;
import com.tovo.erp.temps.service.JourFerieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultation et parametrage du calendrier des jours feries. */
@RestController
@RequestMapping("/api/temps/jours-feries")
public class JourFerieController {

    private final JourFerieService joursFeries;

    public JourFerieController(JourFerieService joursFeries) {
        this.joursFeries = joursFeries;
    }

    @GetMapping
    public List<JourFerieDto> lister(@RequestParam(required = false) Integer annee) {
        return joursFeries.lister(annee);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public JourFerieDto creer(@Valid @RequestBody JourFerieRequest requete) {
        return joursFeries.creer(requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public void supprimer(@PathVariable Long id) {
        joursFeries.supprimer(id);
    }
}
