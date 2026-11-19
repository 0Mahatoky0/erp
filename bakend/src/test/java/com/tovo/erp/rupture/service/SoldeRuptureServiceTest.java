package com.tovo.erp.rupture.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.paie.dto.BulletinPaieDtos;
import com.tovo.erp.paie.model.StatutBulletinPaie;
import com.tovo.erp.paie.service.BulletinPaieService;
import com.tovo.erp.rupture.dto.RuptureDtos;
import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.DroitsRupture;
import com.tovo.erp.rupture.model.LigneSoldeRupture;
import com.tovo.erp.rupture.model.ModePreavis;
import com.tovo.erp.rupture.model.MotifRupture;
import com.tovo.erp.rupture.model.SoldeToutCompte;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import com.tovo.erp.rupture.model.StatutSoldeRupture;
import com.tovo.erp.rupture.repository.DossierRuptureRepository;
import com.tovo.erp.rupture.repository.DroitsRuptureRepository;
import com.tovo.erp.rupture.repository.LigneSoldeRuptureRepository;
import com.tovo.erp.rupture.repository.MaterielRuptureRepository;
import com.tovo.erp.rupture.repository.SoldeToutCompteRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoldeRuptureServiceTest {

    @Mock private SoldeToutCompteRepository soldes;
    @Mock private LigneSoldeRuptureRepository lignes;
    @Mock private DossierRuptureRepository dossiers;
    @Mock private DroitsRuptureRepository droitsRepository;
    @Mock private MaterielRuptureRepository materiels;
    @Mock private ContratService contrats;
    @Mock private EmployeService employes;
    @Mock private BulletinPaieService bulletins;
    @Mock private UtilisateurCourant utilisateurCourant;
    @Mock private JournalAuditService audit;

    private SoldeRuptureService service;

    @BeforeEach
    void setUp() {
        RuptureProperties properties = new RuptureProperties(30, 30, 30, 30, 0, 10, 30, 5, 6);
        service = new SoldeRuptureService(soldes, lignes, dossiers, droitsRepository, materiels,
                contrats, employes, bulletins, properties, utilisateurCourant, audit);
    }

    @Test
    void calculeLeSoldeAvecDispatchEtLicenciement() {
        DossierRupture dossier = dossier(MotifRupture.LICENCIEMENT_ECONOMIQUE,
                LocalDate.of(2026, 5, 18), ModePreavis.DISPENSE_EMPLOYEUR, 30);
        preparerDonnees(dossier, new BigDecimal("1200000"), new BigDecimal("12"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(utilisateurCourant.exiger()).thenReturn(utilisateur(7L));

        RuptureDtos.SoldeDto resultat = service.preparer(1L);

        assertEquals(0, new BigDecimal("720000.00").compareTo(resultat.salaireDu()));
        assertEquals(0, new BigDecimal("480000.00").compareTo(resultat.indemniteConges()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(resultat.indemnitePreavis()));
        assertEquals(0, new BigDecimal("1600000.00").compareTo(resultat.indemniteLicenciement()));
        assertEquals(0, new BigDecimal("4000000.00").compareTo(resultat.totalBrut()));
        assertEquals(0, new BigDecimal("4000000.00").compareTo(resultat.netAvantCotisations()));
        assertEquals(4, resultat.lignes().size());
        assertEquals(StatutDossierRupture.SOLDE_EN_PREPARATION, dossier.getStatut());
    }

    @Test
    void calculeLeSoldeDuneDemissionAvecRetenues() {
        DossierRupture dossier = dossier(MotifRupture.DEMISSION,
                LocalDate.of(2026, 12, 13), ModePreavis.EFFECTUE, 30);
        preparerDonnees(dossier, new BigDecimal("1200000"), new BigDecimal("12"),
                new BigDecimal("100000"), new BigDecimal("1875000"));
        when(utilisateurCourant.exiger()).thenReturn(utilisateur(7L));

        RuptureDtos.SoldeDto resultat = service.preparer(1L);

        assertEquals(0, new BigDecimal("520000.00").compareTo(resultat.salaireDu()));
        assertEquals(0, new BigDecimal("1000000.00").compareTo(resultat.totalBrut()));
        assertEquals(0, new BigDecimal("1975000.00").compareTo(resultat.totalRetenues()));
        assertEquals(0, new BigDecimal("-975000.00").compareTo(resultat.netAvantCotisations()));
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.indemnitePreavis());
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.indemniteLicenciement());
    }

    @Test
    void refuseLaValidationParLeCalculateur() {
        DossierRupture dossier = dossier(MotifRupture.DEMISSION,
                LocalDate.of(2026, 12, 13), ModePreavis.EFFECTUE, 30);
        SoldeToutCompte solde = solde(StatutSoldeRupture.EN_ATTENTE_VALIDATION, 7L);
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(soldes.findByIdDossier(1L)).thenReturn(Optional.of(solde));
        when(utilisateurCourant.exiger()).thenReturn(utilisateur(7L));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.valider(1L, new RuptureDtos.DecisionSolde(null)));

        assertEquals("SEPARATION_TACHES", erreur.getCode());
    }

    @Test
    void refuseLeRecalculDunSoldeValide() {
        DossierRupture dossier = dossier(MotifRupture.DEMISSION,
                LocalDate.of(2026, 12, 13), ModePreavis.EFFECTUE, 30);
        SoldeToutCompte solde = solde(StatutSoldeRupture.VALIDE, 3L);
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(soldes.findByIdDossier(1L)).thenReturn(Optional.of(solde));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.preparer(1L));

        assertEquals("SOLDE_IMMUTABLE", erreur.getCode());
    }

    @Test
    void genereEtLieLeBulletinFinalLorsDuPaiement() {
        DossierRupture dossier = dossier(MotifRupture.LICENCIEMENT_ECONOMIQUE,
                LocalDate.of(2026, 5, 18), ModePreavis.DISPENSE_EMPLOYEUR, 30);
        SoldeToutCompte solde = solde(StatutSoldeRupture.VALIDE, 3L);
        solde.setTotalBrut(new BigDecimal("4000000"));
        solde.setTotalRetenues(new BigDecimal("100000"));
        solde.setNetAvantCotisations(new BigDecimal("3900000"));
        LigneSoldeRupture ligneSolde = new LigneSoldeRupture();
        ligneSolde.setCode("SALAIRE_DU");
        ligneSolde.setLibelle("Salaire du");
        ligneSolde.setMontant(new BigDecimal("720000"));
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(soldes.findByIdDossier(1L)).thenReturn(Optional.of(solde));
        when(contrats.contrat(10L)).thenReturn(contrat(new BigDecimal("1200000")));
        when(lignes.findByIdSoldeOrderByIdAsc(100L)).thenReturn(List.of(ligneSolde));
        when(bulletins.genererBulletinSolde(any())).thenReturn(bulletinFinal());
        when(soldes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuptureDtos.SoldeDto resultat = service.payer(1L,
                new RuptureDtos.PaiementSolde("PAY-2026-0001", "VIREMENT"));

        assertEquals(99L, resultat.idBulletinPaie());
        ArgumentCaptor<BulletinPaieDtos.SoldePaieRequest> captor =
                ArgumentCaptor.forClass(BulletinPaieDtos.SoldePaieRequest.class);
        verify(bulletins).genererBulletinSolde(captor.capture());
        assertEquals("RUP-2026-0001", captor.getValue().referenceExterne());
        assertEquals(1, captor.getValue().lignes().size());
        assertEquals(18, captor.getValue().joursPayes());
    }

    private void preparerDonnees(DossierRupture dossier, BigDecimal salaire,
                                 BigDecimal conges, BigDecimal avances, BigDecimal dedit) {
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        DroitsRupture droits = new DroitsRupture();
        droits.setIdDossier(1L);
        droits.setCongeSolde(conges);
        droits.setAvancesRestantes(avances);
        droits.setDeditFormation(dedit);
        when(droitsRepository.findByIdDossier(1L)).thenReturn(Optional.of(droits));
        when(contrats.contrat(10L)).thenReturn(contrat(salaire));
        when(employes.dossier(20L)).thenReturn(employe());
        when(materiels.findByIdDossierOrderByIdAsc(1L)).thenReturn(List.of());
        AtomicReference<SoldeToutCompte> soldeEnregistre = new AtomicReference<>();
        AtomicReference<List<LigneSoldeRupture>> lignesEnregistrees = new AtomicReference<>(new ArrayList<>());
        when(soldes.save(any())).thenAnswer(invocation -> {
            SoldeToutCompte valeur = invocation.getArgument(0);
            valeur.setId(100L);
            soldeEnregistre.set(valeur);
            return valeur;
        });
        when(soldes.findByIdDossier(1L)).thenAnswer(
                invocation -> Optional.ofNullable(soldeEnregistre.get()));
        when(lignes.saveAll(anyList())).thenAnswer(invocation -> {
            List<LigneSoldeRupture> valeur = invocation.getArgument(0);
            lignesEnregistrees.set(new ArrayList<>(valeur));
            return valeur;
        });
        when(lignes.findByIdSoldeOrderByIdAsc(100L)).thenAnswer(
                invocation -> lignesEnregistrees.get());
    }

    private DossierRupture dossier(MotifRupture motif, LocalDate dateSortie,
                                   ModePreavis mode, int jours) {
        DossierRupture dossier = new DossierRupture();
        dossier.setId(1L);
        dossier.setReference("RUP-2026-0001");
        dossier.setIdContrat(10L);
        dossier.setIdEmploye(20L);
        dossier.setMotif(motif);
        dossier.setDateNotification(LocalDate.of(2026, 11, 13));
        dossier.setDateSortie(dateSortie);
        dossier.setPreavisJours(jours);
        dossier.setPreavisMode(mode);
        dossier.setStatut(StatutDossierRupture.DROITS_ARRETES);
        return dossier;
    }

    private SoldeToutCompte solde(StatutSoldeRupture statut, Long idCalculateur) {
        SoldeToutCompte solde = new SoldeToutCompte();
        solde.setId(100L);
        solde.setIdDossier(1L);
        solde.setStatut(statut);
        solde.setIdCalculateur(idCalculateur);
        solde.setNetAvantCotisations(BigDecimal.ZERO);
        return solde;
    }

    private ContratDtos.ContratDto contrat(BigDecimal salaire) {
        Contrat contrat = new Contrat();
        contrat.setId(10L);
        contrat.setNumero("CT-2026-0001");
        contrat.setIdEmploye(20L);
        contrat.setRang(1);
        contrat.setDateDebut(LocalDate.of(2022, 5, 18));
        contrat.setSalaireBase(salaire);
        contrat.setDevise("MGA");
        contrat.setLieuTravail("Antananarivo");
        return ContratDtos.ContratDto.de(contrat);
    }

    private EmployeEmbaucheDto employe() {
        return new EmployeEmbaucheDto(20L, "MAT-001", "Rakoto", "Andry", "a@example.test",
                null, null, null, 30L, "Developpeur", 40L, "Informatique", null, null,
                "ACTIF", LocalDate.of(2022, 5, 18), LocalDate.of(2022, 5, 18), LocalDateTime.now());
    }

    private BulletinPaieDtos.BulletinDto bulletinFinal() {
        return new BulletinPaieDtos.BulletinDto(99L, 1L, 20L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                StatutBulletinPaie.VALIDE, 18, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, null, "BSD-000001",
                BulletinPaieDtos.TYPE_SOLDE_TOUT_COMPTE);
    }

    private UtilisateurConnecte utilisateur(Long id) {
        return new UtilisateurConnecte(id, "RH", "rh@example.test", Roles.RH, null, null);
    }
}
