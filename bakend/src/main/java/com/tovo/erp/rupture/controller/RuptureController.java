package com.tovo.erp.rupture.controller;

import com.tovo.erp.rupture.dto.RuptureDtos;
import com.tovo.erp.rupture.service.DossierRuptureService;
import com.tovo.erp.rupture.service.SoldeRuptureService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rupture")
public class RuptureController {

    private final DossierRuptureService dossiers;
    private final SoldeRuptureService solde;

    public RuptureController(DossierRuptureService dossiers, SoldeRuptureService solde) {
        this.dossiers = dossiers;
        this.solde = solde;
    }

    @GetMapping("/dossiers")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public List<RuptureDtos.DossierDto> lister(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String motif,
            @RequestParam(required = false) String recherche) {
        return dossiers.lister(statut, motif, recherche);
    }

    @GetMapping("/dossiers/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public Map<String, Object> detail(@PathVariable Long id) {
        return dossiers.detail(id);
    }

    @PostMapping("/dossiers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RuptureDtos.DossierDto creer(@Valid @RequestBody RuptureDtos.CreationDossier demande) {
        return dossiers.creer(demande);
    }

    @PostMapping("/dossiers/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RuptureDtos.DossierDto valider(@PathVariable Long id) {
        return dossiers.valider(id);
    }

    @PostMapping("/dossiers/{id}/notifier")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RuptureDtos.DossierDto notifier(@PathVariable Long id) {
        return dossiers.notifier(id);
    }

    @PatchMapping("/dossiers/{id}/date-sortie")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RuptureDtos.DossierDto modifierDateSortie(
            @PathVariable Long id,
            @Valid @RequestBody RuptureDtos.ModificationDateSortie demande) {
        return dossiers.modifierDateSortie(id, demande);
    }

    @PostMapping("/dossiers/{id}/arreter-droits")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RuptureDtos.DroitsDto arreterDroits(@PathVariable Long id) {
        return dossiers.arreterDroits(id);
    }

    @PostMapping("/dossiers/{id}/solde/preparer")
    @PreAuthorize("hasAnyRole('FINANCE', 'RH', 'DIRECTION')")
    public RuptureDtos.SoldeDto preparerSolde(@PathVariable Long id) {
        return solde.preparer(id);
    }

    @PostMapping("/dossiers/{id}/solde/valider")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RuptureDtos.SoldeDto validerSolde(
            @PathVariable Long id,
            @RequestBody(required = false) RuptureDtos.DecisionSolde decision) {
        return solde.valider(id, decision);
    }

    @PostMapping("/dossiers/{id}/solde/payer")
    @PreAuthorize("hasAnyRole('FINANCE', 'DIRECTION')")
    public RuptureDtos.SoldeDto payerSolde(
            @PathVariable Long id,
            @Valid @RequestBody RuptureDtos.PaiementSolde demande) {
        return solde.payer(id, demande);
    }

    @PostMapping("/dossiers/{id}/documents/generer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public List<RuptureDtos.DocumentDto> genererDocuments(@PathVariable Long id) {
        return dossiers.genererDocuments(id);
    }

    @PostMapping("/documents/{id}/remettre")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RuptureDtos.DocumentDto remettreDocument(
            @PathVariable Long id,
            @Valid @RequestBody RuptureDtos.RemiseDocument demande) {
        return dossiers.remettreDocument(id, demande);
    }

    @PatchMapping("/dossiers/{idDossier}/taches/{idTache}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public RuptureDtos.TacheDto gererTache(
            @PathVariable Long idDossier,
            @PathVariable Long idTache,
            @Valid @RequestBody RuptureDtos.TacheRequest demande) {
        return dossiers.gererTache(idDossier, idTache, demande);
    }

    @PostMapping("/dossiers/{id}/materiels")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public RuptureDtos.MaterielDto ajouterMateriel(
            @PathVariable Long id,
            @Valid @RequestBody RuptureDtos.MaterielRequest demande) {
        return dossiers.ajouterMateriel(id, demande);
    }

    @PostMapping("/dossiers/{id}/entretien")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'EMPLOYE')")
    public RuptureDtos.EntretienDto enregistrerEntretien(
            @PathVariable Long id,
            @Valid @RequestBody RuptureDtos.EntretienRequest demande) {
        return dossiers.enregistrerEntretien(id, demande);
    }

    @PostMapping("/dossiers/{id}/cloturer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RuptureDtos.DossierDto cloturer(@PathVariable Long id) {
        return dossiers.cloturer(id);
    }

    @PostMapping("/dossiers/{id}/archiver")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public RuptureDtos.DossierDto archiver(@PathVariable Long id) {
        return dossiers.archiver(id);
    }

    @GetMapping("/indicateurs")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public RuptureDtos.IndicateursDto indicateurs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return dossiers.indicateurs(debut, fin);
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER', 'FINANCE')")
    public Map<String, Object> types() {
        return dossiers.types();
    }
}
