package com.tovo.erp.besoin.controller;

import com.tovo.erp.besoin.dto.ReferentielDto;
import com.tovo.erp.besoin.service.ReferentielService;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Referentiel : departements, services et employes. */
@RestController
@RequestMapping("/api/referentiel")
public class ReferentielController {

    private final ReferentielService referentielService;
    private final EmployeService employeService;

    public ReferentielController(ReferentielService referentielService,
                                 EmployeService employeService) {
        this.referentielService = referentielService;
        this.employeService = employeService;
    }

    @GetMapping("/departements")
    public List<ReferentielDto.DepartementListe> departements() {
        return referentielService.departements();
    }

    @GetMapping("/services")
    public List<ReferentielDto.ServiceListe> services(@RequestParam(required = false) Long idDepartement) {
        return referentielService.services(idDepartement);
    }

    @GetMapping("/employes")
    public List<EmployeDto.Liste> employes(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) String recherche) {
        return employeService.lister(statut, idService, recherche);
    }
}
