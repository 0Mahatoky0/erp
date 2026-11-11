package com.tovo.erp.conges.controller;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.model.StatutAbsence;
import com.tovo.erp.conges.model.StatutDemandeConge;
import com.tovo.erp.conges.service.CongeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conges")
public class CongeController {
    private final CongeService service;

    public CongeController(CongeService service) {
        this.service = service;
    }

    @GetMapping("/types")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.TypeDto> types() {
        return service.types();
    }

    @GetMapping("/employes/{idEmploye}/soldes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.SoldeDto> soldes(@PathVariable Long idEmploye,
                                           @RequestParam(required = false) Integer annee) {
        return service.soldes(idEmploye, annee == null ? java.time.LocalDate.now().getYear() : annee);
    }

    @GetMapping("/demandes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.DemandeDto> demandes(@RequestParam(required = false) StatutDemandeConge statut) {
        return service.demandes(statut);
    }

    @GetMapping("/employes/{idEmploye}/demandes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<CongeDtos.DemandeDto> demandesEmploye(@PathVariable Long idEmploye) {
        return service.demandesEmploye(idEmploye);
    }

    @PostMapping("/demandes")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto creerDemande(@Valid @RequestBody CongeDtos.DemandeRequest request) {
        return service.creerDemande(request);
    }

    @PostMapping("/demandes/{id}/valider")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto valider(@PathVariable Long id, @RequestBody(required = false) CongeDtos.DecisionRequest request) {
        return service.decision(id, request, true);
    }

    @PostMapping("/demandes/{id}/refuser")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto refuser(@PathVariable Long id, @RequestBody(required = false) CongeDtos.DecisionRequest request) {
        return service.decision(id, request, false);
    }

    @GetMapping("/absences")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.AbsenceDto> absences(@RequestParam(required = false) StatutAbsence statut) {
        return service.absences(statut);
    }

    @PostMapping("/absences")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.AbsenceDto creerAbsence(@Valid @RequestBody CongeDtos.AbsenceRequest request) {
        return service.creerAbsence(request);
    }

    @PostMapping("/absences/{id}/justifier")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.AbsenceDto justifier(@PathVariable Long id, @RequestBody CongeDtos.DecisionRequest request) {
        return service.justifier(id, request == null ? null : request.idValidateur(),
                request == null ? null : request.motif());
    }
}
