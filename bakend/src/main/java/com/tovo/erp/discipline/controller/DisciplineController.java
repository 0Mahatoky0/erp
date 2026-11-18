package com.tovo.erp.discipline.controller;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.service.AccesDisciplinaireService;
import com.tovo.erp.discipline.service.IndicateurDisciplineService;
import com.tovo.erp.discipline.service.IncidentService;
import com.tovo.erp.discipline.service.JournalDisciplineService;
import com.tovo.erp.discipline.service.PieceService;
import com.tovo.erp.discipline.service.ProcedureService;
import com.tovo.erp.discipline.service.SanctionService;
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
@RequestMapping("/api/discipline")
public class DisciplineController {

    private final IncidentService incidents;
    private final ProcedureService procedures;
    private final SanctionService sanctions;
    private final PieceService pieces;
    private final AccesDisciplinaireService acces;
    private final JournalDisciplineService journal;
    private final IndicateurDisciplineService indicateurs;

    public DisciplineController(IncidentService incidents, ProcedureService procedures,
                                SanctionService sanctions, PieceService pieces,
                                AccesDisciplinaireService acces, JournalDisciplineService journal,
                                IndicateurDisciplineService indicateurs) {
        this.incidents = incidents;
        this.procedures = procedures;
        this.sanctions = sanctions;
        this.pieces = pieces;
        this.acces = acces;
        this.journal = journal;
        this.indicateurs = indicateurs;
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.IncidentDto> incidents(@RequestParam String motif) {
        return incidents.lister(motif);
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'RH', 'DIRECTION')")
    public DisciplineDtos.IncidentDto declarer(
            @Valid @RequestBody DisciplineDtos.IncidentRequest requete,
            @RequestParam String motif) {
        return incidents.creer(requete, motif);
    }

    @GetMapping("/incidents/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'RH', 'DIRECTION', 'EMPLOYE')")
    public DisciplineDtos.IncidentDto incident(@PathVariable Long id, @RequestParam String motif) {
        return incidents.incident(id, motif);
    }

    @PostMapping("/incidents/{id}/qualifier")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.IncidentDto qualifier(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.QualificationRequest requete) {
        return incidents.qualifier(id, requete);
    }

    @GetMapping("/procedures")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.ProcedureDto> procedures(
            @RequestParam(required = false) Long idEmploye) {
        return procedures.lister(idEmploye);
    }

    @GetMapping("/procedures/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public DisciplineDtos.ProcedureDto procedure(@PathVariable Long id, @RequestParam String motif) {
        return procedures.procedure(id, motif);
    }

    @PostMapping("/procedures/{id}/explication")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public DisciplineDtos.ProcedureDto demanderExplication(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.EtapeRequest requete,
            @RequestParam String motif) {
        return procedures.demanderExplication(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/reponse")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'EMPLOYE')")
    public DisciplineDtos.ProcedureDto enregistrerReponse(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.EtapeRequest requete,
            @RequestParam String motif) {
        return procedures.enregistrerReponse(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/sans-reponse")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto constaterSansReponse(
            @PathVariable Long id, @RequestParam String motif) {
        return procedures.constaterSansReponse(id, motif);
    }

    @PostMapping("/procedures/{id}/convocation")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public DisciplineDtos.ProcedureDto convoquer(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.EtapeRequest requete,
            @RequestParam String motif) {
        return procedures.convoquer(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/entretien")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public DisciplineDtos.ProcedureDto enregistrerEntretien(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.EtapeRequest requete,
            @RequestParam String motif) {
        return procedures.enregistrerEntretien(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/deliberation")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto deliberer(@PathVariable Long id, @RequestParam String motif) {
        return procedures.deliberer(id, motif);
    }

    @PostMapping("/procedures/{id}/decision")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto decider(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.DecisionRequest requete,
            @RequestParam String motif) {
        return procedures.decider(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/notification")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto notifier(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.NotificationRequest requete,
            @RequestParam String motif) {
        return procedures.notifier(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/recours")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'EMPLOYE')")
    public DisciplineDtos.ProcedureDto contester(
            @PathVariable Long id, @Valid @RequestBody DisciplineDtos.RecoursRequest requete,
            @RequestParam String motif) {
        return procedures.contester(id, requete, motif);
    }

    @PostMapping("/procedures/{id}/cloture")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto cloturer(@PathVariable Long id, @RequestParam String motif) {
        return procedures.cloturer(id, motif);
    }

    @PostMapping("/procedures/{id}/annulation")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.ProcedureDto annuler(@PathVariable Long id, @RequestParam String motif) {
        return procedures.annuler(id, motif);
    }

    @GetMapping("/sanctions")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.SanctionDto> sanctions(
            @RequestParam(required = false) Long idEmploye) {
        return sanctions.lister(idEmploye);
    }

    @GetMapping("/sanctions/variables")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE')")
    public List<DisciplineDtos.VariablePaieDto> variables(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return sanctions.variables(debut, fin);
    }

    @GetMapping("/absences")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'FINANCE')")
    public List<DisciplineDtos.AbsenceDto> absences(@RequestParam(required = false) Long idEmploye) {
        return sanctions.absences(idEmploye);
    }

    @PostMapping("/absences/{id}/traiter")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE')")
    public DisciplineDtos.AbsenceDto marquerTraitee(@PathVariable Long id) {
        return sanctions.marquerTraitee(id);
    }

    @GetMapping("/pieces/incident/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<DisciplineDtos.PieceDto> piecesIncident(@PathVariable Long id, @RequestParam String motif) {
        return pieces.listerIncident(id, motif);
    }

    @GetMapping("/pieces/procedure/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public List<DisciplineDtos.PieceDto> piecesProcedure(@PathVariable Long id, @RequestParam String motif) {
        return pieces.listerProcedure(id, motif);
    }

    @PostMapping("/pieces")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public DisciplineDtos.PieceDto ajouterPiece(
            @Valid @RequestBody DisciplineDtos.PieceRequest requete, @RequestParam String motif) {
        return pieces.ajouter(requete, motif);
    }

    @GetMapping("/acces")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.AccesDto> acces(@RequestParam Long idProcedure) {
        return acces.lister(idProcedure);
    }

    @PostMapping("/acces")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.AccesDto accorderAcces(
            @Valid @RequestBody DisciplineDtos.AccesRequest requete) {
        return acces.accorder(requete);
    }

    @PostMapping("/acces/{id}/revoquer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public void revoquerAcces(@PathVariable Long id, @RequestParam String motif) {
        acces.revoquer(id, motif);
    }

    @GetMapping("/journal")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.JournalDto> journal(@RequestParam String motif) {
        return journal.lire();
    }

    @GetMapping("/journal/{ressource}/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<DisciplineDtos.JournalDto> journalRessource(
            @PathVariable String ressource, @PathVariable Long id, @RequestParam String motif) {
        return journal.parDossier(ressource, id, motif);
    }

    @GetMapping("/indicateurs")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public DisciplineDtos.IndicateursDto indicateurs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return indicateurs.indicateurs(debut, fin);
    }
}
