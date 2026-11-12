package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {
    @Mock CandidatRepository candidats;
    @Mock CandidatureRepository candidatures;
    @Mock OffreEmploiRepository offres;
    @Mock HistoriqueCandidatureRepository historiques;
    private CandidatureService service;

    @BeforeEach
    void setUp() {
        service = new CandidatureService(candidats, candidatures, offres, historiques);
    }

    @Test
    void accepteUneCandidatureEtCreeSonHistorique() {
        OffreEmploi offre = new OffreEmploi();
        offre.setIdOffre(1L);
        offre.setStatut(OffreStatut.PUBLIEE);
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(2L);
        candidat.setEmail("candidat@example.com");
        when(offres.findById(1L)).thenReturn(Optional.of(offre));
        when(candidats.findByEmailIgnoreCase(candidat.getEmail())).thenReturn(Optional.of(candidat));
        when(candidatures.existsByOffreIdOffreAndCandidatIdCandidat(1L, 2L)).thenReturn(false);
        when(candidatures.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Candidature result = service.postuler(1L, candidat, "Motivé", "PORTAIL");

        assertSame(candidat, result.getCandidat());
        assertEquals(CandidatureStatut.RECUE, result.getStatut());
        verify(historiques).save(argThat(h -> h.getNouveauStatut() == CandidatureStatut.RECUE));
    }

    @Test
    void empecheUneDoubleCandidature() {
        OffreEmploi offre = new OffreEmploi();
        offre.setIdOffre(1L);
        offre.setStatut(OffreStatut.PUBLIEE);
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(2L);
        candidat.setEmail("candidat@example.com");
        when(offres.findById(1L)).thenReturn(Optional.of(offre));
        when(candidats.findByEmailIgnoreCase(candidat.getEmail())).thenReturn(Optional.of(candidat));
        when(candidatures.existsByOffreIdOffreAndCandidatIdCandidat(1L, 2L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.postuler(1L, candidat, null, null));
        verify(candidatures, never()).save(any());
    }

    @Test
    void historiseLaPreselection() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(2L);
        candidat.setNom("Nom");
        candidat.setPrenom("Prenom");
        candidat.setEmail("candidat@example.com");
        OffreEmploi offre = new OffreEmploi();
        offre.setIdOffre(1L);
        offre.setReference("OF-2026-0001");
        offre.setTitre("Developpeur");
        offre.setStatut(OffreStatut.PUBLIEE);
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        candidature.setCandidat(candidat);
        candidature.setOffre(offre);
        candidature.setStatut(CandidatureStatut.EN_ETUDE);
        when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));
        when(candidatures.save(candidature)).thenReturn(candidature);

        service.changerStatut(3L, CandidatureStatut.PRESELECTIONNEE, 7L, "Profil retenu");

        assertEquals(CandidatureStatut.PRESELECTIONNEE, candidature.getStatut());
        verify(historiques).save(argThat(h -> h.getAncienStatut() == CandidatureStatut.EN_ETUDE
                && h.getNouveauStatut() == CandidatureStatut.PRESELECTIONNEE));
    }

    @Test
    void historiseUneAcceptationPubliqueCommeActionDuCandidat() {
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        candidature.setStatut(CandidatureStatut.RETENUE);
        Candidat candidat = new Candidat();
        candidature.setCandidat(candidat);
        OffreEmploi offre = new OffreEmploi();
        offre.setStatut(OffreStatut.PUBLIEE);
        candidature.setOffre(offre);
        when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));
        when(candidatures.save(candidature)).thenReturn(candidature);

        service.marquerEmbauchee(3L, AuteurType.CANDIDAT, null, "Acceptation publique");

        verify(historiques).save(argThat(h -> h.getAuteurType() == AuteurType.CANDIDAT
                && h.getIdUtilisateur() == null
                && h.getNouveauStatut() == CandidatureStatut.EMBAUCHEE));
    }

    @Test
    void refuseUneTransitionQuiContourneLeCycle() {
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        candidature.setStatut(CandidatureStatut.RECUE);
        when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));

        assertThrows(IllegalStateException.class,
                () -> service.changerStatut(3L, CandidatureStatut.RETENUE, 7L, "Décision"));
        verify(candidatures, never()).save(any());
        verifyNoInteractions(historiques);
    }

    @Test
    void rendLesStatutsFinauxEffectivementTerminaux() {
        for (CandidatureStatut statutFinal : new CandidatureStatut[] {
                CandidatureStatut.RETENUE,
                CandidatureStatut.REFUSEE,
                CandidatureStatut.RETIREE
        }) {
            Candidature candidature = new Candidature();
            candidature.setIdCandidature(3L);
            candidature.setStatut(statutFinal);
            when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));

            assertThrows(IllegalStateException.class,
                    () -> service.changerStatut(3L, CandidatureStatut.EN_ETUDE, 7L, "Réouverture"));
        }
        verify(candidatures, never()).save(any());
        verifyNoInteractions(historiques);
    }

    @Test
    void retrouveLesCandidaturesDUnCandidatParEmailPourLeSuiviPublic() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(42L);
        candidat.setEmail("test@example.com");
        Candidature une = new Candidature();
        une.setIdCandidature(10L);
        une.setCandidat(candidat);
        une.setStatut(CandidatureStatut.RECUE);
        Candidature deux = new Candidature();
        deux.setIdCandidature(11L);
        deux.setCandidat(candidat);
        deux.setStatut(CandidatureStatut.RETENUE);

        when(candidats.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(candidat));
        when(candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(42L))
                .thenReturn(java.util.List.of(une, deux));

        var resultats = service.listerParEmailPublic("test@example.com");

        assertEquals(2, resultats.size());
        assertEquals(CandidatureStatut.RECUE, resultats.get(0).statut());
        assertEquals(CandidatureStatut.RETENUE, resultats.get(1).statut());
    }

    @Test
    void supporteUneCandidatureSansOffreLieePourLeSuiviPublic() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(42L);
        candidat.setEmail("test@example.com");
        Candidature sansOffre = new Candidature();
        sansOffre.setIdCandidature(12L);
        sansOffre.setCandidat(candidat);
        sansOffre.setStatut(CandidatureStatut.RECUE);

        when(candidats.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(candidat));
        when(candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(42L))
                .thenReturn(java.util.List.of(sansOffre));

        var resultats = service.listerParEmailPublic("test@example.com");

        assertEquals(1, resultats.size());
        assertNull(resultats.get(0).offre());
        assertEquals(CandidatureStatut.RECUE, resultats.get(0).statut());
    }

    @Test
    void retrouveLesCandidaturesDuCompteCandidatConnecte() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(42L);
        candidat.setIdUtilisateur(99L);
        candidat.setEmail("connecte@example.com");
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(21L);
        candidature.setCandidat(candidat);
        candidature.setStatut(CandidatureStatut.RETENUE);

        when(candidats.findByIdUtilisateur(99L)).thenReturn(Optional.of(candidat));
        when(candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(42L))
                .thenReturn(java.util.List.of(candidature));

        var resultats = service.listerParUtilisateurConnecte(99L);

        assertEquals(1, resultats.size());
        assertEquals(21L, resultats.get(0).idCandidature());
        assertEquals(CandidatureStatut.RETENUE, resultats.get(0).statut());
    }

    @Test
    void retrouveLesCandidaturesParEmailQuandLeLienIdUtilisateurEstAbsent() {
        Candidat candidat = new Candidat();
        candidat.setIdCandidat(55L);
        candidat.setIdUtilisateur(null);
        candidat.setEmail("iharilala@example.com");
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(31L);
        candidature.setCandidat(candidat);
        candidature.setStatut(CandidatureStatut.RECUE);

        when(candidats.findByIdUtilisateur(123L)).thenReturn(Optional.empty());
        when(candidats.findByEmailIgnoreCase("iharilala@example.com")).thenReturn(Optional.of(candidat));
        when(candidatures.findByCandidatIdCandidatOrderByDateCandidatureDesc(55L))
                .thenReturn(java.util.List.of(candidature));

        var resultats = service.listerParUtilisateurConnecte(123L, "iharilala@example.com");

        assertEquals(1, resultats.size());
        assertEquals(31L, resultats.get(0).idCandidature());
    }
}
