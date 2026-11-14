package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.model.PointageBrut;
import com.tovo.erp.temps.repository.PointageBrutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointageBrutServiceTest {

    @Mock PointageBrutRepository pointages;
    private PointageBrutService service;
    private final AtomicLong ids = new AtomicLong();

    @BeforeEach
    void setUp() {
        service = new PointageBrutService(pointages);
        lenient().when(pointages.save(any())).thenAnswer(i -> {
            PointageBrut pointage = i.getArgument(0);
            if (pointage.getId() == null) {
                pointage.setId(ids.incrementAndGet());
            }
            return pointage;
        });
    }

    @Test
    void exigeUnMotifPourUneCorrectionManuelle() {
        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.enregistrer(new TempsDtos.PointageBrutRequest(7L, LocalDateTime.now(),
                        "ENTREE", "MANUEL", null, false, null, null, null, null, null), 1L));

        assertEquals("CORRECTION_NON_JUSTIFIEE", erreur.getCode());
    }

    @Test
    void exigeUnTerminalAuthentifiePourUnBadge() {
        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.enregistrer(new TempsDtos.PointageBrutRequest(7L, LocalDateTime.now(),
                        "ENTREE", "BADGE", "TERM-1", false, null, null, null, null, null), null));

        assertEquals("TERMINAL_NON_AUTHENTIFIE", erreur.getCode());
    }

    @Test
    void ignoreUnDoublonDeMoinsDeDeuxMinutes() {
        LocalDate jour = LocalDate.of(2026, 6, 8);
        PointageBrut entree = evenement(7L, jour.atTime(8, 0), PointageBrut.Sens.ENTREE);
        PointageBrut doublon = evenement(7L, jour.atTime(8, 1), PointageBrut.Sens.ENTREE);
        PointageBrut sortie = evenement(7L, jour.atTime(17, 0), PointageBrut.Sens.SORTIE);
        when(pointages.findByIdEmployeAndHorodatageBetweenOrderByHorodatageAsc(anyLong(), any(), any()))
                .thenReturn(List.of(entree, doublon, sortie));

        TempsDtos.PointageBrutJourDto synthese = service.syntheseJour(7L, jour);

        assertEquals(1, synthese.paires().size());
        assertEquals(540, synthese.minutesTravaillees());
        assertTrue(synthese.anomalies().contains("DOUBLON_IGNORE"));
    }

    @Test
    void rattacheUnPosteDeNuitAuJourDeLEntree() {
        LocalDate jour = LocalDate.of(2026, 6, 8);
        PointageBrut entree = evenement(7L, jour.atTime(22, 0), PointageBrut.Sens.ENTREE);
        PointageBrut sortie = evenement(7L, jour.plusDays(1).atTime(6, 0), PointageBrut.Sens.SORTIE);
        when(pointages.findByIdEmployeAndHorodatageBetweenOrderByHorodatageAsc(anyLong(), any(), any()))
                .thenReturn(List.of(entree, sortie));

        TempsDtos.PointageBrutJourDto synthese = service.syntheseJour(7L, jour);

        assertEquals(1, synthese.paires().size());
        assertEquals(480, synthese.minutesTravaillees());
        assertEquals(jour, synthese.paires().getFirst().entree().toLocalDate());
    }

    private PointageBrut evenement(Long idEmploye, LocalDateTime horodatage, PointageBrut.Sens sens) {
        PointageBrut pointage = new PointageBrut();
        pointage.setId(ids.incrementAndGet());
        pointage.setIdEmploye(idEmploye);
        pointage.setHorodatage(horodatage);
        pointage.setSens(sens);
        pointage.setSource(PointageBrut.Source.BADGE);
        pointage.setTerminalId("TERM-1");
        pointage.setTerminalAuthentifie(true);
        pointage.setLieuId(1L);
        return pointage;
    }
}
