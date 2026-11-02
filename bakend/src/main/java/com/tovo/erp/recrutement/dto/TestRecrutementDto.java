package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.TestRecrutement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TestRecrutementDto(Long idTest, Long idOffre, String titre, String description,
                                 Integer dureeMinutes, BigDecimal noteMax, LocalDateTime dateCreation) {
    public static TestRecrutementDto de(TestRecrutement t) {
        return new TestRecrutementDto(t.getIdTest(), t.getOffre().getIdOffre(), t.getTitre(),
                t.getDescription(), t.getDureeMinutes(), t.getNoteMax(), t.getDateCreation());
    }
}
