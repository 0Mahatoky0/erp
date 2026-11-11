package com.tovo.erp.paie.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.model.*;
import com.tovo.erp.paie.repository.*;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BulletinPaieService {
    private final ParametrePaieRepository parametres;
    private final PeriodePaieRepository periodes;
    private final BulletinPaieRepository bulletins;
    private final EmployeRepository employes;
    private final ContratService contrats;

    public BulletinPaieService(ParametrePaieRepository parametres, PeriodePaieRepository periodes,
                               BulletinPaieRepository bulletins, EmployeRepository employes,
                               ContratService contrats) {
        this.parametres = parametres;
        this.periodes = periodes;
        this.bulletins = bulletins;
        this.employes = employes;
        this.contrats = contrats;
    }

    @Transactional(readOnly = true)
    public BulletinPaieDtos.ParametresDto parametres() {
        return parametres.findFirstByActifTrueOrderByDateModificationDesc()
                .map(BulletinPaieDtos.ParametresDto::de)
                .orElse(null);
    }

    @Transactional
    public BulletinPaieDtos.ParametresDto enregistrerParametres(BulletinPaieDtos.ParametresRequest request) {
        ParametrePaie precedent = parametres.findFirstByActifTrueOrderByDateModificationDesc().orElse(null);
        if (precedent != null) {
            precedent.setActif(false);
            parametres.save(precedent);
        }
        ParametrePaie parametre = new ParametrePaie();
        parametre.setTauxCotisationsSalariales(request.tauxCotisationsSalariales());
        parametre.setTauxCotisationsPatronales(request.tauxCotisationsPatronales());
        parametre.setTauxIrsa(request.tauxIrsa());
        return BulletinPaieDtos.ParametresDto.de(parametres.save(parametre));
    }

    @Transactional
    public BulletinPaieDtos.PeriodeAvecBulletinsDto calculer(LocalDate mois) {
        ParametrePaie parametre = parametres.findFirstByActifTrueOrderByDateModificationDesc()
                .orElseThrow(() -> new BusinessException("PARAMETRES_PAIE_MANQUANTS",
                        "Configurez les taux de paie avant de calculer une période."));
        LocalDate moisNormalise = mois.withDayOfMonth(1);
        PeriodePaie periode = periodes.findByMois(moisNormalise).orElseGet(() -> {
            PeriodePaie nouvelle = new PeriodePaie();
            nouvelle.setMois(moisNormalise);
            return periodes.save(nouvelle);
        });
        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE) {
            throw new BusinessException("PERIODE_CLOTUREE", "Une période clôturée ne peut plus être recalculée.");
        }
        employes.findByStatutOrderByNomAscPrenomAsc(com.tovo.erp.employe.model.StatutEmploye.ACTIF)
                .forEach(employe -> {
                    ContratDtos.ContratDto contrat = contrats.courant(employe.getId());
                    if (contrat == null || contrat.salaireBase() == null) return;
                    BigDecimal brut = contrat.salaireBase();
                    BigDecimal cotisationsSalariales = taux(brut, parametre.getTauxCotisationsSalariales());
                    BigDecimal impot = taux(brut.subtract(cotisationsSalariales).max(BigDecimal.ZERO),
                            parametre.getTauxIrsa());
                    BigDecimal cotisationsPatronales = taux(brut, parametre.getTauxCotisationsPatronales());
                    BulletinPaie bulletin = bulletins.findByIdPeriodeAndIdEmploye(periode.getId(), employe.getId())
                            .orElseGet(BulletinPaie::new);
                    bulletin.setIdPeriode(periode.getId());
                    bulletin.setIdEmploye(employe.getId());
                    bulletin.setSalaireBrut(brut);
                    bulletin.setCotisationsSalariales(cotisationsSalariales);
                    bulletin.setImpot(impot);
                    bulletin.setRetenues(BigDecimal.ZERO);
                    bulletin.setSalaireNet(brut.subtract(cotisationsSalariales).subtract(impot));
                    bulletin.setCotisationsPatronales(cotisationsPatronales);
                    bulletins.save(bulletin);
                });
        periode.setStatut(StatutPeriodePaie.CALCULEE);
        periodes.save(periode);
        return lire(periode.getId());
    }

    @Transactional(readOnly = true)
    public BulletinPaieDtos.PeriodeAvecBulletinsDto lire(Long idPeriode) {
        PeriodePaie periode = periodes.findById(idPeriode)
                .orElseThrow(() -> BusinessException.notFound("Période de paie introuvable."));
        return new BulletinPaieDtos.PeriodeAvecBulletinsDto(
                BulletinPaieDtos.PeriodeDto.de(periode),
                bulletins.findByIdPeriodeOrderByIdEmployeAsc(idPeriode).stream()
                        .map(BulletinPaieDtos.BulletinDto::de).toList());
    }

    @Transactional(readOnly = true)
    public List<BulletinPaieDtos.PeriodeDto> periodes() {
        return periodes.findAllByOrderByMoisDesc().stream()
                .map(BulletinPaieDtos.PeriodeDto::de)
                .toList();
    }

    @Transactional
    public BulletinPaieDtos.BulletinDto enregistrerRetenue(
            Long idBulletin, BulletinPaieDtos.RetenueRequest request) {
        BulletinPaie bulletin = bulletins.findById(idBulletin)
                .orElseThrow(() -> BusinessException.notFound("Bulletin de paie introuvable."));
        PeriodePaie periode = periodes.findById(bulletin.getIdPeriode())
                .orElseThrow(() -> BusinessException.notFound("Période de paie introuvable."));
        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE
                || bulletin.getStatut() == StatutBulletinPaie.VALIDE) {
            throw new BusinessException("BULLETIN_NON_MODIFIABLE",
                    "Un bulletin validé ou une période clôturée ne peut plus être modifié.");
        }
        bulletin.setRetenues(request.montant());
        bulletin.setSalaireNet(bulletin.getSalaireBrut()
                .subtract(bulletin.getCotisationsSalariales())
                .subtract(bulletin.getImpot())
                .subtract(request.montant()));
        return BulletinPaieDtos.BulletinDto.de(bulletins.save(bulletin));
    }

    @Transactional
    public BulletinPaieDtos.PeriodeDto cloturer(Long idPeriode) {
        PeriodePaie periode = periodes.findById(idPeriode)
                .orElseThrow(() -> BusinessException.notFound("Période de paie introuvable."));
        if (periode.getStatut() != StatutPeriodePaie.CALCULEE) {
            throw new BusinessException("PERIODE_NON_CALCULEE", "Calculez la période avant de la clôturer.");
        }
        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        periode.setDateCloture(LocalDateTime.now());
        return BulletinPaieDtos.PeriodeDto.de(periodes.save(periode));
    }

    private BigDecimal taux(BigDecimal base, BigDecimal pourcentage) {
        return base.multiply(pourcentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
