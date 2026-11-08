package com.tovo.erp.embauche.service;

import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.besoin.service.PosteService;
import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.model.TypeContratEmbauche;
import com.tovo.erp.embauche.repository.HistoriquePropositionRepository;
import com.tovo.erp.embauche.repository.PropositionEmbaucheRepository;
import com.tovo.erp.embauche.repository.ReponseCandidatRepository;
import com.tovo.erp.embauche.repository.VersionPropositionRepository;
import com.tovo.erp.iam.model.Role;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.recrutement.dto.CandidatureEmbaucheDto;
import com.tovo.erp.recrutement.model.CandidatureStatut;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionServiceTest {

    @Mock private PropositionEmbaucheRepository propositions;
    @Mock private VersionPropositionRepository versions;
    @Mock private ReponseCandidatRepository reponses;
    @Mock private HistoriquePropositionRepository historiques;
    @Mock private ReferenceService references;
    @Mock private CandidatureService candidatures;
    @Mock private DemandeService demandes;
    @Mock private PosteService postes;
    @Mock private EmbaucheService embauche;

    private PropositionService service;
    private Utilisateur rh;

    @BeforeEach
    void setUp() {
        EmbaucheProperties proprietes = new EmbaucheProperties(
                7, 3, 3, new BigDecimal("500000"), 3, 3_600_000L);
        service = new PropositionService(propositions, versions, reponses, historiques,
                references, candidatures, demandes, postes, embauche, proprietes);
        rh = utilisateur(9L, RoleNom.RH);
    }

    @Test
    void refuseUnePropositionPourUneCandidatureNonRetenue() {
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.EN_EVALUATION, 10L));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.creer(rh, creation(1L, new BigDecimal("600000"))));

        assertEquals("CANDIDATURE_NON_RETENUE", erreur.getCode());
        verify(propositions, never()).save(any());
    }

    @Test
    void refuseUneSecondePropositionActivePourLaMemeCandidature() {
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.RETENUE, 10L));
        when(propositions.existsByIdCandidatureAndActiveTrue(1L)).thenReturn(true);

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.creer(rh, creation(1L, new BigDecimal("600000"))));

        assertEquals("PROPOSITION_ACTIVE_EXISTANTE", erreur.getCode());
        verifyNoInteractions(versions, historiques);
    }

    @Test
    void refuseUnSalaireSousLeMinimumLegal() {
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.RETENUE, 10L));
        when(propositions.existsByIdCandidatureAndActiveTrue(1L)).thenReturn(false);
        DemandeDto.LigneDetail ligne = ligne(10L, 5L, 1, 0);
        when(demandes.detailLignePourEmbauche(10L)).thenReturn(ligne);
        when(postes.trouver(5L)).thenReturn(poste(5L, "100000", "900000"));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.creer(rh, creation(1L, new BigDecimal("100000"))));

        assertEquals("SALAIRE_SOUS_MINIMUM", erreur.getCode());
        verify(propositions, never()).save(any());
    }

    @Test
    void refuseUnCddSansDuree() {
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.RETENUE, 10L));
        when(propositions.existsByIdCandidatureAndActiveTrue(1L)).thenReturn(false);
        when(demandes.detailLignePourEmbauche(10L)).thenReturn(ligne(10L, 5L, 1, 0));
        when(postes.trouver(5L)).thenReturn(poste(5L, "500000", "900000"));

        PropositionDtos.Creation demande = new PropositionDtos.Creation(
                1L, 5L, null, null, null, TypeContratEmbauche.CDD, null,
                LocalDate.now().plusDays(10), 1, new BigDecimal("600000"), "MGA",
                null, null, null, false);

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.creer(rh, demande));

        assertEquals("DUREE_CDD_INVALIDE", erreur.getCode());
    }

    @Test
    void exigeFinanceOuDirectionPourValiderUneDerogation() {
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setId(4L);
        proposition.setStatut(PropositionStatut.BROUILLON);
        proposition.setDerogationFinanceDirection(true);
        when(propositions.findById(4L)).thenReturn(Optional.of(proposition));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.valider(4L, rh));

        assertEquals("ACCES_REFUSE", erreur.getCode());
        verify(propositions, never()).save(any());
    }

    @Test
    void refuseDEnvoyerUnePropositionNonValidee() {
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setId(4L);
        proposition.setStatut(PropositionStatut.BROUILLON);
        when(propositions.findById(4L)).thenReturn(Optional.of(proposition));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.envoyer(4L, rh));

        assertEquals("PROPOSITION_NON_VALIDEE", erreur.getCode());
    }

    @Test
    void enregistreLeRefusPublicEtMetAJourLaCandidature() {
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setId(4L);
        proposition.setIdCandidature(1L);
        proposition.setReference("PE-2026-0001");
        proposition.setVersion(1);
        proposition.setStatut(PropositionStatut.ENVOYEE);
        proposition.setDateExpiration(LocalDateTime.now().plusDays(2));
        when(propositions.findByTokenAcceptation("token")).thenReturn(Optional.of(proposition));
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.RETENUE, 10L));
        when(historiques.findByPropositionIdOrderByDateActionAsc(4L)).thenReturn(List.of());
        when(versions.findByPropositionIdOrderByNumeroVersionDesc(4L)).thenReturn(List.of());
        when(reponses.findByPropositionIdOrderByDateReponseDesc(4L)).thenReturn(List.of());

        service.refuserPublique("token", "Offre moins attractive", "127.0.0.1");

        assertEquals(PropositionStatut.REFUSEE, proposition.getStatut());
        assertFalse(proposition.isActive());
        verify(reponses).save(any());
        verify(historiques).save(any());
        verify(candidatures).marquerRefuseeParCandidat(1L, null, "Offre moins attractive");
    }

    @Test
    void desactiveLaVersionCouranteAvantDeCreerLaContreProposition() {
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setId(4L);
        proposition.setIdCandidature(1L);
        proposition.setReference("PE-2026-0001");
        proposition.setVersion(1);
        proposition.setStatut(PropositionStatut.ENVOYEE);
        proposition.setDateExpiration(LocalDateTime.now().plusDays(2));
        when(propositions.findByTokenAcceptation("token")).thenReturn(Optional.of(proposition));
        when(references.suivante("PROPOSITION_EMBAUCHE", "PE", 4)).thenReturn("PE-2026-0002");
        when(candidatures.detailsPourEmbauche(1L))
                .thenReturn(candidature(CandidatureStatut.RETENUE, 10L));
        when(historiques.findByPropositionIdOrderByDateActionAsc(any())).thenReturn(List.of());
        when(versions.findByPropositionIdOrderByNumeroVersionDesc(any())).thenReturn(List.of());
        when(reponses.findByPropositionIdOrderByDateReponseDesc(any())).thenReturn(List.of());

        service.contreProposerPublique("token", "Souhaite un meilleur salaire", "127.0.0.1");

        assertFalse(proposition.isActive());
        assertEquals(PropositionStatut.RETIREE, proposition.getStatut());
        verify(propositions).flush();
    }

    private PropositionDtos.Creation creation(Long idCandidature, BigDecimal salaire) {
        return new PropositionDtos.Creation(
                idCandidature, 5L, null, null, null, TypeContratEmbauche.CDI, null,
                LocalDate.now().plusDays(10), 1, salaire, "MGA",
                null, null, null, false);
    }

    private CandidatureEmbaucheDto candidature(CandidatureStatut statut, Long idDemandePoste) {
        return new CandidatureEmbaucheDto(
                1L, 2L, "Rakoto", "Anja", "anja@example.com", "0340000000",
                "Antananarivo", "1234567890", LocalDate.of(1995, 5, 12),
                3L, "OF-2026-0001", "Developpeur", "CDI", "Antananarivo", 1,
                new BigDecimal("500000"), new BigDecimal("900000"), idDemandePoste,
                statut, LocalDateTime.now().minusDays(2));
    }

    private DemandeDto.LigneDetail ligne(Long id, Long idPoste, int effectif, int nombrePourvus) {
        return new DemandeDto.LigneDetail(
                id, 20L, 2L, "Informatique", "Technique",
                idPoste, "Developpeur", null, null,
                null, null, null, null, null, null,
                "Developpeur", null, null, null, null,
                effectif, nombrePourvus, null, null, null,
                1, null, null, null, List.of(), List.of());
    }

    private PosteDto.Liste poste(Long id, String salaireMin, String salaireMax) {
        return new PosteDto.Liste(
                id, 2L, "Informatique", 3L, "Technique", "Developpeur", "Developpement",
                new BigDecimal(salaireMin), new BigDecimal(salaireMax), true, 0);
    }

    private Utilisateur utilisateur(Long id, RoleNom roleNom) {
        Role role = new Role();
        role.setNom(roleNom.name());
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        return utilisateur;
    }
}
