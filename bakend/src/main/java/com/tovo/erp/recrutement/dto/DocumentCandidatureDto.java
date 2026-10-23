package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.domain.DocumentCandidature;
import com.tovo.erp.recrutement.domain.TypeDocument;
import java.time.LocalDateTime;

public record DocumentCandidatureDto(Long idDocument, Long idCandidature, TypeDocument typeDocument,
                                     String nomFichier, String typeMime, Long tailleOctets,
                                     LocalDateTime dateDepot) {
    public static DocumentCandidatureDto de(DocumentCandidature d) {
        return new DocumentCandidatureDto(d.getIdDocument(), d.getCandidature().getIdCandidature(),
                d.getTypeDocument(), d.getNomFichier(), d.getTypeMime(), d.getTailleOctets(), d.getDateDepot());
    }
}
