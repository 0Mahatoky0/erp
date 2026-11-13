package com.tovo.erp.employe.controller;

import com.tovo.erp.employe.dto.AlerteDto;
import com.tovo.erp.employe.dto.DemandeModificationDtos.CreationDemandeRequest;
import com.tovo.erp.employe.dto.DemandeModificationDtos.DecisionDemandeRequest;
import com.tovo.erp.employe.dto.DemandeModificationDtos.DemandeModificationDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheRequest;
import com.tovo.erp.employe.dto.DossierRhDto;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.dto.MissionDtos.CreationMissionRequest;
import com.tovo.erp.employe.dto.MissionDtos.MissionDto;
import com.tovo.erp.employe.dto.MissionDtos.ModificationMissionRequest;
import com.tovo.erp.employe.dto.MissionDtos.SuiviDto;
import com.tovo.erp.employe.dto.MissionDtos.SuiviRequest;
import com.tovo.erp.employe.service.AlerteService;
import com.tovo.erp.employe.service.DemandeModificationService;
import com.tovo.erp.employe.service.DossierRhService;
import com.tovo.erp.employe.service.MissionService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;

/**
 * API RH dediee au dossier administratif complet (bloc 5). Seul le role RH y
 * accede ; les autres roles utilisent la vue minimale du referentiel.
 */
@RestController
@RequestMapping("/api/rh/dossiers")
@PreAuthorize("hasRole('RH')")
public class DossierRhController {

    private final DossierRhService dossiers;
    private final AlerteService alertes;
    private final MissionService missions;
    private final DemandeModificationService demandes;
    private final UtilisateurCourant utilisateurCourant;

    public DossierRhController(DossierRhService dossiers,
                               AlerteService alertes,
                               MissionService missions,
                               DemandeModificationService demandes,
                               UtilisateurCourant utilisateurCourant) {
        this.dossiers = dossiers;
        this.alertes = alertes;
        this.missions = missions;
        this.demandes = demandes;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public List<EmployeDto.Liste> lister(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long idService,
            @RequestParam(required = false) String recherche) {
        return dossiers.lister(statut, idService, recherche);
    }

    @GetMapping("/{id}")
    public DossierRhDto dossier(@PathVariable Long id) {
        return dossiers.dossierComplet(id);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentEmbaucheDto ajouterDocument(@PathVariable Long id,
                                               @Valid @RequestBody DocumentEmbaucheRequest requete) {
        return dossiers.ajouterDocument(id, requete);
    }

    @PatchMapping("/{id}/documents/{idDocument}/statut")
    public DocumentEmbaucheDto changerStatutDocument(@PathVariable Long id,
                                                     @PathVariable Long idDocument,
                                                     @Valid @RequestBody StatutDocumentRequest requete) {
        return dossiers.changerStatutDocument(id, idDocument, requete.statut());
    }

    @GetMapping("/{id}/alertes")
    public List<AlerteDto> alertes(@PathVariable Long id) {
        return alertes.lister(id);
    }

    @PostMapping("/{id}/alertes/recalcul")
    public List<AlerteDto> recalculerAlertes(@PathVariable Long id) {
        return alertes.recalculer(id);
    }

    @PatchMapping("/alertes/{idAlerte}/lue")
    public AlerteDto marquerAlerteLue(@PathVariable Long idAlerte) {
        return alertes.marquerLue(idAlerte);
    }

    @GetMapping("/alertes")
    public List<AlerteDto> toutesLesAlertes() {
        return alertes.listerToutes();
    }

    @GetMapping("/{id}/missions")
    public List<MissionDto> missions(@PathVariable Long id) {
        return missions.lister(id);
    }

    @PostMapping("/{id}/missions")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionDto creerMission(@PathVariable Long id,
                                   @Valid @RequestBody CreationMissionRequest requete) {
        return missions.creer(id, requete);
    }

    @PatchMapping("/missions/{idMission}")
    public MissionDto modifierMission(@PathVariable Long idMission,
                                      @Valid @RequestBody ModificationMissionRequest requete) {
        return missions.modifier(idMission, requete);
    }

    @GetMapping("/missions/{idMission}/suivis")
    public List<SuiviDto> suivis(@PathVariable Long idMission) {
        return missions.suivis(idMission);
    }

    @PostMapping("/missions/{idMission}/suivis")
    @ResponseStatus(HttpStatus.CREATED)
    public SuiviDto ajouterSuivi(@PathVariable Long idMission,
                                 @Valid @RequestBody SuiviRequest requete) {
        return missions.ajouterSuivi(idMission, requete, utilisateurCourant.exiger().getId());
    }

    @GetMapping("/{id}/demandes-modification")
    public List<DemandeModificationDto> demandes(@PathVariable Long id) {
        return demandes.lister(id);
    }

    @PostMapping("/{id}/demandes-modification")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeModificationDto creerDemande(@PathVariable Long id,
                                               @Valid @RequestBody CreationDemandeRequest requete) {
        return demandes.creer(id, requete, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/demandes-modification/{idDemande}/decision")
    public DemandeModificationDto deciderDemande(@PathVariable Long idDemande,
                                                 @Valid @RequestBody DecisionDemandeRequest requete) {
        return demandes.decider(idDemande, requete.approuver(), requete.motifDecision(),
                utilisateurCourant.exiger().getId());
    }

    public record StatutDocumentRequest(@NotBlank String statut) {
    }
}
