package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.model.AnomaliePointage;
import com.tovo.erp.temps.model.HeureSupplementaire;
import com.tovo.erp.temps.model.JourTravaille;
import com.tovo.erp.temps.model.PlanningTravail;
import com.tovo.erp.temps.model.Pointage;
import com.tovo.erp.temps.repository.AnomaliePointageRepository;
import com.tovo.erp.temps.repository.HeureSupplementaireRepository;
import com.tovo.erp.temps.repository.JourTravailleRepository;
import com.tovo.erp.temps.repository.PlanningTravailRepository;
import com.tovo.erp.temps.repository.PeriodeTempsCloseRepository;
import com.tovo.erp.temps.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempsServiceTest {

    @Mock PlanningTravailRepository plannings;
    @Mock PointageRepository pointages;
    @Mock JourTravailleRepository jours;
    @Mock AnomaliePointageRepository anomalies;
    @Mock HeureSupplementaireRepository heuresSup;
    @Mock IndisponibiliteProvider indisponibilites;
    @Mock CalendrierTempsService calendrier;
    @Mock PeriodeTempsCloseRepository periodesCloses;

    private TempsService service;
    private PlanningTravail planning;
    private final Map<LocalDate, Pointage> pointagesParDate = new HashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @BeforeEach
    void setUp() {
        service = new TempsService(plannings, pointages, jours, anomalies, heuresSup,
                indisponibilites, calendrier, periodesCloses);
        planning = new PlanningTravail();
        planning.setId(1L);
        planning.setIdEmploye(7L);
        planning.setJoursTravailles("LUN-VEN");
        planning.setHeureDebut(LocalTime.of(8, 0));
        planning.setHeureFin(LocalTime.of(17, 0));
        planning.setPauseMinutes(60);
        planning.setToleranceMinutes(5);
        planning.setDateDebut(LocalDate.of(2026, 1, 1));

        lenient().when(plannings.findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinIsNullOrderByDateDebutDesc(
                eq(7L), any(LocalDate.class))).thenReturn(Optional.of(planning));
        lenient().when(plannings.findFirstByIdEmployeAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(
                eq(7L), any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.empty());
        lenient().when(pointages.findByIdEmployeAndDatePointage(eq(7L), any(LocalDate.class)))
                .thenAnswer(i -> Optional.ofNullable(pointagesParDate.get(i.getArgument(1))));
        lenient().when(jours.findByIdEmployeAndDateJour(eq(7L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        lenient().when(jours.save(any())).thenAnswer(i -> {
            JourTravaille jour = i.getArgument(0);
            if (jour.getId() == null) {
                jour.setId(ids.incrementAndGet());
            }
            return jour;
        });
        lenient().when(anomalies.save(any())).thenAnswer(i -> {
            AnomaliePointage anomalie = i.getArgument(0);
            if (anomalie.getId() == null) {
                anomalie.setId(ids.incrementAndGet());
            }
            return anomalie;
        });
        lenient().when(heuresSup.findByIdEmployeAndSemaineDebut(eq(7L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        lenient().when(heuresSup.save(any())).thenAnswer(i -> {
            HeureSupplementaire heureSup = i.getArgument(0);
            if (heureSup.getId() == null) {
                heureSup.setId(ids.incrementAndGet());
            }
            return heureSup;
        });
        lenient().when(indisponibilites.estIndisponible(eq(7L), any(LocalDate.class))).thenReturn(false);
        lenient().when(calendrier.estFerie(any(LocalDate.class))).thenReturn(false);
        lenient().when(periodesCloses.couvrant(any(LocalDate.class))).thenReturn(Optional.empty());
        lenient().when(periodesCloses.findAllByOrderByAnneeDescMoisDesc()).thenReturn(java.util.List.of());
        lenient().when(periodesCloses.save(any())).thenAnswer(i -> {
            com.tovo.erp.temps.model.PeriodeTempsClose periode = i.getArgument(0);
            periode.setId(ids.incrementAndGet());
            return periode;
        });
    }

    @Test
    void clotureLaPeriodeMensuelleEtRefuseUneNouvelleCloture() {
        TempsDtos.PeriodeCloseDto periode = service.cloturer(new TempsDtos.ClotureRequest(2026, 5), 1L);
        assertEquals(2026, periode.annee());
        assertEquals(5, periode.mois());
        assertNotNull(periode.id());
        when(periodesCloses.findByAnneeAndMois(2026, 5)).thenReturn(Optional.of(new com.tovo.erp.temps.model.PeriodeTempsClose()));
        var erreur = assertThrows(BusinessException.class,
                () -> service.cloturer(new TempsDtos.ClotureRequest(2026, 5), 1L));
        assertEquals("PERIODE_DEJA_CLOTUREE", erreur.getCode());
    }

    @Test
    void refuseLaClotureDUnePeriodeNonTerminee() {
        java.time.YearMonth futur = java.time.YearMonth.now().plusMonths(1);
        var erreur = assertThrows(BusinessException.class,
                () -> service.cloturer(new TempsDtos.ClotureRequest(futur.getYear(), futur.getMonthValue()), 1L));
        assertEquals("PERIODE_NON_TERMINEE", erreur.getCode());
    }

    @Test
    void refuseUneClotureAvecAnomalieOuverte() {
        when(anomalies.existsByDateAnomalieBetweenAndStatut(eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)), eq(AnomaliePointage.Statut.OUVERTE))).thenReturn(true);
        var erreur = assertThrows(BusinessException.class,
                () -> service.cloturer(new TempsDtos.ClotureRequest(2026, 5), 1L));
        assertEquals("ANOMALIE_OUVERTE", erreur.getCode());
    }

    @Test
    void refuseLePointageSurUnePeriodeCloturee() {
        com.tovo.erp.temps.model.PeriodeTempsClose periode = new com.tovo.erp.temps.model.PeriodeTempsClose();
        periode.setAnnee(2026);
        periode.setMois(5);
        LocalDate jour = LocalDate.of(2026, 5, 12);
        when(periodesCloses.couvrant(jour)).thenReturn(Optional.of(periode));
        var erreur = assertThrows(BusinessException.class,
                () -> service.pointerEntree(new TempsDtos.PointageRequest(7L, jour,
                        LocalTime.of(8, 0))));
        assertEquals("PERIODE_CLOTUREE", erreur.getCode());
    }

    @Test
    void calculeLaSyntheseJournaliereAvecPauseForfaitaire() {
        LocalDate jour = LocalDate.of(2026, 6, 8);
        pointagesParDate.put(jour, pointage(jour, LocalTime.of(8, 0), LocalTime.of(17, 35)));

        TempsDtos.JourTravailleDto dto = service.calculerJour(7L, jour);

        assertEquals(575, dto.minutesPresence());
        assertEquals(60, dto.minutesPause());
        assertEquals(515, dto.minutesTravaillees());
        assertEquals(480, dto.minutesTheoriques());
        assertEquals(0, dto.minutesRetard());
        assertEquals(35, dto.minutesHs());
        assertEquals("A_VALIDER", dto.statut());
        assertTrue(dto.anomalies().isEmpty());
    }

    @Test
    void detecteLeRetardAuDelaDeLaTolerance() {
        LocalDate jour = LocalDate.of(2026, 6, 9);
        pointagesParDate.put(jour, pointage(jour, LocalTime.of(8, 12), LocalTime.of(17, 0)));

        TempsDtos.JourTravailleDto dto = service.calculerJour(7L, jour);

        assertEquals(12, dto.minutesRetard());
        assertEquals("ANOMALIE", dto.statut());
        assertEquals("RETARD_NON_JUSTIFIE", dto.anomalies().getFirst().type());
    }

    @Test
    void calculeLesHeuresSupplementairesParSemaineCivile() {
        LocalDate lundi = LocalDate.of(2026, 6, 8);
        for (int i = 0; i < 5; i++) {
            LocalDate jour = lundi.plusDays(i);
            pointagesParDate.put(jour, pointage(jour, LocalTime.of(8, 0), LocalTime.of(19, 24)));
        }

        TempsDtos.HeureSupplementaireDto dto = service.calculerSemaine(7L, lundi);

        assertEquals(3120, dto.minutesTotal());
        assertEquals(480, dto.minutesHs30());
        assertEquals(240, dto.minutesHs50());
        assertFalse(dto.alertePlafond());
        assertEquals("DETECTEE", dto.statut());
    }

    @Test
    void ignoreLesJoursNonTravaillesDuPlanning() {
        LocalDate samedi = LocalDate.of(2026, 6, 13);

        assertNull(service.calculerJour(7L, samedi));
    }

    @Test
    void refuseLaValidationDUneJourneeAvecAnomalieOuverte() {
        LocalDate jour = LocalDate.of(2026, 6, 10);
        pointagesParDate.put(jour, pointage(jour, LocalTime.of(8, 0), null));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.validerJour(7L, jour));

        assertEquals("ANOMALIE_OUVERTE", erreur.getCode());
    }

    private Pointage pointage(LocalDate date, LocalTime entree, LocalTime sortie) {
        Pointage pointage = new Pointage();
        pointage.setId(ids.incrementAndGet());
        pointage.setIdEmploye(7L);
        pointage.setDatePointage(date);
        pointage.setHeureEntree(entree);
        pointage.setHeureSortie(sortie);
        return pointage;
    }
}
