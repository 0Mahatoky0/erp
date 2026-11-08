package com.tovo.erp.besoin.dto;

import com.tovo.erp.besoin.model.Poste;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Objets de transfert du catalogue des postes officiels (RM-006 a RM-011). */
public final class PosteDto {

    private PosteDto() {
    }

    /** Poste du catalogue, avec le nom de son service. */
    public record Liste(Long id, Long idService, String service,
                        Long idDepartement, String departement,
                        String intitule, String description,
                        BigDecimal salaireMin, BigDecimal salaireMax,
                        boolean actif, long nbPostesLies) {

        public static Liste de(Poste poste, long nbPostesLies) {
            return new Liste(
                    poste.getId(),
                    poste.getService().getId(),
                    poste.getService().getNom(),
                    poste.getService().getDepartement().getId(),
                    poste.getService().getDepartement().getNom(),
                    poste.getIntitule(),
                    poste.getDescription(),
                    poste.getSalaireMin(),
                    poste.getSalaireMax(),
                    poste.isActif(),
                    nbPostesLies);
        }
    }

    /** Creation d'un poste officiel (reserve au role RH). */
    public record Creation(@NotNull Long idService,
                            @NotBlank String intitule,
                            String description,
                            @PositiveOrZero BigDecimal salaireMin,
                            @PositiveOrZero BigDecimal salaireMax) {
    }

    /** Modification d'un poste officiel : un poste est desactive, jamais supprime (RM-011). */
    public record Modification(@NotBlank String intitule,
                                String description,
                                @PositiveOrZero BigDecimal salaireMin,
                                @PositiveOrZero BigDecimal salaireMax,
                                boolean actif) {
    }
}
