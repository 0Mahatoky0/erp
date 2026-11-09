package com.tovo.erp.employe.controller;

import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.DocumentEmbaucheFichier;
import com.tovo.erp.employe.dto.DocumentEmbaucheRequest;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Dossier salarie cree par l'acceptation d'une proposition d'embauche. */
@RestController
@RequestMapping("/api/employes")
public class EmployeController {

    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;

    public EmployeController(EmployeService employes, UtilisateurCourant utilisateurCourant) {
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/{id}/dossier")
    public EmployeEmbaucheDto dossier(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        return employes.dossier(id);
    }

    @GetMapping("/{id}/documents")
    public List<DocumentEmbaucheDto> documents(@PathVariable Long id) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        return employes.documents(id);
    }

    @PostMapping("/{id}/documents")
    public DocumentEmbaucheDto ajouterDocument(@PathVariable Long id,
                                               @Valid @RequestBody DocumentEmbaucheRequest demande) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return employes.enregistrerDocument(id, demande);
    }

    @GetMapping("/documents/{idDocument}")
    public ResponseEntity<byte[]> document(@PathVariable Long idDocument) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION, Roles.MANAGER);
        DocumentEmbaucheFichier fichier = employes.contenuDocument(idDocument);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fichier.nomFichier() == null ? "document" : fichier.nomFichier(),
                                StandardCharsets.UTF_8)
                        .build().toString())
                .body(fichier.contenu());
    }
}
