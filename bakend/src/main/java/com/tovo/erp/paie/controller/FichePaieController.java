package com.tovo.erp.paie.controller;

import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.service.BulletinPaieService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiches-paie")
public class FichePaieController {
    private final BulletinPaieService service;

    public FichePaieController(BulletinPaieService service) {
        this.service = service;
    }

    @GetMapping("/periodes/{idPeriode}")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
    public List<BulletinPaieDtos.FicheDto> fiches(@PathVariable Long idPeriode) {
        return service.fiches(idPeriode);
    }

    @GetMapping("/employes/{idEmploye}")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION', 'EMPLOYE')")
    public List<BulletinPaieDtos.FicheDto> fichesEmploye(@PathVariable Long idEmploye) {
        return service.fichesEmploye(idEmploye);
    }

    @GetMapping("/miennes")
    @PreAuthorize("hasRole('EMPLOYE')")
    public List<BulletinPaieDtos.FicheDto> mesFiches() {
        return service.mesFiches();
    }
}
