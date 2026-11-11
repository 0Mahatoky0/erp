package com.tovo.erp.paie.dto;

import com.tovo.erp.paie.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BulletinPaieDtos {
    private BulletinPaieDtos() {}

    public record ParametresRequest(@NotNull @DecimalMin("0.0") BigDecimal tauxCotisationsSalariales,
                                    @NotNull @DecimalMin("0.0") BigDecimal tauxCotisationsPatronales,
                                    @NotNull @DecimalMin("0.0") BigDecimal tauxIrsa) {}

    public record RetenueRequest(@NotNull @DecimalMin("0.0") BigDecimal montant) {}

    public record ParametresDto(Long id, BigDecimal tauxCotisationsSalariales,
                                BigDecimal tauxCotisationsPatronales, BigDecimal tauxIrsa,
                                boolean actif) {
        public static ParametresDto de(ParametrePaie p) {
            return new ParametresDto(p.getId(), p.getTauxCotisationsSalariales(),
                    p.getTauxCotisationsPatronales(), p.getTauxIrsa(), p.isActif());
        }
    }

    public record PeriodeDto(Long id, LocalDate mois, StatutPeriodePaie statut,
                             LocalDateTime dateCreation, LocalDateTime dateCloture) {
        public static PeriodeDto de(PeriodePaie p) {
            return new PeriodeDto(p.getId(), p.getMois(), p.getStatut(),
                    p.getDateCreation(), p.getDateCloture());
        }
    }

    public record BulletinDto(Long id, Long idPeriode, Long idEmploye,
                              BigDecimal salaireBrut, BigDecimal cotisationsSalariales,
                              BigDecimal impot, BigDecimal retenues, BigDecimal salaireNet,
                              BigDecimal cotisationsPatronales, StatutBulletinPaie statut) {
        public static BulletinDto de(BulletinPaie b) {
            return new BulletinDto(b.getId(), b.getIdPeriode(), b.getIdEmploye(),
                    b.getSalaireBrut(), b.getCotisationsSalariales(), b.getImpot(),
                    b.getRetenues(), b.getSalaireNet(), b.getCotisationsPatronales(), b.getStatut());
        }
    }

    public record PeriodeAvecBulletinsDto(PeriodeDto periode, List<BulletinDto> bulletins) {}
}
