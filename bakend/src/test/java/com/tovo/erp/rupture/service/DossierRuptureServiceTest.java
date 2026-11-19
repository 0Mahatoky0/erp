package com.tovo.erp.rupture.service;

import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.discipline.service.ProcedureService;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.employe.service.MissionService;
import com.tovo.erp.evaluation.service.FormationService;
import com.tovo.erp.iam.service.UtilisateurService;
import com.tovo.erp.rupture.dto.RuptureDtos;
import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.InitiateurRupture;
import com.tovo.erp.rupture.model.ModePreavis;
import com.tovo.erp.rupture.model.MotifRupture;
import com.tovo.erp.rupture.model.SoldeToutCompte;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import com.tovo.erp.rupture.model.StatutSoldeRupture;
import com.tovo.erp.rupture.repository.DocumentRuptureRepository;
import com.tovo.erp.rupture.repository.DossierRuptureRepository;
import com.tovo.erp.rupture.repository.DroitsRuptureRepository;
import com.tovo.erp.rupture.repository.EntretienRuptureRepository;
import com.tovo.erp.rupture.repository.MaterielRuptureRepository;
import com.tovo.erp.rupture.repository.SoldeToutCompteRepository;
import com.tovo.erp.rupture.repository.TacheDepartRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import com.tovo.erp.temps.service.TempsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierRuptureServiceTest {

    @Mock private DossierRuptureRepository dossiers;
    @Mock private DroitsRuptureRepository droitsRepository;
    @Mock private SoldeToutCompteRepository soldes;
    @Mock private TacheDepartRepository taches;
    @Mock private MaterielRuptureRepository materiels;
    @Mock private DocumentRuptureRepository documents;
    @Mock private EntretienRuptureRepository entretiens;
    @Mock private ReferenceService references;
    @Mock private ContratService contrats;
    @Mock private CarriereService carrieres;
    @Mock private EmployeService employes;
    @Mock private ProcedureService procedures;
    @Mock private CongeService conges;
    @Mock private TempsService temps;
    @Mock private MissionService missions;
    @Mock private DemandeService demandes;
    @Mock private PosteService postes;
    @Mock private FormationService formations;
    @Mock private UtilisateurService utilisateurs;
    @Mock private UtilisateurCourant utilisateurCourant;
    @Mock private JournalAuditService audit;
    @Mock private SoldeRuptureService soldeService;

    private DossierRuptureService service;

    @BeforeEach
    void setUp() {
        RuptureProperties properties = new RuptureProperties(30, 30, 30, 30, 0, 10, 30, 5, 6);
        service = new DossierRuptureService(dossiers, droitsRepository, soldes, taches, materiels,
                documents, entretiens, references, contrats, carrieres, employes, procedures,
                conges, temps, missions, demandes, postes, formations, utilisateurs,
                utilisateurCourant, audit, properties, soldeService);
    }

    @Test
    void calculeLePreavisLorsDeLaNotification() {
        DossierRupture dossier = dossier(StatutDossierRupture.INITIEE);
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossiers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taches.findByIdDossierOrderByIdAsc(1L)).thenReturn(List.of());
        when(employes.dossier(20L)).thenReturn(employe());
        when(employes.idUtilisateurDe(20L)).thenReturn(Optional.empty());

        RuptureDtos.DossierDto resultat = service.notifier(1L);

        assertEquals(LocalDate.of(2026, 11, 14), resultat.datePreavisDebut());
        assertEquals(LocalDate.of(2026, 12, 13), resultat.datePreavisFin());
        assertEquals(LocalDate.of(2026, 12, 13), resultat.dateSortie());
        assertEquals(StatutDossierRupture.EN_PREAVIS, resultat.statut());
        verify(taches).saveAll(any());
    }

    @Test
    void exigeUnAvenantPourModifierLaDateDeSortie() {
        DossierRupture dossier = dossier(StatutDossierRupture.EN_PREAVIS);
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.modifierDateSortie(1L,
                        new RuptureDtos.ModificationDateSortie(
                                LocalDate.of(2026, 12, 20), false, null)));

        assertEquals("AVENANT_REQUIS", erreur.getCode());
    }

    @Test
    void refuseUnLicenciementDisciplinaireSansProcedureDecidee() {
        RuptureDtos.CreationDossier demande = new RuptureDtos.CreationDossier(
                10L, MotifRupture.LICENCIEMENT_DISCIPLINAIRE, InitiateurRupture.EMPLOYEUR,
                LocalDate.of(2026, 11, 13), null, 30, ModePreavis.EFFECTUE,
                null, "Faits graves", 900L);
        when(contrats.contrat(10L)).thenReturn(contratActif());
        when(dossiers.existsByIdEmployeAndStatutNotIn(anyLong(), any())).thenReturn(false);
        when(procedures.licenciementDecide(900L, 20L)).thenReturn(false);

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.creer(demande));

        assertEquals("LICENCIEMENT_NON_AUTORISE", erreur.getCode());
    }

    @Test
    void refuseLaClotureSiLaChecklistEstIncomplete() {
        DossierRupture dossier = dossier(StatutDossierRupture.DOCUMENTS_REMIS);
        dossier.setDateSortie(LocalDate.now().minusDays(1));
        SoldeToutCompte solde = new SoldeToutCompte();
        solde.setStatut(StatutSoldeRupture.PAYE);
        solde.setNetAvantCotisations(BigDecimal.TEN);
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(soldes.findByIdDossier(1L)).thenReturn(Optional.of(solde));
        when(taches.countByIdDossierAndBloquanteTrueAndStatutNot(anyLong(), any())).thenReturn(2L);
        when(utilisateurCourant.exiger()).thenReturn(utilisateur(6L));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.cloturer(1L));

        assertEquals("CHECKLIST_INCOMPLETE", erreur.getCode());
    }

    private DossierRupture dossier(StatutDossierRupture statut) {
        DossierRupture dossier = new DossierRupture();
        dossier.setId(1L);
        dossier.setReference("RUP-2026-0001");
        dossier.setIdContrat(10L);
        dossier.setIdEmploye(20L);
        dossier.setMotif(MotifRupture.DEMISSION);
        dossier.setInitiateur(InitiateurRupture.SALARIE);
        dossier.setDateNotification(LocalDate.of(2026, 11, 13));
        dossier.setPreavisJours(30);
        dossier.setPreavisMode(ModePreavis.EFFECTUE);
        dossier.setStatut(statut);
        return dossier;
    }

    private ContratDtos.ContratDto contratActif() {
        Contrat contrat = new Contrat();
        contrat.setId(10L);
        contrat.setNumero("CT-2026-0001");
        contrat.setIdEmploye(20L);
        contrat.setRang(1);
        contrat.setDateDebut(LocalDate.of(2022, 5, 18));
        contrat.setSalaireBase(BigDecimal.valueOf(1200000));
        contrat.setDevise("MGA");
        contrat.setLieuTravail("Antananarivo");
        contrat.setStatutExecution(StatutExecutionContrat.EN_COURS);
        return ContratDtos.ContratDto.de(contrat);
    }

    private EmployeEmbaucheDto employe() {
        return new EmployeEmbaucheDto(20L, "MAT-001", "Rakoto", "Andry", "a@example.test",
                null, null, null, null, null, null, null, null, null,
                "ACTIF", LocalDate.of(2022, 5, 18), LocalDate.of(2022, 5, 18), LocalDateTime.now());
    }

    private UtilisateurConnecte utilisateur(Long id) {
        return new UtilisateurConnecte(id, "RH", "rh@example.test", Roles.RH, null, null);
    }
}
