package com.tovo.erp.contrat.controller;

import com.tovo.erp.contrat.dto.CarriereDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Historique de remuneration et evenements de carriere (bloc 5). */
@RestController
@RequestMapping("/api/employes/{idEmploye}/carriere")
@PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
public class CarriereController {

    private final CarriereService carriere;
    private final UtilisateurCourant utilisateurCourant;

    public CarriereController(CarriereService carriere, UtilisateurCourant utilisateurCourant) {
        this.carriere = carriere;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/salaires")
    public List<CarriereDtos.SalaireDto> salaires(@PathVariable Long idEmploye) {
        return carriere.salaires(idEmploye);
    }

    @GetMapping("/salaires/courant")
    public CarriereDtos.SalaireDto salaireCourant(@PathVariable Long idEmploye) {
        return carriere.salaireCourant(idEmploye);
    }

    @PostMapping("/salaires")
    @ResponseStatus(HttpStatus.CREATED)
    public CarriereDtos.SalaireDto enregistrerSalaire(@PathVariable Long idEmploye,
                                                      @Valid @RequestBody CarriereDtos.SalaireRequest requete) {
        return carriere.enregistrerSalaire(idEmploye, requete, utilisateurCourant.exiger().getId());
    }

    @GetMapping("/evenements")
    public List<CarriereDtos.EvenementDto> evenements(@PathVariable Long idEmploye) {
        return carriere.evenements(idEmploye);
    }

    @PostMapping("/evenements")
    @ResponseStatus(HttpStatus.CREATED)
    public CarriereDtos.EvenementDto enregistrerEvenement(@PathVariable Long idEmploye,
                                                          @Valid @RequestBody CarriereDtos.EvenementRequest requete) {
        return carriere.enregistrerEvenement(idEmploye, requete, utilisateurCourant.exiger().getId());
    }
}
