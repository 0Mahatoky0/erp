package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.repository.HistoriqueOffreRepository;
import com.tovo.erp.recrutement.repository.OffreEmploiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffreEmploiServiceTest {
    @Mock OffreEmploiRepository offres;
    @Mock HistoriqueOffreRepository historiques;
    @Mock JdbcTemplate jdbc;
    private OffreEmploiService service;

    @BeforeEach
    void setUp() {
        service = new OffreEmploiService(offres, historiques, jdbc);
    }

    @Test
    void creeUneOffreUniquementDepuisUneDemandeAcceptee() {
        OffreEmploi offre = offre(10L);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);
        when(offres.save(offre)).thenReturn(offre);

        OffreEmploi result = service.creer(offre);

        assertEquals(OffreStatut.BROUILLON, result.getStatut());
        verify(historiques).save(argThat(h -> h.getAncienStatut() == null
                && h.getNouveauStatut() == OffreStatut.BROUILLON));
    }

    @Test
    void refuseUneDemandeNonAcceptee() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(11L))).thenReturn(0);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.creer(offre(11L)));

        assertEquals("La demande de recrutement doit être ACCEPTEE", error.getMessage());
        verify(offres, never()).save(any());
    }

    @Test
    void gereLesTransitionsEtLesHistorise() {
        OffreEmploi offre = offre(10L);
        offre.setIdOffre(1L);
        when(offres.findById(1L)).thenReturn(Optional.of(offre));
        when(offres.save(offre)).thenReturn(offre);

        service.publier(1L, 7L);
        assertEquals(OffreStatut.PUBLIEE, offre.getStatut());
        service.cloturer(1L, 7L);
        assertEquals(OffreStatut.CLOTUREE, offre.getStatut());

        assertThrows(IllegalStateException.class, () -> service.annuler(1L, 7L));
        verify(historiques, times(2)).save(any(HistoriqueOffre.class));
    }

    @Test
    void refuseUneClotureAvantPublication() {
        OffreEmploi offre = offre(10L);
        offre.setIdOffre(1L);
        when(offres.findById(1L)).thenReturn(Optional.of(offre));

        assertThrows(IllegalStateException.class, () -> service.cloturer(1L, 7L));
        verify(offres, never()).save(any());
    }

    private OffreEmploi offre(Long demande) {
        OffreEmploi offre = new OffreEmploi();
        offre.setIdDemandePoste(demande);
        offre.setIdUtilisateurCreateur(7L);
        offre.setReference("REF-" + demande);
        offre.setTitre("Développeur");
        offre.setDescription("Description");
        offre.setProfilRecherche("Java");
        offre.setEffectifRecherche(1);
        offre.setModeSelection("Entretiens");
        offre.setDatePublication(LocalDateTime.now());
        return offre;
    }
}
