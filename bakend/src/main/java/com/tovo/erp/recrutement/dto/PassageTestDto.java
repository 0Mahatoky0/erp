package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.*;
import java.time.LocalDateTime;

public record PassageTestDto(Long idPassage, Long idTest, Long idCandidature, Integer numeroTentative,
                             LocalDateTime datePrevue, LocalDateTime dateDebut, LocalDateTime dateFin,
                             TestStatut statut, String commentaire) {
    public static PassageTestDto de(PassageTest p) {
        return new PassageTestDto(p.getIdPassage(), p.getTest().getIdTest(),
                p.getCandidature().getIdCandidature(), p.getNumeroTentative(), p.getDatePrevue(),
                p.getDateDebut(), p.getDateFin(), p.getStatut(), p.getCommentaire());
    }
}
