package com.tovo.erp.besoin.controller;

import com.tovo.erp.besoin.model.Departement;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.besoin.repository.DepartementRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.iam.dto.EmployeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Referentiel : departements, services et employes. */
@RestController
@RequestMapping("/api/referentiel")
public class ReferentielController {

    private final DepartementRepository departementRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeService employeService;

    public ReferentielController(DepartementRepository departementRepository,
                                  ServiceRepository serviceRepository,
                                  EmployeService employeService) {
        this.departementRepository = departementRepository;
        this.serviceRepository = serviceRepository;
        this.employeService = employeService;
    }

    @GetMapping("/departements")
    public ResponseEntity<List<Departement>> departements() {
        return ResponseEntity.ok(departementRepository.findAll());
    }

    @GetMapping("/services")
    public ResponseEntity<List<Service>> services(@RequestParam(required = false) Long idDepartement) {
        if (idDepartement != null) {
            return ResponseEntity.ok(serviceRepository.findByDepartementIdOrderByNom(idDepartement));
        }
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @GetMapping("/employes")
    public ResponseEntity<List<EmployeDto.Liste>> employes(
            @RequestParam(required = false) StatutEmploye statut,
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(employeService.lister(statut, idService, recherche));
    }
}
