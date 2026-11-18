package com.tovo.erp.paie.service;

import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.paie.model.BaremeIrsa;
import com.tovo.erp.paie.model.BulletinPaie;
import com.tovo.erp.paie.model.LigneBulletin;
import com.tovo.erp.paie.model.ParametrePaie;
import com.tovo.erp.paie.model.StatutBulletinPaie;
import com.tovo.erp.paie.repository.BaremeIrsaRepository;
import com.tovo.erp.paie.repository.BulletinPaieRepository;
import com.tovo.erp.paie.repository.FichePaieRepository;
import com.tovo.erp.paie.repository.LigneBulletinRepository;
import com.tovo.erp.paie.repository.ParametrePaieRepository;
import com.tovo.erp.paie.repository.PeriodePaieRepository;
import com.tovo.erp.paie.repository.RubriquePaieRepository;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.service.TempsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulletinPaieServiceTest {

    @Mock ParametrePaieRepository parametres;
    @Mock PeriodePaieRepository periodes;
    @Mock BulletinPaieRepository bulletins;
    @Mock LigneBulletinRepository lignes;
    @Mock FichePaieRepository fiches;
    @Mock BaremeIrsaRepository baremes;
    @Mock RubriquePaieRepository rubriques;
    @Mock EmployeService employes;
    @Mock ContratService contrats;
    @Mock CarriereService carriere;
    @Mock TempsService temps;
    @Mock PaieService variablesConges;
    @Mock ReferenceService references;
    @Mock UtilisateurCourant utilisateurCourant;
    @Mock JournalAuditService audit;

    private BulletinPaieService service;
    private ParametrePaie parametre;

    @BeforeEach
    void setUp() {
        service = new BulletinPaieService(parametres, periodes, bulletins, lignes, fiches, baremes,
                rubriques, employes, contrats, carriere, temps, variablesConges, references,
                utilisateurCourant, audit);
        parametre = new ParametrePaie();
        parametre.setSmig(new BigDecimal("1400000"));
        parametre.setPlafondCnapsSmig(BigDecimal.valueOf(8));
        parametre.setTauxCnapsSalarial(BigDecimal.ONE);
        parametre.setTauxCnapsPatronal(BigDecimal.valueOf(13));
        parametre.setTauxOstieSalarial(BigDecimal.ONE);
        parametre.setTauxOstiePatronal(BigDecimal.valueOf(5));
        parametre.setTauxFmfpPatronal(BigDecimal.ONE);
        parametre.setMajorationHs30(BigDecimal.valueOf(30));
        parametre.setMajorationHs50(BigDecimal.valueOf(50));
        parametre.setMajorationNuit(BigDecimal.valueOf(30));
        parametre.setMajorationDimanche(BigDecimal.valueOf(50));
        parametre.setMajorationFerie(BigDecimal.valueOf(100));
        parametre.setMinimumIrsa(BigDecimal.ZERO);
        parametre.setReductionCharge(BigDecimal.ZERO);
    }

    @Test
    void calculeFicheSpecimenSansRetenue() {
        BulletinPaie bulletin = new BulletinPaie();
        bulletin.setId(1L);
        service.calculerBulletin(bulletin, new BigDecimal("262680.00"),
                new TempsDtos.VariablePaieDto(7L, 0, 0, 0, 0, 0), BigDecimal.ZERO,
                parametre, bareme(), Map.of());

        assertEquals(new BigDecimal("262680.00"), bulletin.getSalaireBrut());
        assertEquals(new BigDecimal("5253.60"), bulletin.getCotisationsSalariales());
        assertEquals(new BigDecimal("257426.40"), bulletin.getBaseImposable());
        assertEquals(BigDecimal.ZERO.setScale(2), bulletin.getImpot());
        assertEquals(new BigDecimal("257426.40"), bulletin.getSalaireNet());
        assertEquals(new BigDecimal("49909.20"), bulletin.getCotisationsPatronales());
        assertEquals(StatutBulletinPaie.CALCULE, bulletin.getStatut());
        verify(bulletins).save(bulletin);
    }

    @Test
    void calculeHeuresSupplementairesValidees() {
        BulletinPaie bulletin = new BulletinPaie();
        bulletin.setId(2L);
        service.calculerBulletin(bulletin, new BigDecimal("262680.00"),
                new TempsDtos.VariablePaieDto(7L, 60, 0, 0, 0, 0), BigDecimal.ZERO,
                parametre, bareme(), Map.of());

        assertTrue(bulletin.getSalaireBrut().compareTo(new BigDecimal("264195.49")) > 0);
        ArgumentCaptor<List<LigneBulletin>> captor = ArgumentCaptor.forClass(List.class);
        verify(lignes).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().anyMatch(ligne -> "HS_30".equals(ligne.getCodeRubrique())
                && ligne.getMontantSalarial().signum() > 0));
    }

    private List<BaremeIrsa> bareme() {
        return List.of(
                tranche("0", "350000", "0"),
                tranche("350000", "400000", "5"),
                tranche("400000", "500000", "10"),
                tranche("500000", "600000", "15"),
                tranche("600000", "4000000", "20"),
                tranche("4000000", null, "25"));
    }

    private BaremeIrsa tranche(String inf, String sup, String taux) {
        BaremeIrsa tranche = new BaremeIrsa();
        tranche.setDateEffet(LocalDate.of(2026, 1, 1));
        tranche.setBorneInf(new BigDecimal(inf));
        tranche.setBorneSup(sup == null ? null : new BigDecimal(sup));
        tranche.setTaux(new BigDecimal(taux));
        return tranche;
    }
}
