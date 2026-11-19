package com.tovo.erp.rupture.dto;

import com.tovo.erp.rupture.model.CategorieLigneSolde;
import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.DocumentRupture;
import com.tovo.erp.rupture.model.DroitsRupture;
import com.tovo.erp.rupture.model.EntretienRupture;
import com.tovo.erp.rupture.model.InitiateurRupture;
import com.tovo.erp.rupture.model.LigneSoldeRupture;
import com.tovo.erp.rupture.model.MaterielRupture;
import com.tovo.erp.rupture.model.ModePreavis;
import com.tovo.erp.rupture.model.MotifRupture;
import com.tovo.erp.rupture.model.SoldeToutCompte;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import com.tovo.erp.rupture.model.StatutMaterielRupture;
import com.tovo.erp.rupture.model.StatutSoldeRupture;
import com.tovo.erp.rupture.model.StatutTacheRupture;
import com.tovo.erp.rupture.model.TacheDepart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RuptureDtos {

    private RuptureDtos() {
    }

    public record CreationDossier(
            Long idContrat,
            MotifRupture motif,
            InitiateurRupture initiateur,
            LocalDate dateNotification,
            LocalDate dateSortieSouhaitee,
            Integer preavisJours,
            ModePreavis modePreavis,
            String motifAutre,
            String description,
            Long idProcedureDisciplinaire) {
    }

    public record ModificationDateSortie(
            LocalDate dateSortie,
            boolean avenantSigne,
            String referencePiece) {
    }

    public record DecisionSolde(String motifRefus) {
    }

    public record PaiementSolde(String referencePaiement, String modePaiement) {
    }

    public record RemiseDocument(String modeRemise, String referencePreuve) {
    }

    public record TacheRequest(StatutTacheRupture statut, String commentaire) {
    }

    public record EntretienRequest(
            LocalDate dateEntretien,
            Long animateurId,
            String questionnaireJson,
            Integer satisfaction,
            String commentaireConfidentiel) {
    }

    public record MaterielRequest(
            String libelle,
            String type,
            String numeroSerie,
            BigDecimal montantRetenue) {
    }

    public record DossierDto(
            Long id,
            String reference,
            Long idContrat,
            Long idEmploye,
            String nomEmploye,
            String numeroContrat,
            MotifRupture motif,
            InitiateurRupture initiateur,
            LocalDate dateNotification,
            LocalDate datePreavisDebut,
            LocalDate datePreavisFin,
            LocalDate dateSortie,
            Integer preavisJours,
            ModePreavis modePreavis,
            StatutDossierRupture statut,
            String motifAutre,
            String description,
            Long idProcedureDisciplinaire,
            LocalDate dateArchivage,
            LocalDateTime dateCreation,
            BigDecimal netIndicatif) {

        public static DossierDto de(DossierRupture dossier) {
            return de(dossier, null, null, null);
        }

        public static DossierDto de(DossierRupture dossier, String nomEmploye, String numeroContrat) {
            return de(dossier, nomEmploye, numeroContrat, null);
        }

        public static DossierDto de(DossierRupture dossier, String nomEmploye, String numeroContrat,
                                   BigDecimal netIndicatif) {
            return new DossierDto(
                    dossier.getId(),
                    dossier.getReference(),
                    dossier.getIdContrat(),
                    dossier.getIdEmploye(),
                    nomEmploye,
                    numeroContrat,
                    dossier.getMotif(),
                    dossier.getInitiateur(),
                    dossier.getDateNotification(),
                    dossier.getDatePreavisDebut(),
                    dossier.getDatePreavisFin(),
                    dossier.getDateSortie(),
                    dossier.getPreavisJours(),
                    dossier.getPreavisMode(),
                    dossier.getStatut(),
                    dossier.getMotifAutre(),
                    dossier.getDescription(),
                    dossier.getIdProcedureDisciplinaire(),
                    dossier.getDateArchivage(),
                    dossier.getDateCreation(),
                    netIndicatif);
        }
    }

    public record DroitsDto(
            Long id,
            Long idDossier,
            BigDecimal congeSolde,
            String congeDetails,
            int congesFutursAnnules,
            BigDecimal primesDues,
            BigDecimal avancesRestantes,
            BigDecimal deditFormation,
            String details,
            LocalDateTime dateArret) {

        public static DroitsDto de(DroitsRupture droits) {
            return new DroitsDto(
                    droits.getId(),
                    droits.getIdDossier(),
                    droits.getCongeSolde(),
                    droits.getCongeDetails(),
                    droits.getCongesFutursAnnules(),
                    droits.getPrimesDues(),
                    droits.getAvancesRestantes(),
                    droits.getDeditFormation(),
                    droits.getDetails(),
                    droits.getDateArret());
        }
    }

    public record SoldeDto(
            Long id,
            Long idDossier,
            BigDecimal salaireDu,
            BigDecimal indemnitePreavis,
            BigDecimal indemniteLicenciement,
            BigDecimal indemniteConges,
            BigDecimal primes,
            BigDecimal retenuesAvances,
            BigDecimal retenuesDedit,
            BigDecimal retenuesMateriel,
            BigDecimal totalBrut,
            BigDecimal totalRetenues,
            BigDecimal netAvantCotisations,
            BigDecimal montantPaye,
            String devise,
            StatutSoldeRupture statut,
            LocalDateTime datePreparation,
            Long idCalculateur,
            LocalDateTime dateValidation,
            Long idValidateur,
            LocalDate datePaiement,
            String referencePaiement,
            String modePaiement,
            String motifRefus,
            Long idBulletinPaie,
            List<LigneDto> lignes) {

        public static SoldeDto de(SoldeToutCompte solde, List<LigneDto> lignes) {
            return new SoldeDto(
                    solde.getId(),
                    solde.getIdDossier(),
                    solde.getSalaireDu(),
                    solde.getIndemnitePreavis(),
                    solde.getIndemniteLicenciement(),
                    solde.getIndemniteConges(),
                    solde.getPrimes(),
                    solde.getRetenuesAvances(),
                    solde.getRetenuesDedit(),
                    solde.getRetenuesMateriel(),
                    solde.getTotalBrut(),
                    solde.getTotalRetenues(),
                    solde.getNetAvantCotisations(),
                    solde.getMontantPaye(),
                    solde.getDevise(),
                    solde.getStatut(),
                    solde.getDatePreparation(),
                    solde.getIdCalculateur(),
                    solde.getDateValidation(),
                    solde.getIdValidateur(),
                    solde.getDatePaiement(),
                    solde.getReferencePaiement(),
                    solde.getModePaiement(),
                    solde.getMotifRefus(),
                    solde.getIdBulletinPaie(),
                    lignes);
        }
    }

    public record LigneDto(
            Long id,
            String code,
            String libelle,
            CategorieLigneSolde categorie,
            BigDecimal base,
            BigDecimal quantite,
            BigDecimal taux,
            BigDecimal montant,
            String source) {

        public static LigneDto de(LigneSoldeRupture ligne) {
            return new LigneDto(
                    ligne.getId(),
                    ligne.getCode(),
                    ligne.getLibelle(),
                    ligne.getCategorie(),
                    ligne.getBase(),
                    ligne.getQuantite(),
                    ligne.getTaux(),
                    ligne.getMontant(),
                    ligne.getSource());
        }
    }

    public record TacheDto(
            Long id,
            String code,
            String libelle,
            String categorie,
            String responsable,
            StatutTacheRupture statut,
            LocalDate dateEcheance,
            LocalDateTime dateCloture,
            Long idResponsable,
            boolean bloquante,
            String commentaire) {

        public static TacheDto de(TacheDepart tache) {
            return new TacheDto(
                    tache.getId(),
                    tache.getCode(),
                    tache.getLibelle(),
                    tache.getCategorie(),
                    tache.getResponsable(),
                    tache.getStatut(),
                    tache.getDateEcheance(),
                    tache.getDateCloture(),
                    tache.getIdResponsable(),
                    tache.isBloquante(),
                    tache.getCommentaire());
        }
    }

    public record MaterielDto(
            Long id,
            String libelle,
            String type,
            String numeroSerie,
            StatutMaterielRupture statut,
            LocalDate dateRestitution,
            BigDecimal montantRetenue) {

        public static MaterielDto de(MaterielRupture materiel) {
            return new MaterielDto(
                    materiel.getId(),
                    materiel.getLibelle(),
                    materiel.getType(),
                    materiel.getNumeroSerie(),
                    materiel.getStatut(),
                    materiel.getDateRestitution(),
                    materiel.getMontantRetenue());
        }
    }

    public record DocumentDto(
            Long id,
            String code,
            String libelle,
            String format,
            String hashSha256,
            Long idDocumentEmploye,
            LocalDateTime dateGeneration,
            LocalDate dateRemise,
            String modeRemise,
            String referencePreuve) {

        public static DocumentDto de(DocumentRupture document) {
            return new DocumentDto(
                    document.getId(),
                    document.getCode(),
                    document.getLibelle(),
                    document.getFormat(),
                    document.getHashSha256(),
                    document.getIdDocumentEmploye(),
                    document.getDateGeneration(),
                    document.getDateRemise(),
                    document.getModeRemise(),
                    document.getReferencePreuve());
        }
    }

    public record EntretienDto(
            Long id,
            Long idDossier,
            LocalDate dateEntretien,
            Long animateurId,
            Integer satisfaction,
            LocalDateTime dateCreation) {

        public static EntretienDto de(EntretienRupture entretien) {
            return new EntretienDto(
                    entretien.getId(),
                    entretien.getIdDossier(),
                    entretien.getDateEntretien(),
                    entretien.getAnimateurId(),
                    entretien.getSatisfaction(),
                    entretien.getDateCreation());
        }
    }

    public record IndicateursDto(
            long departs,
            BigDecimal effectifMoyen,
            BigDecimal tauxRotation,
            BigDecimal ancienneteMoyenneMois) {
    }

    public record TypeDto(String code, String libelle) {
    }
}
