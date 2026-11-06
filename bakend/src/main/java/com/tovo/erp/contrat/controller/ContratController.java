package com.tovo.erp.contrat.controller;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.service.AvenantService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.contrat.service.OnboardingService;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Cycle de vie des contrats, affectations, avenants et onboarding (bloc 4). */
@RestController
@RequestMapping("/api/contrats")
public class ContratController {

    private final ContratService contrats;
    private final OnboardingService onboarding;
    private final AvenantService avenants;
    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;

    public ContratController(ContratService contrats, OnboardingService onboarding,
                             AvenantService avenants, EmployeService employes,
                             UtilisateurCourant utilisateurCourant) {
        this.contrats = contrats;
        this.onboarding = onboarding;
        this.avenants = avenants;
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @GetMapping
    public List<ContratDtos.ContratDto> lister(
            @RequestParam(required = false) ContratEtat etat) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.lister(etat);
    }

    @GetMapping("/actifs")
    public List<ContratDtos.ContratDto> actifs() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.actifs();
    }

    @GetMapping("/alertes")
    public List<ContratDtos.AlerteDto> alertes() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.alertes();
    }

    @GetMapping("/{id}")
    public ContratDtos.ContratDto contrat(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.contrat(id);
    }

    @GetMapping("/employe/{idEmploye}")
    public List<ContratDtos.ContratDto> contratsDe(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.contratsDe(idEmploye);
    }

    @GetMapping("/employe/{idEmploye}/courant")
    public ResponseEntity<ContratDtos.ContratDto> courant(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        ContratDtos.ContratDto courant = contrats.courant(idEmploye);
        return courant == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(courant);
    }

    @GetMapping("/employe/{idEmploye}/affectations")
    public List<ContratDtos.AffectationDto> affectationsDe(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.affectationsDe(idEmploye);
    }

    @GetMapping("/employe/{idEmploye}/onboarding")
    public ContratDtos.OnboardingDto onboardingDe(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.onboardingDeOuNull(idEmploye);
    }

    @GetMapping("/onboarding")
    public List<ContratDtos.OnboardingDto> onboardings() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return onboarding.onboardings();
    }

    @GetMapping("/onboarding/{id}")
    public ContratDtos.OnboardingDto onboarding(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return onboarding.onboarding(id);
    }

    @GetMapping("/{id}/onboarding")
    public ContratDtos.OnboardingDto onboardingDuContrat(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return onboarding.onboardingDuContrat(id);
    }

    @GetMapping("/{id}/avenants")
    public List<ContratDtos.AvenantDto> avenantsDe(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return avenants.avenantsDe(id);
    }

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    @PostMapping("/{id}/signer")
    public ContratDtos.ContratDto signer(
            @PathVariable Long id,
            @RequestBody(required = false) ContratDtos.SignatureRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        Long signataire = demande == null || demande.signataireEmployeurId() == null
                ? utilisateurCourant.exiger().getId()
                : demande.signataireEmployeurId();
        return contrats.signer(id, new ContratDtos.SignatureRequest(signataire));
    }

    @PostMapping("/{id}/valider")
    public ContratDtos.ContratDto valider(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return contrats.valider(id);
    }

    @PostMapping("/{id}/annuler")
    public ContratDtos.ContratDto annuler(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.AnnulationRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return contrats.annuler(id, demande);
    }

    @PostMapping("/{id}/renouveler")
    public ContratDtos.ContratDto renouveler(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.RenouvellementRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        ContratDtos.RenouvellementRequest complete = new ContratDtos.RenouvellementRequest(
                demande.dateFinPrevue(), demande.salaireBase(), demande.primesAvantages(),
                demande.horaires(), demande.lieuTravail(), demande.motifCdd(),
                demande.signataireEmployeurId() == null
                        ? utilisateurCourant.exiger().getId()
                        : demande.signataireEmployeurId());
        return contrats.renouveler(id, complete);
    }

    @PostMapping("/{id}/transformer-cdi")
    public ContratDtos.ContratDto transformerEnCdi(
            @PathVariable Long id,
            @RequestBody(required = false) ContratDtos.TransformationCdiRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        ContratDtos.TransformationCdiRequest complete = demande == null
                ? new ContratDtos.TransformationCdiRequest(null, null, null, null, null,
                        utilisateurCourant.exiger().getId())
                : new ContratDtos.TransformationCdiRequest(demande.dateDebut(),
                        demande.salaireBase(), demande.primesAvantages(), demande.horaires(),
                        demande.lieuTravail(), demande.signataireEmployeurId() == null
                                ? utilisateurCourant.exiger().getId()
                                : demande.signataireEmployeurId());
        return contrats.transformerEnCdi(id, complete);
    }

    @PostMapping("/{id}/avenants")
    public ContratDtos.AvenantDto creerAvenant(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.AvenantRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return avenants.creer(id, demande);
    }

    @PostMapping("/avenants/{idAvenant}/signer")
    public ContratDtos.AvenantDto signerAvenant(@PathVariable Long idAvenant) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return avenants.signer(idAvenant, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/avenants/{idAvenant}/valider")
    public ContratDtos.AvenantDto validerAvenant(@PathVariable Long idAvenant) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return avenants.valider(idAvenant);
    }

    @PostMapping("/employe/{idEmploye}/affectations")
    public ContratDtos.AffectationDto ajouterAffectation(
            @PathVariable Long idEmploye,
            @Valid @RequestBody ContratDtos.AffectationRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.ajouterAffectation(idEmploye, demande);
    }

    @PatchMapping("/taches/{idTache}")
    public ContratDtos.TacheDto mettreAJourTache(
            @PathVariable Long idTache,
            @Valid @RequestBody ContratDtos.TacheMiseAJourRequest demande) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return onboarding.mettreAJourTache(idTache, demande);
    }

    @PostMapping("/echeances/traiter")
    public ContratService.ResultatEcheances traiterEcheances() {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        return contrats.traiterEcheances();
    }

    // ------------------------------------------------------------------
    // Document
    // ------------------------------------------------------------------

    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        ContratDtos.ContratDto contrat = contrats.contrat(id);
        if (contrat.documentPdfId() == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        DocumentEmbaucheFichier fichier = employes.contenuDocument(contrat.documentPdfId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fichier.nomFichier() == null ? "contrat.pdf" : fichier.nomFichier(),
                                StandardCharsets.UTF_8)
                        .build().toString())
                .body(fichier.contenu());
    }
}
