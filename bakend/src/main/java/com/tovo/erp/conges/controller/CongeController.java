package com.tovo.erp.conges.controller;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.conges.service.SuppleanceService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conges")
public class CongeController {
    private final CongeService service;
    private final SuppleanceService suppleances;
    private final UtilisateurCourant utilisateurCourant;

    public CongeController(CongeService service, SuppleanceService suppleances,
                           UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.suppleances = suppleances;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/types")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<CongeDtos.TypeDto> types() {
        return service.types();
    }

    @GetMapping("/moi")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('EMPLOYE')")
    public CongeDtos.EmployeCourantDto employeCourant() {
        return service.employeCourant(utilisateurCourant.exiger());
    }

    @GetMapping("/moi/absences")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('EMPLOYE')")
    public List<CongeDtos.AbsenceDto> mesAbsences() {
        var utilisateur = utilisateurCourant.exiger();
        Long idEmploye = service.employeCourant(utilisateur).idEmploye();
        return service.absencesEmploye(idEmploye);
    }

    @GetMapping("/employes/{idEmploye}/soldes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.SoldeDto> soldes(@PathVariable Long idEmploye,
                                           @RequestParam(required = false) Integer annee) {
        service.verifierAccesEmploye(idEmploye, utilisateurCourant.exiger());
        return service.soldes(idEmploye, annee == null ? java.time.LocalDate.now().getYear() : annee);
    }

    @GetMapping("/demandes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.DemandeDto> demandes(@RequestParam(required = false) String statut) {
        return service.demandes(statut);
    }

    @GetMapping("/employes/{idEmploye}/demandes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<CongeDtos.DemandeDto> demandesEmploye(@PathVariable Long idEmploye) {
        service.verifierAccesEmploye(idEmploye, utilisateurCourant.exiger());
        return service.demandesEmploye(idEmploye);
    }

    @PostMapping("/demandes")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CongeDtos.DemandeDto creerDemande(@Valid @RequestBody CongeDtos.DemandeRequest request) {
        service.verifierAccesEmploye(request.idEmploye(), utilisateurCourant.exiger());
        return service.creerDemande(request);
    }

    @PostMapping("/demandes/{id}/valider")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto valider(@PathVariable Long id, @RequestBody(required = false) CongeDtos.DecisionRequest request) {
        return service.decision(id, request, true, utilisateurCourant.exiger());
    }

    @PostMapping("/demandes/{id}/refuser")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto refuser(@PathVariable Long id, @RequestBody(required = false) CongeDtos.DecisionRequest request) {
        return service.decision(id, request, false, utilisateurCourant.exiger());
    }

    @PostMapping("/demandes/{id}/annuler")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CongeDtos.DemandeDto annuler(@PathVariable Long id,
                                         @RequestBody(required = false) CongeDtos.AnnulationRequest request) {
        return service.annuler(id, request == null ? null : request.motif(), utilisateurCourant.exiger());
    }

    @PostMapping("/demandes/{id}/ecourter")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.DemandeDto ecourter(@PathVariable Long id,
                                          @Valid @RequestBody CongeDtos.EcourtementRequest request) {
        return service.ecourter(id, request, utilisateurCourant.exiger());
    }

    @PostMapping("/soldes/ajuster")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CongeDtos.SoldeDto ajuster(@Valid @RequestBody CongeDtos.AjustementRequest request) {
        return service.ajuster(request, utilisateurCourant.exiger().getId());
    }

    @GetMapping("/employes/{idEmploye}/mouvements")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.MouvementDto> mouvements(@PathVariable Long idEmploye,
                                                   @RequestParam(required = false) Long idTypeConge) {
        service.verifierAccesEmploye(idEmploye, utilisateurCourant.exiger());
        return service.mouvements(idEmploye, idTypeConge);
    }

    @PostMapping("/acquisition")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public int acquerir(@RequestParam int annee, @RequestParam int mois) {
        return service.acquerirMois(annee, mois);
    }

    @GetMapping("/absences")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.AbsenceDto> absences(@RequestParam(required = false) String statut) {
        return service.absences(statut);
    }

    @PostMapping("/absences")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CongeDtos.AbsenceDto creerAbsence(@Valid @RequestBody CongeDtos.AbsenceRequest request) {
        service.verifierAccesEmploye(request.idEmploye(), utilisateurCourant.exiger());
        return service.creerAbsence(request);
    }

    @PostMapping("/absences/{id}/justifier")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.AbsenceDto justifier(@PathVariable Long id, @RequestBody CongeDtos.DecisionRequest request) {
        return service.justifier(id, request == null ? null : request.idValidateur(),
                request == null ? null : request.motif());
    }

    @GetMapping("/periodes-bloquees")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.PeriodeBloqueeDto> periodesBloquees() {
        return service.periodesBloquees();
    }

    @PostMapping("/periodes-bloquees")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CongeDtos.PeriodeBloqueeDto creerPeriodeBloquee(
            @Valid @RequestBody CongeDtos.PeriodeBloqueeRequest request) {
        return service.creerPeriodeBloquee(request);
    }

    @DeleteMapping("/periodes-bloquees/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public void supprimerPeriodeBloquee(@PathVariable Long id) {
        service.supprimerPeriodeBloquee(id);
    }

    @GetMapping("/matrice-remplacement")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.MatriceRemplacementDto> matrice() {
        return suppleances.matrice();
    }

    @PostMapping("/matrice-remplacement")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public CongeDtos.MatriceRemplacementDto enregistrerMatrice(
            @RequestBody CongeDtos.MatriceRemplacementRequest request) {
        return suppleances.enregistrerMatrice(request);
    }

    @PostMapping("/demandes/{id}/suppleance")
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.SuppleanceDto proposerSuppleance(
            @PathVariable Long id, @RequestBody(required = false) CongeDtos.SuppleanceRequest request) {
        return suppleances.proposer(id, request);
    }

    @GetMapping("/demandes/{id}/suppleances")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.SuppleanceDto> suppleancesDeDemande(@PathVariable Long id) {
        return suppleances.deDemande(id);
    }

    @PostMapping("/suppleances/{id}/repondre")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public CongeDtos.SuppleanceDto repondreSuppleance(
            @PathVariable Long id, @Valid @RequestBody CongeDtos.ReponseSuppleanceRequest request) {
        return suppleances.repondre(id, request);
    }

    @PostMapping("/suppleances/{id}/terminer")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public CongeDtos.SuppleanceDto terminerSuppleance(@PathVariable Long id) {
        return suppleances.terminer(id);
    }

    @GetMapping("/employes/{idEmploye}/suppleances")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<CongeDtos.SuppleanceDto> suppleancesDuRemplacant(@PathVariable Long idEmploye) {
        return suppleances.deRemplacant(idEmploye);
    }

    @GetMapping("/planning")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<CongeDtos.DemandeDto> planning() {
        return service.planningConges();
    }

    @GetMapping("/variables-paie")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
    public List<CongeDtos.VariablePaieDto> variablesPaie() {
        return service.variablesPaie();
    }
}
