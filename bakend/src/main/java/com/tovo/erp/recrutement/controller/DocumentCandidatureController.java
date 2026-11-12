package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.DocumentCandidatureDto;
import com.tovo.erp.recrutement.service.DocumentCandidatureService;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidatures/{id}/documents")
public class DocumentCandidatureController {

    private final DocumentCandidatureService documents;
    private final UtilisateurCourant utilisateurCourant;

    public DocumentCandidatureController(DocumentCandidatureService documents,
                                         UtilisateurCourant utilisateurCourant) {
        this.documents = documents;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentCandidatureDto> deposer(@PathVariable Long id, @RequestParam String type,
                                                          @RequestPart MultipartFile fichier) throws IOException {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documents.deposer(id, type, fichier));
    }

    @GetMapping
    public List<DocumentCandidatureDto> lister(@PathVariable Long id) {
        return documents.listerDto(id);
    }
}
