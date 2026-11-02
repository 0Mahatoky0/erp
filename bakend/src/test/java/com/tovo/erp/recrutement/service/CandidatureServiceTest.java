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
        Candidature candidature = new Candidature();
        candidature.setIdCandidature(3L);
        candidature.setStatut(CandidatureStatut.RECUE);
        when(candidatures.findById(3L)).thenReturn(Optional.of(candidature));
        when(candidatures.save(candidature)).thenReturn(candidature);

        service.changerStatut(3L, CandidatureStatut.PRESELECTIONNEE, 7L, "Profil retenu");

        assertEquals(CandidatureStatut.PRESELECTIONNEE, candidature.getStatut());
        verify(historiques).save(argThat(h -> h.getAncienStatut() == CandidatureStatut.RECUE
                && h.getNouveauStatut() == CandidatureStatut.PRESELECTIONNEE));
    }
}
