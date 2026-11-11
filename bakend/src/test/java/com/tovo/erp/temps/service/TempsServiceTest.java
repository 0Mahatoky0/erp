package com.tovo.erp.temps.service;

import com.tovo.erp.conges.service.CongeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TempsServiceTest {
    @Mock private PlanningTravailRepository planning;
    @Mock private PointageRepository pointages;
    @Mock private CongeService conges;

    private TempsService service;

    @BeforeEach
    void setUp() {
        service = new TempsService(planning, pointages, conges);
    }

    @Test
    void interditLePointagePendantUnCongeValide() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        when(conges.estEnCongeValide(7L, date)).thenReturn(true);

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.pointerEntree(new TempsDtos.PointageRequest(7L, date, null)));

        assertEquals("POINTAGE_INTERDIT_CONGE", erreur.getCode());
        verify(pointages, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void autoriseLePointageHorsCongeValide() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        when(conges.estEnCongeValide(7L, date)).thenReturn(false);
        when(pointages.findByIdEmployeAndDatePointage(7L, date)).thenReturn(Optional.empty());
        when(pointages.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        TempsDtos.PointageDto resultat = service.pointerEntree(
                new TempsDtos.PointageRequest(7L, date, null));

        assertEquals(date, resultat.date());
        verify(pointages).save(org.mockito.ArgumentMatchers.any());
    }
}
