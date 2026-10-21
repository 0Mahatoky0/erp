package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentCandidatureService {
    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private final DocumentCandidatureRepository documents;
    private final CandidatureRepository candidatures;

    public DocumentCandidatureService(DocumentCandidatureRepository documents, CandidatureRepository candidatures) {
        this.documents = documents; this.candidatures = candidatures;
    }

    @Transactional
    public DocumentCandidature deposer(Long idCandidature, TypeDocument type, MultipartFile fichier) throws IOException {
        Candidature candidature = candidatures.findById(idCandidature)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
        if (fichier.isEmpty() || fichier.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("Le fichier est vide ou dépasse 10 Mo");
        if (fichier.getContentType() == null || !fichier.getContentType().matches(
                "application/pdf|application/msword|application/vnd.openxmlformats-officedocument.wordprocessingml.document|image/(png|jpeg)"))
            throw new IllegalArgumentException("Type MIME non autorisé");
        DocumentCandidature document = new DocumentCandidature();
        document.setCandidature(candidature);
        document.setTypeDocument(type);
        document.setNomFichier(fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename());
        document.setTypeMime(fichier.getContentType());
        document.setTailleOctets(fichier.getSize());
        document.setCleStockage("candidatures/" + idCandidature + "/" + UUID.randomUUID());
        return documents.save(document);
    }

    public List<DocumentCandidature> lister(Long idCandidature) {
        if (!candidatures.existsById(idCandidature)) throw new IllegalArgumentException("Candidature introuvable");
        return documents.findByCandidatureIdCandidature(idCandidature);
    }
}
