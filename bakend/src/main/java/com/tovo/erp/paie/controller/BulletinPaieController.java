package com.tovo.erp.paie.controller;

import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.service.BulletinPaieService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/paie")
@PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
public class BulletinPaieController {
    private final BulletinPaieService service;

    public BulletinPaieController(BulletinPaieService service) {
        this.service = service;
    }

    @GetMapping("/parametres")
    public BulletinPaieDtos.ParametresDto parametres() {
        return service.parametres();
    }

    @PutMapping("/parametres")
    public BulletinPaieDtos.ParametresDto enregistrerParametres(
            @Valid @RequestBody BulletinPaieDtos.ParametresRequest request) {
        return service.enregistrerParametres(request);
    }

    @PostMapping("/periodes/calcul")
    public BulletinPaieDtos.PeriodeAvecBulletinsDto calculer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mois) {
        return service.calculer(mois);
    }

    @PostMapping("/periodes/{idPeriode}/consolider")
    public BulletinPaieDtos.PeriodeDto consolider(@PathVariable Long idPeriode) {
        return service.consolider(idPeriode);
    }

    @PostMapping("/periodes/{idPeriode}/valider")
    public BulletinPaieDtos.PeriodeDto valider(@PathVariable Long idPeriode) {
        return service.valider(idPeriode);
    }

    @PostMapping("/periodes/{idPeriode}/statut")
    public BulletinPaieDtos.PeriodeDto changerStatut(@PathVariable Long idPeriode,
                                                     @RequestParam String statut) {
        return service.changerStatut(idPeriode, statut);
    }

    @GetMapping("/periodes/{idPeriode}")
    public BulletinPaieDtos.PeriodeAvecBulletinsDto lire(@PathVariable Long idPeriode) {
        return service.lire(idPeriode);
    }

    @GetMapping("/periodes")
    public java.util.List<BulletinPaieDtos.PeriodeDto> periodes() {
        return service.periodes();
    }

    @PutMapping("/bulletins/{idBulletin}/retenue")
    public BulletinPaieDtos.BulletinDto retenue(
            @PathVariable Long idBulletin,
            @Valid @RequestBody BulletinPaieDtos.RetenueRequest request) {
        return service.enregistrerRetenue(idBulletin, request);
    }

    @PutMapping("/bulletins/{idBulletin}/ajustement")
    public BulletinPaieDtos.BulletinDto ajuster(
            @PathVariable Long idBulletin,
            @Valid @RequestBody BulletinPaieDtos.AjustementRequest request) {
        return service.ajuster(idBulletin, request);
    }

    @GetMapping("/bulletins/{idBulletin}")
    public BulletinPaieDtos.BulletinDetailDto detail(@PathVariable Long idBulletin) {
        return service.detail(idBulletin);
    }

    @GetMapping("/bulletins/{idBulletin}/lignes")
    public java.util.List<BulletinPaieDtos.LigneDto> lignes(@PathVariable Long idBulletin) {
        return service.lignes(idBulletin);
    }

    @PostMapping("/periodes/{idPeriode}/cloturer")
    public BulletinPaieDtos.PeriodeDto cloturer(@PathVariable Long idPeriode) {
        return service.cloturer(idPeriode);
    }
}
