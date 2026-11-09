package com.tovo.erp.contrat.controller;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.AvenantService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.contrat.service.NotificationService;
import com.tovo.erp.contrat.service.OnboardingService;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Cycle de vie des contrats, affectations, avenants et onboarding (bloc 4). */
@RestController
@RequestMapping("/api/contrats")
public class ContratController {

    private final ContratService contrats;
    private final OnboardingService onboarding;
    private final AvenantService avenants;
    private final NotificationService notifications;
    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;

    public ContratController(ContratService contrats, OnboardingService onboarding,
                             AvenantService avenants, NotificationService notifications,
                             EmployeService employes, UtilisateurCourant utilisateurCourant) {
        this.contrats = contrats;
        this.onboarding = onboarding;
        this.avenants = avenants;
        this.notifications = notifications;
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.ContratDto> lister(
            @RequestParam(required = false) String etat) {
        return contrats.lister(etat);
    }

    @GetMapping("/actifs")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.ContratDto> actifs() {
        return contrats.actifs();
    }

    @GetMapping("/alertes")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.AlerteDto> alertes() {
        return contrats.alertes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.ContratDto contrat(@PathVariable Long id) {
        return contrats.contrat(id);
    }

    @GetMapping("/employe/{idEmploye}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.ContratDto> contratsDe(@PathVariable Long idEmploye) {
        return contrats.contratsDe(idEmploye);
    }

    @GetMapping("/employe/{idEmploye}/courant")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ResponseEntity<ContratDtos.ContratDto> courant(@PathVariable Long idEmploye) {
        ContratDtos.ContratDto courant = contrats.courant(idEmploye);
        return courant == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(courant);
    }

    @GetMapping("/employe/{idEmploye}/affectations")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.AffectationDto> affectationsDe(@PathVariable Long idEmploye) {
        return contrats.affectationsDe(idEmploye);
    }

    @GetMapping("/employe/{idEmploye}/onboarding")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.OnboardingDto onboardingDe(@PathVariable Long idEmploye) {
        return contrats.onboardingDeOuNull(idEmploye);
    }

    @GetMapping("/onboarding")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.OnboardingDto> onboardings() {
        return onboarding.onboardings();
    }

    @GetMapping("/onboarding/{id}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.OnboardingDto onboarding(@PathVariable Long id) {
        return onboarding.onboarding(id);
    }

    @GetMapping("/{id}/onboarding")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.OnboardingDto onboardingDuContrat(@PathVariable Long id) {
        return onboarding.onboardingDuContrat(id);
    }

    @GetMapping("/{id}/avenants")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.AvenantDto> avenantsDe(@PathVariable Long id) {
        return avenants.avenantsDe(id);
    }

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    /** Signature de l'employeur (RH/Direction). Conserve pour compatibilite. */
    @PostMapping("/{id}/signer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto signer(
            @PathVariable Long id,
            @RequestBody(required = false) ContratDtos.SignatureEmployeurRequest demande,
            HttpServletRequest request) {
        return contrats.signerEmployeur(id, utilisateurCourant.exiger(), demande,
                request.getRemoteAddr());
    }

    /** Signature de l'employeur explicite (RG-B4-05). */
    @PostMapping("/{id}/signer-employeur")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto signerEmployeur(
            @PathVariable Long id,
            @RequestBody(required = false) ContratDtos.SignatureEmployeurRequest demande,
            HttpServletRequest request) {
        return contrats.signerEmployeur(id, utilisateurCourant.exiger(), demande,
                request.getRemoteAddr());
    }

    /** Decision tracee de fin de periode d'essai (RG-B4-14). */
    @PostMapping("/{id}/decision-fin-essai")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto deciderFinEssai(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.DecisionFinEssaiRequest demande) {
        return contrats.deciderFinEssai(id, demande, utilisateurCourant.exiger());
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto valider(@PathVariable Long id) {
        return contrats.valider(id);
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto annuler(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.AnnulationRequest demande) {
        return contrats.annuler(id, demande, utilisateurCourant.exiger());
    }

    // ------------------------------------------------------------------
    // Notifications internes
    // ------------------------------------------------------------------

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public List<ContratDtos.NotificationDto> notifications(
            @RequestParam(required = false) String statut) {
        return notifications.lister(statut);
    }

    @PatchMapping("/notifications/{id}/lire")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.NotificationDto marquerNotificationLue(@PathVariable Long id) {
        return notifications.marquerLue(id);
    }

    @PostMapping("/{id}/renouveler")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto renouveler(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.RenouvellementRequest demande) {
        ContratDtos.RenouvellementRequest complete = new ContratDtos.RenouvellementRequest(
                demande.dateFinPrevue(), demande.salaireBase(), demande.primesAvantages(),
                demande.horaires(), demande.lieuTravail(), demande.motifCdd(),
                demande.signataireEmployeurId() == null
                        ? utilisateurCourant.exiger().getId()
                        : demande.signataireEmployeurId());
        return contrats.renouveler(id, complete);
    }

    @PostMapping("/{id}/transformer-cdi")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ContratDto transformerEnCdi(
            @PathVariable Long id,
            @RequestBody(required = false) ContratDtos.TransformationCdiRequest demande) {
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
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.AvenantDto creerAvenant(
            @PathVariable Long id,
            @Valid @RequestBody ContratDtos.AvenantRequest demande) {
        return avenants.creer(id, demande);
    }

    @PostMapping("/avenants/{idAvenant}/signer")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.AvenantDto signerAvenant(@PathVariable Long idAvenant) {
        return avenants.signer(idAvenant, utilisateurCourant.exiger().getId());
    }

    @PostMapping("/avenants/{idAvenant}/valider")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.AvenantDto validerAvenant(@PathVariable Long idAvenant) {
        return avenants.valider(idAvenant);
    }

    @PostMapping("/employe/{idEmploye}/affectations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.AffectationDto ajouterAffectation(
            @PathVariable Long idEmploye,
            @Valid @RequestBody ContratDtos.AffectationRequest demande) {
        return contrats.ajouterAffectation(idEmploye, demande);
    }

    @PatchMapping("/taches/{idTache}")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION', 'MANAGER')")
    public ContratDtos.TacheDto mettreAJourTache(
            @PathVariable Long idTache,
            @Valid @RequestBody ContratDtos.TacheMiseAJourRequest demande) {
        return onboarding.mettreAJourTache(idTache, demande);
    }

    @PostMapping("/echeances/traiter")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ContratDtos.ResultatEcheances traiterEcheances() {
        return contrats.traiterEcheances();
    }

    // ------------------------------------------------------------------
    // Document
    // ------------------------------------------------------------------

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('RH', 'DIRECTION')")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
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
