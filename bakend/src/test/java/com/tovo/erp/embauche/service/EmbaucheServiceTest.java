package com.tovo.erp.embauche.service;

import com.tovo.erp.besoin.service.DemandeService;
import com.tovo.erp.contrat.service.ContratService;
import com.tovo.erp.embauche.dto.PropositionDtos;
import com.tovo.erp.embauche.model.PropositionEmbauche;
import com.tovo.erp.embauche.model.PropositionStatut;
import com.tovo.erp.embauche.repository.HistoriquePropositionRepository;
import com.tovo.erp.embauche.repository.PropositionEmbaucheRepository;
import com.tovo.erp.embauche.repository.ReponseCandidatRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.recrutement.service.CandidatureService;
import com.tovo.erp.shared.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbaucheServiceTest {

    @Mock private PropositionEmbaucheRepository propositions;
    @Mock private ReponseCandidatRepository reponses;
    @Mock private HistoriquePropositionRepository historiques;
    @Mock private CandidatureService candidatures;
    @Mock private DemandeService demandes;
    @Mock private EmployeService employes;
    @Mock private ContratService contrats;

    private EmbaucheService service;

    @BeforeEach
    void setUp() {
        service = new EmbaucheService(propositions, reponses, historiques,
                candidatures, demandes, employes, contrats);
    }

    @Test
    void uneAcceptationDejaEffectueeEstIdempotente() {
        PropositionEmbauche proposition = proposition(PropositionStatut.ACCEPTEE);
        proposition.setReference("PE-2026-0001");
        when(propositions.findById(4L)).thenReturn(Optional.of(proposition));

        PropositionDtos.ResultatAcceptation resultat = service.accepterInterne(4L, auteur(), null);

        assertEquals(4L, resultat.idProposition());
        assertEquals("PE-2026-0001", resultat.reference());
        assertEquals(PropositionStatut.ACCEPTEE, resultat.statut());
        verify(propositions, never()).save(any());
        verify(employes, never()).creerOuReactiverDossier(any(), any(), any(), any());
    }

    @Test
    void refuseUnePropositionQuiNEstPasEnvoyee() {
        PropositionEmbauche proposition = proposition(PropositionStatut.BROUILLON);
        when(propositions.findById(4L)).thenReturn(Optional.of(proposition));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.accepterInterne(4L, auteur(), null));

        assertEquals("PROPOSITION_NON_ENVOYEE", erreur.getCode());
    }

    @Test
    void marqueLaPropositionExpireeAvantDeRefuserLAcceptation() {
        PropositionEmbauche proposition = proposition(PropositionStatut.ENVOYEE);
        proposition.setDateExpiration(LocalDateTime.now().minusHours(1));
        when(propositions.findById(4L)).thenReturn(Optional.of(proposition));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.accepterInterne(4L, auteur(), null));

        assertEquals("PROPOSITION_EXPIREE", erreur.getCode());
        assertEquals(PropositionStatut.EXPIREE, proposition.getStatut());
        verify(propositions).save(proposition);
        verify(candidatures).marquerExpiree(1L);
        verify(historiques).save(any());
        verify(contrats, never()).creerContratEmbauche(any());
    }

    private UtilisateurConnecte auteur() {
        return new UtilisateurConnecte(9L, "Auteur", "auteur@example.com", "RH", null, null);
    }

    private PropositionEmbauche proposition(PropositionStatut statut) {
        PropositionEmbauche proposition = new PropositionEmbauche();
        proposition.setId(4L);
        proposition.setIdCandidature(1L);
        proposition.setStatut(statut);
        proposition.setActive(true);
        return proposition;
    }
}
