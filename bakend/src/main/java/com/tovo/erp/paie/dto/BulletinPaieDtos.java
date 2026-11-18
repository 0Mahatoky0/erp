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
                                    @NotNull @DecimalMin("0.0") BigDecimal tauxIrsa,
                                    @DecimalMin("0.0") BigDecimal smig,
                                    @DecimalMin("0.0") BigDecimal plafondCnapsSmig,
                                    @DecimalMin("0.0") BigDecimal tauxCnapsSalarial,
                                    @DecimalMin("0.0") BigDecimal tauxCnapsPatronal,
                                    @DecimalMin("0.0") BigDecimal tauxOstieSalarial,
                                    @DecimalMin("0.0") BigDecimal tauxOstiePatronal,
                                    @DecimalMin("0.0") BigDecimal tauxFmfpPatronal,
                                    @DecimalMin("0.0") BigDecimal majorationHs30,
                                    @DecimalMin("0.0") BigDecimal majorationHs50,
                                    @DecimalMin("0.0") BigDecimal majorationNuit,
                                    @DecimalMin("0.0") BigDecimal majorationDimanche,
                                    @DecimalMin("0.0") BigDecimal majorationFerie,
                                    @DecimalMin("0.0") BigDecimal minimumIrsa,
                                    @DecimalMin("0.0") BigDecimal reductionCharge,
                                    @DecimalMin("0.0") BigDecimal quotientRetenue,
                                    LocalDate dateEffet) {}

    public record RetenueRequest(@NotNull @DecimalMin("0.0") BigDecimal montant) {}

    public record AjustementRequest(@DecimalMin("0.0") BigDecimal primeExceptionnelle,
                                    @DecimalMin("0.0") BigDecimal avance,
                                    @Min(0) Integer personnesCharge,
                                    @NotBlank String motif) {}

    public record ParametresDto(Long id, BigDecimal tauxCotisationsSalariales,
                                BigDecimal tauxCotisationsPatronales, BigDecimal tauxIrsa,
                                boolean actif, LocalDate dateEffet, BigDecimal smig,
                                BigDecimal plafondCnapsSmig, BigDecimal tauxCnapsSalarial,
                                BigDecimal tauxCnapsPatronal, BigDecimal tauxOstieSalarial,
                                BigDecimal tauxOstiePatronal, BigDecimal tauxFmfpPatronal,
                                BigDecimal majorationHs30, BigDecimal majorationHs50,
                                BigDecimal majorationNuit, BigDecimal majorationDimanche,
                                BigDecimal majorationFerie, BigDecimal minimumIrsa,
                                BigDecimal reductionCharge, BigDecimal quotientRetenue) {
        public static ParametresDto de(ParametrePaie p) {
            return new ParametresDto(p.getId(), p.getTauxCotisationsSalariales(),
                    p.getTauxCotisationsPatronales(), p.getTauxIrsa(), p.isActif(), p.getDateEffet(),
                    p.getSmig(), p.getPlafondCnapsSmig(), p.getTauxCnapsSalarial(),
                    p.getTauxCnapsPatronal(), p.getTauxOstieSalarial(), p.getTauxOstiePatronal(),
                    p.getTauxFmfpPatronal(), p.getMajorationHs30(), p.getMajorationHs50(),
                    p.getMajorationNuit(), p.getMajorationDimanche(), p.getMajorationFerie(),
                    p.getMinimumIrsa(), p.getReductionCharge(), p.getQuotientRetenue());
        }
    }

    public record PeriodeDto(Long id, LocalDate mois, StatutPeriodePaie statut,
                             LocalDateTime dateCreation, LocalDateTime dateCloture,
                             LocalDate dateClotureVariables, LocalDate datePaiement,
                             String versionRegles) {
        public static PeriodeDto de(PeriodePaie p) {
            return new PeriodeDto(p.getId(), p.getMois(), p.getStatut(), p.getDateCreation(),
                    p.getDateCloture(), p.getDateClotureVariables(), p.getDatePaiement(),
                    p.getVersionRegles());
        }
    }

    public record BulletinDto(Long id, Long idPeriode, Long idEmploye,
                              BigDecimal salaireBrut, BigDecimal cotisationsSalariales,
                              BigDecimal impot, BigDecimal retenues, BigDecimal salaireNet,
                              BigDecimal cotisationsPatronales, StatutBulletinPaie statut,
                              int jourPayes, BigDecimal salaireBasePeriode, BigDecimal baseImposable,
                              BigDecimal primeExceptionnelle, BigDecimal avance, int personnesCharge,
                              String motifAjustement, String numero, String type) {
        public static BulletinDto de(BulletinPaie b) {
            return new BulletinDto(b.getId(), b.getIdPeriode(), b.getIdEmploye(), b.getSalaireBrut(),
                    b.getCotisationsSalariales(), b.getImpot(), b.getRetenues(), b.getSalaireNet(),
                    b.getCotisationsPatronales(), b.getStatut(), b.getJourPayes(),
                    b.getSalaireBasePeriode(), b.getBaseImposable(), b.getPrimeExceptionnelle(),
                    b.getAvance(), b.getPersonnesCharge(), b.getMotifAjustement(), b.getNumero(),
                    b.getType());
        }
    }

    public record LigneDto(Long id, String codeRubrique, String libelle, int ordre, BigDecimal base,
                           BigDecimal taux, BigDecimal quantite, BigDecimal montantSalarial,
                           BigDecimal montantPatronal, String formuleAppliquee,
                           String versionRegle, String sources) {
        public static LigneDto de(LigneBulletin ligne) {
            return new LigneDto(ligne.getId(), ligne.getCodeRubrique(), ligne.getLibelle(),
                    ligne.getOrdre(), ligne.getBase(), ligne.getTaux(), ligne.getQuantite(),
                    ligne.getMontantSalarial(), ligne.getMontantPatronal(),
                    ligne.getFormuleAppliquee(), ligne.getVersionRegle(), ligne.getSources());
        }
    }

    public record BulletinDetailDto(BulletinDto bulletin, List<LigneDto> lignes) {}

    public record FicheDto(Long id, Long idBulletin, Long idPeriode, Long idEmploye, LocalDate mois,
                           String numero, BigDecimal netAPayer, LocalDateTime dateGeneration,
                           String contenu) {
        public static FicheDto de(FichePaie fiche) {
            return new FicheDto(fiche.getId(), fiche.getIdBulletin(), fiche.getIdPeriode(),
                    fiche.getIdEmploye(), fiche.getMois(), fiche.getNumero(), fiche.getNetAPayer(),
                    fiche.getDateGeneration(), fiche.getContenuJson());
        }
    }

    public record PeriodeAvecBulletinsDto(PeriodeDto periode, List<BulletinDto> bulletins) {}
}
