package com.tovo.erp.contrat.controller;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Consultation des contrats, affectations et parcours d'integration (bloc 4). */
@RestController
@RequestMapping("/api/contrats")
public class ContratController {

    private final ContratService contrats;
    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;

    public ContratController(ContratService contrats, EmployeService employes,
                             UtilisateurCourant utilisateurCourant) {
        this.contrats = contrats;
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
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

    @GetMapping("/employe/{idEmploye}/affectations")
    public List<ContratDtos.AffectationDto> affectationsDe(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.affectationsDe(idEmploye);
    }

    @GetMapping("/employe/{idEmploye}/onboarding")
    public ContratDtos.OnboardingDto onboardingDe(@PathVariable Long idEmploye) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION, RoleNom.MANAGER);
        return contrats.onboardingDe(idEmploye);
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        ContratDtos.ContratDto contrat = contrats.contrat(id);
        if (contrat.documentPdfId() == null) {
            return ResponseEntity.notFound().build();
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
