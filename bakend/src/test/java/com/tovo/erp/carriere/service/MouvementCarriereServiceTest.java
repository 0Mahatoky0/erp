package com.tovo.erp.carriere.service;

import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.carriere.dto.CarriereMobiliteDtos;
import com.tovo.erp.carriere.model.CampagneAugmentation;
import com.tovo.erp.carriere.model.MouvementCarriere;
import com.tovo.erp.carriere.model.TypeMouvementCarriere;
import com.tovo.erp.carriere.repository.CampagneAugmentationRepository;
import com.tovo.erp.carriere.repository.GrilleSalarialeRepository;
import com.tovo.erp.carriere.repository.MouvementCarriereRepository;
import com.tovo.erp.carriere.repository.MutationRepository;
import com.tovo.erp.carriere.repository.PromotionRepository;
import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.service.CarriereService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.discipline.service.EligibiliteDisciplineService;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.evaluation.service.EvaluationService;
import com.tovo.erp.evaluation.service.ReferentielService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import com.tovo.erp.shared.reference.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MouvementCarriereServiceTest {

    @Mock private MouvementCarriereRepository mouvements;
    @Mock private PromotionRepository promotions;
    @Mock private MutationRepository mutations;
    @Mock private GrilleSalarialeRepository grilles;
    @Mock private CampagneAugmentationRepository campagnes;
    @Mock private EmployeService employes;
    @Mock private ContratService contrats;
    @Mock private CarriereService historique;
    @Mock private PosteService postes;
    @Mock private EvaluationService evaluations;
    @Mock private ReferentielService referentiel;
    @Mock private EligibiliteDisciplineService eligibiliteDiscipline;
    @Mock private UtilisateurCourant utilisateurCourant;
    @Mock private JournalAuditService audit;
    @Mock private ReferenceService references;

    private MouvementCarriereService service;

    @BeforeEach
    void setUp() {
        service = new MouvementCarriereService(mouvements, promotions, mutations, grilles,
                campagnes, employes, contrats, historique, postes, evaluations, referentiel,
                eligibiliteDiscipline, utilisateurCourant, audit, references);
    }

    @Test
    void creeUnMouvementAvecSaReference() {
        when(employes.existe(5L)).thenReturn(true);
        when(references.suivante("MOUVEMENT_CARRIERE", "MV", 4)).thenReturn("MV-2026-0001");
        when(utilisateurCourant.exiger()).thenReturn(utilisateur());
        when(mouvements.save(any())).thenAnswer(invocation -> {
            MouvementCarriere mouvement = invocation.getArgument(0);
            mouvement.setId(1L);
            return mouvement;
        });

        CarriereMobiliteDtos.MouvementDto resultat = service.creer(new CarriereMobiliteDtos.MouvementRequest(
                5L, TypeMouvementCarriere.PROMOTION, "RH", "Promotion", "Justification",
                LocalDate.of(2026, 4, 1), null, null, null, false, null,
                new CarriereMobiliteDtos.PromotionRequest("CHANGEMENT_ECHELON", 7L, 8L,
                        "CADRE", "CADRE", "E2", "E3", new BigDecimal("1200000"),
                        new BigDecimal("1500000")),
                null, new BigDecimal("1500000")));

        assertEquals("MV-2026-0001", resultat.reference());
        assertEquals(TypeMouvementCarriere.PROMOTION, resultat.type());
        verify(promotions).save(any());
    }

    @Test
    void refuseUneDerogationAutoDeclareeParLeDemandeur() {
        when(employes.existe(5L)).thenReturn(true);
        when(utilisateurCourant.exiger()).thenReturn(utilisateur());

        BusinessException erreur = assertThrows(BusinessException.class, () ->
                service.creer(new CarriereMobiliteDtos.MouvementRequest(
                        5L, TypeMouvementCarriere.PROMOTION, "RH", "Promotion", "Justification",
                        LocalDate.of(2026, 4, 1), null, null, null, true, "Motif",
                        new CarriereMobiliteDtos.PromotionRequest("CHANGEMENT_ECHELON", 7L, 8L,
                                "CADRE", "CADRE", "E2", "E3", new BigDecimal("1200000"),
                                new BigDecimal("1500000")),
                        null, new BigDecimal("1500000"))));

        assertEquals("ACCES_REFUSE", erreur.getCode());
    }

    @Test
    void reconstitueLaCarriereALaDateDeConnaissance() {
        ContratDtos.AffectationDto affectation = new ContratDtos.AffectationDto(1L, 5L, 3L, 7L,
                2L, 4L, 9L, "Antananarivo", LocalDate.of(2026, 1, 1), null, 100, true, null, null,
                "CADRE", null, "E3", LocalDateTime.of(2026, 1, 1, 8, 0), null, null);
        com.tovo.erp.contrat.dto.CarriereDtos.SalaireDto salaire =
                new com.tovo.erp.contrat.dto.CarriereDtos.SalaireDto(2L, 5L, 3L,
                        new BigDecimal("1200000"), "MGA", LocalDate.of(2026, 1, 1), null,
                        "Salaire du contrat", 9L, LocalDateTime.of(2026, 1, 1, 8, 0),
                        LocalDateTime.of(2026, 1, 1, 8, 0), null, null, null, null, null);
        when(contrats.affectationsDe(5L)).thenReturn(List.of(affectation));
        when(historique.salaires(5L)).thenReturn(List.of(salaire));
        when(contrats.contratsDe(5L)).thenReturn(List.of());

        CarriereMobiliteDtos.CarriereDateDto resultat = service.carriereALaDate(5L,
                LocalDate.of(2026, 3, 20), LocalDate.of(2026, 4, 1));

        assertEquals(7L, resultat.idPoste());
        assertEquals(new BigDecimal("1200000"), resultat.salaireBrut());
        assertEquals("CADRE", resultat.categorie());
    }

    @Test
    void simuleUneCampagneSansDepasserLEnveloppe() {
        CampagneAugmentation campagne = new CampagneAugmentation();
        campagne.setId(8L);
        campagne.setLibelle("Augmentation 2026");
        campagne.setDateEffet(LocalDate.of(2026, 5, 1));
        campagne.setEnveloppeBudgetaire(new BigDecimal("1000000"));
        campagne.setPopulation("ACTIFS");
        campagne.setRegle("{\"taux\":0.05}");
        when(campagnes.findById(8L)).thenReturn(Optional.of(campagne));
        when(employes.actifs()).thenReturn(List.of(new EmployeDto.Actif(5L, "Rakoto", "Andry", 2L,
                LocalDate.of(2024, 1, 1))));
        when(historique.salairesBaseParIds(List.of(5L))).thenReturn(Map.of(5L, new BigDecimal("1200000")));

        CarriereMobiliteDtos.CampagneDto resultat = service.simulerCampagne(8L);

        assertEquals(0, new BigDecimal("720000").compareTo(
                (BigDecimal) resultat.simulation().get("impactAnnuel")));
        assertFalse((Boolean) resultat.simulation().get("depasseEnveloppe"));
        verify(audit).enregistrer(any(), any(), any(), any(), any());
        assertTrue(resultat.simulation().containsKey("lignes"));
    }

    private UtilisateurConnecte utilisateur() {
        return new UtilisateurConnecte(9L, "RH", "rh@erp.test", Roles.RH, 2L, "RH");
    }
}
