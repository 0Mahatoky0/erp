package com.tovo.erp.temps.service;

import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos.AffectationRegimeRequest;
import com.tovo.erp.temps.dto.TempsDtos.HoraireRequest;
import com.tovo.erp.temps.dto.TempsDtos.RegimeRequest;
import com.tovo.erp.temps.model.AffectationRegime;
import com.tovo.erp.temps.model.HoraireTravail;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.model.RegimeTravail;
import com.tovo.erp.temps.repository.AffectationRegimeRepository;
import com.tovo.erp.temps.repository.HoraireTravailRepository;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.RegimeTravailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegimeTravailServiceTest {

    @Mock RegimeTravailRepository regimes;
    @Mock HoraireTravailRepository horaires;
    @Mock AffectationRegimeRepository affectations;
    @Mock PlanningTravailRepository plannings;
    @Mock EmployeService employes;

    private RegimeTravailService service;

    @BeforeEach
    void setUp() {
        service = new RegimeTravailService(regimes, horaires, affectations, plannings, employes);
    }

    @Test
    void creeUnRegimeEnNormalisantCodeEtJours() {
        when(regimes.findByCodeIgnoreCase("STANDARD")).thenReturn(Optional.empty());
        when(regimes.save(any())).thenAnswer(invocation -> {
            RegimeTravail regime = invocation.getArgument(0);
            regime.setId(1L);
            return regime;
        });

        var dto = service.creer(new RegimeRequest(" standard ", "Horaire standard",
                null, null, "lundi, mardi, lundi", null, null, null, null, null));

        assertEquals("STANDARD", dto.code());
        assertEquals("LUNDI,MARDI", dto.joursTravailles());
        assertEquals(40, dto.heuresHebdo());
        assertEquals(10, dto.toleranceRetardMin());
    }

    @Test
    void refuseUnCodeDejaUtilise() {
        RegimeTravail existant = new RegimeTravail();
        existant.setId(2L);
        when(regimes.findByCodeIgnoreCase("STANDARD")).thenReturn(Optional.of(existant));

        var erreur = assertThrows(BusinessException.class,
                () -> service.creer(new RegimeRequest("STANDARD", "Doublon",
                        null, null, null, null, null, null, null, null)));

        assertEquals("REGIME_CODE_EXISTANT", erreur.getCode());
        verify(regimes, never()).save(any());
    }

    @Test
    void refuseUnePlageHoraireIncoherente() {
        when(regimes.findById(1L)).thenReturn(Optional.of(new RegimeTravail()));
        var requete = new HoraireRequest("LUNDI", LocalTime.of(17, 0), LocalTime.of(8, 0),
                null, null);

        var erreur = assertThrows(BusinessException.class, () -> service.ajouterHoraire(1L, requete));

        assertEquals("HORAIRES_INVALIDES", erreur.getCode());
        verify(horaires, never()).save(any());
    }

    @Test
    void affecteLeRegimeEtGenereLePlanning() {
        RegimeTravail regime = new RegimeTravail();
        regime.setId(3L);
        regime.setCode("STANDARD");
        regime.setLibelle("Horaire standard");
        regime.setJoursTravailles("LUNDI,MARDI");
        regime.setPauseMinutes(45);
        regime.setToleranceRetardMin(8);
        when(employes.existe(7L)).thenReturn(true);
        when(regimes.findById(3L)).thenReturn(Optional.of(regime));
        when(affectations.countChevauchement(eq(7L), any(), any())).thenReturn(0L);
        when(affectations.save(any())).thenAnswer(invocation -> {
            AffectationRegime affectation = invocation.getArgument(0);
            if (affectation.getId() == null) {
                affectation.setId(11L);
            }
            return affectation;
        });
        HoraireTravail horaire = new HoraireTravail();
        horaire.setHeureDebut(LocalTime.of(8, 0));
        horaire.setHeureFin(LocalTime.of(16, 30));
        when(horaires.findByIdRegimeOrderByJourSemaineAscHeureDebutAsc(3L)).thenReturn(List.of(horaire));
        when(plannings.save(any())).thenAnswer(invocation -> {
            PlanningTravail planning = invocation.getArgument(0);
            planning.setId(99L);
            return planning;
        });

        var dto = service.affecter(7L, new AffectationRegimeRequest(
                3L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true));

        assertEquals(11L, dto.id());
        assertEquals(99L, dto.idPlanning());
        verify(plannings).save(argThat(planning ->
                planning.getPauseMinutes() == 45
                        && planning.getToleranceMinutes() == 8
                        && planning.getHeureDebut().equals(LocalTime.of(8, 0))
                        && planning.getHeureFin().equals(LocalTime.of(16, 30))));
    }

    @Test
    void refuseUneAffectationQuiChevaucheUneAutre() {
        when(employes.existe(7L)).thenReturn(true);
        when(affectations.countChevauchement(eq(7L), any(), any())).thenReturn(1L);

        var erreur = assertThrows(BusinessException.class,
                () -> service.affecter(7L, new AffectationRegimeRequest(
                        3L, LocalDate.of(2026, 1, 1), null, null)));

        assertEquals("REGIME_CHEVAUCHEMENT", erreur.getCode());
        verify(affectations, never()).save(any());
    }
}
