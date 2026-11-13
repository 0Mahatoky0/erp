package com.tovo.erp.besoin.controller;

import com.tovo.erp.besoin.dto.ReferentielDto;
import com.tovo.erp.besoin.service.ReferentielService;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
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
    private final UtilisateurCourant utilisateurCourant;

    public ReferentielController(ReferentielService referentielService,
                                 EmployeService employeService,
                                 UtilisateurCourant utilisateurCourant) {
        this.referentielService = referentielService;
        this.employeService = employeService;
        this.utilisateurCourant = utilisateurCourant;
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
    public List<?> employes(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) String recherche) {
        List<EmployeDto.Liste> liste = employeService.lister(statut, idService, recherche);
        if (utilisateurCourant.aLeRole(Roles.RH)) {
            return liste;
        }
        return liste.stream().map(EmployeDto.Minimal::de).toList();
    }
}
