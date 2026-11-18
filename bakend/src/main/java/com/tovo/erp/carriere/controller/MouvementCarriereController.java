package com.tovo.erp.carriere.controller;

import com.tovo.erp.carriere.dto.CarriereMobiliteDtos;
import com.tovo.erp.carriere.service.MouvementCarriereService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MouvementCarriereController {

    private final MouvementCarriereService service;

    public MouvementCarriereController(MouvementCarriereService service) {
        this.service = service;
    }

    @GetMapping("/mouvements")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public List<CarriereMobiliteDtos.MouvementDto> lister(
            @RequestParam(required = false) String statut) {
        return service.lister(statut);
    }

    @PostMapping("/mouvements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CarriereMobiliteDtos.MouvementDto creer(
            @Valid @RequestBody CarriereMobiliteDtos.MouvementRequest requete) {
        return service.creer(requete);
    }

    @GetMapping("/mouvements/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public CarriereMobiliteDtos.MouvementDto mouvement(@PathVariable Long id) {
        return service.mouvement(id);
    }

    @PostMapping("/mouvements/{id}/soumettre")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CarriereMobiliteDtos.MouvementDto soumettre(@PathVariable Long id) {
        return service.soumettre(id);
    }

    @PostMapping("/mouvements/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE', 'MANAGER')")
    public CarriereMobiliteDtos.MouvementDto valider(
            @PathVariable Long id,
            @RequestBody(required = false) CarriereMobiliteDtos.DecisionRequest requete) {
        return service.valider(id, requete == null ? new CarriereMobiliteDtos.DecisionRequest(null, null, null) : requete);
    }

    @PostMapping("/mouvements/{id}/appliquer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CarriereMobiliteDtos.MouvementDto appliquer(@PathVariable Long id) {
        return service.appliquer(id);
    }

    @PostMapping("/mouvements/{id}/annuler")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CarriereMobiliteDtos.MouvementDto annuler(
            @PathVariable Long id, @RequestParam String motif) {
        return service.annuler(id, motif);
    }

    @GetMapping("/employes/{idEmploye}/carriere")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE', 'EMPLOYE')")
    public CarriereMobiliteDtos.CarriereDateDto carriereALaDate(
            @PathVariable Long idEmploye,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate connu_le) {
        return service.carriereALaDate(idEmploye, date, connu_le);
    }

    @GetMapping("/employes/{idEmploye}/frise")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE', 'EMPLOYE')")
    public List<CarriereMobiliteDtos.FriseLigneDto> frise(@PathVariable Long idEmploye) {
        return service.frise(idEmploye);
    }

    @GetMapping("/carriere/integrite")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CarriereMobiliteDtos.RapportIntegriteDto rapportIntegrite() {
        return service.rapportIntegrite();
    }

    @GetMapping("/campagnes-augmentation")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE')")
    public List<CarriereMobiliteDtos.CampagneDto> campagnes() {
        return service.campagnes();
    }

    @PostMapping("/campagnes-augmentation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CarriereMobiliteDtos.CampagneDto creerCampagne(
            @Valid @RequestBody CarriereMobiliteDtos.CampagneRequest requete) {
        return service.creerCampagne(requete);
    }

    @PostMapping("/campagnes-augmentation/{id}/simuler")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE')")
    public CarriereMobiliteDtos.CampagneDto simulerCampagne(@PathVariable Long id) {
        return service.simulerCampagne(id);
    }
}
