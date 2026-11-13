package com.tovo.erp.shared.audit;

import com.tovo.erp.shared.UtilisateurCourant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalAuditServiceTest {

    @Mock
    private JournalAuditRepository journal;

    @Mock
    private UtilisateurCourant utilisateurCourant;

    private final AtomicReference<JournalAudit> derniere = new AtomicReference<>();
    private final AtomicLong sequence = new AtomicLong();
    private JournalAuditService service;

    @BeforeEach
    void setUp() {
        service = new JournalAuditService(journal, utilisateurCourant);
        lenient().when(utilisateurCourant.optionnel()).thenReturn(null);
        lenient().when(journal.findTopByOrderByIdDesc())
                .thenAnswer(invocation -> Optional.ofNullable(derniere.get()));
        lenient().when(journal.save(any(JournalAudit.class))).thenAnswer(invocation -> {
            JournalAudit entree = invocation.getArgument(0);
            entree.setId(sequence.incrementAndGet());
            derniere.set(entree);
            return entree;
        });
    }

    @Test
    void chaineLesEmpreintesDesEntrees() {
        service.enregistrer("CREATE", "employe", 1L, null, "{}");
        JournalAudit premiere = derniere.get();

        service.enregistrer("UPDATE", "employe", 1L, "{}", "{\"nom\":\"X\"}");
        JournalAudit seconde = derniere.get();

        assertNotNull(premiere.getHash());
        assertEquals(64, premiere.getHash().length());
        assertNull(premiere.getHashPrecedent());
        assertEquals(premiere.getHash(), seconde.getHashPrecedent());
        assertNotEquals(premiere.getHash(), seconde.getHash());
    }

    @Test
    void verifieUneChaineIntacte() {
        service.enregistrer("CREATE", "employe", 1L, null, "{}");
        JournalAudit premiere = derniere.get();
        service.enregistrer("UPDATE", "employe", 1L, "{}", "{\"nom\":\"X\"}");
        JournalAudit seconde = derniere.get();

        when(journal.findAllByOrderByIdAsc()).thenReturn(List.of(premiere, seconde));

        assertEquals(0, service.verifierChaine());
    }

    @Test
    void detecteUneAlteration() {
        service.enregistrer("CREATE", "employe", 1L, null, "{}");
        JournalAudit premiere = derniere.get();
        service.enregistrer("UPDATE", "employe", 1L, "{}", "{\"nom\":\"X\"}");
        JournalAudit seconde = derniere.get();

        premiere.setApres("{\"nom\":\"falsifie\"}");
        when(journal.findAllByOrderByIdAsc()).thenReturn(List.of(premiere, seconde));

        assertTrue(service.verifierChaine() > 0);
    }
}
