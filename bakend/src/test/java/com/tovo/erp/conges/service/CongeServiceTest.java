package com.tovo.erp.conges.service;

import com.tovo.erp.conges.dto.CongeDtos;
import com.tovo.erp.conges.model.*;
import com.tovo.erp.conges.repository.*;
import com.tovo.erp.employe.dto.EmployeDto;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.UtilisateurConnecte;
import com.tovo.erp.temps.service.CalendrierTempsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CongeServiceTest {
    @Mock TypeCongeRepository types;
    @Mock SoldeCongeRepository soldes;
    @Mock DroitCongeRepository droits;
    @Mock DemandeCongeRepository demandes;
    @Mock AbsenceRepository absences;
    @Mock MouvementCongeRepository mouvements;
    @Mock EmployeService employes;
    @Mock CalendrierTempsService calendrier;
    @Mock ParametreCongeService parametres;
    @Mock PeriodeBloqueeRepository periodesBloquees;
    @Mock SuppleanceService suppleances;

    private CongeService service;
    private TypeConge annuel;
    private UtilisateurConnecte manager;
    private UtilisateurConnecte rh;
    private UtilisateurConnecte direction;

    @BeforeEach
    void setUp() {
        service = new CongeService(types, soldes, demandes, absences, droits,
                mouvements, employes, calendrier, parametres, periodesBloquees, suppleances);
        annuel = new TypeConge();
        annuel.setId(1L);
        annuel.setCode("ANNUEL");
        annuel.setLibelle("Conge annuel");
        annuel.setJoursAcquisMois(new BigDecimal("2.50"));
        annuel.setDecompteSolde(true);
        manager = new UtilisateurConnecte(10L, "Manager", "m@x.mg", "MANAGER", 1L, "Service");
        rh = new UtilisateurConnecte(11L, "RH", "rh@x.mg", "RH", 1L, "RH");
        direction = new UtilisateurConnecte(12L, "Direction", "d@x.mg", "DIRECTION", 1L, "Direction");

        lenient().when(parametres.texte(anyString(), anyString())).thenReturn("OUVRABLES");
        lenient().when(parametres.decimal(eq("PLAFOND_SOLDE"), any())).thenReturn(new BigDecimal("90"));
        lenient().when(parametres.decimal(eq("ALERTE_SOLDE"), any())).thenReturn(new BigDecimal("72"));
        lenient().when(parametres.entier(eq("PREVENANCE_DUREE_JOURS"), anyInt())).thenReturn(3);
        lenient().when(parametres.entier(eq("PREVENANCE_JOURS"), anyInt())).thenReturn(7);
        lenient().when(parametres.entier(eq("VALIDATION_DIRECTION_JOURS"), anyInt())).thenReturn(15);
    }

    @Test
    void decompteLesJoursOuvrablesLundiSamediEtExclutLesFeries() {
        LocalDate debut = LocalDate.of(2026, 12, 22);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(employes.existe(7L)).thenReturn(true);
        when(types.findById(1L)).thenReturn(Optional.of(annuel));
        when(calendrier.estFerie(any(LocalDate.class))).thenAnswer(i ->
                i.getArgument(0).equals(LocalDate.of(2026, 12, 25)));
        when(demandes.existeChevauchement(eq(7L), eq(debut), eq(fin), anyList())).thenReturn(false);
        when(absences.existeChevauchement(eq(7L), eq(debut), eq(fin), anyList())).thenReturn(false);

        SoldeConge solde = solde(7L, 2026, "10.00", "0.00", "0.00");
        when(soldes.findByIdEmployeAndIdTypeCongeAndAnnee(7L, 1L, 2026)).thenReturn(Optional.of(solde));
        when(soldes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(demandes.save(any())).thenAnswer(i -> {
            DemandeConge d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        CongeDtos.DemandeDto dto = service.creerDemande(new CongeDtos.DemandeRequest(
                7L, 1L, debut, fin, null, null, "Conges de fin d'annee", null, null));

        assertEquals(0, new BigDecimal("8.00").compareTo(dto.nombreJours()));
        assertEquals(StatutDemandeConge.SOUMISE, dto.statut());
        assertEquals(0, new BigDecimal("8.00").compareTo(solde.getJoursReserves()));
        verify(mouvements).save(argThat(m -> m.getTypeMouvement() == TypeMouvementConge.RESERVATION));
    }

    @Test
    void refuseUneDemandeQuiChevaucheUneAbsence() {
        when(employes.existe(7L)).thenReturn(true);
        when(types.findById(1L)).thenReturn(Optional.of(annuel));
        when(calendrier.estFerie(any(LocalDate.class))).thenReturn(false);
        LocalDate debut = LocalDate.of(2026, 12, 7);
        when(demandes.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(false);
        when(absences.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(true);

        var erreur = assertThrows(com.tovo.erp.shared.BusinessException.class,
                () -> service.creerDemande(new CongeDtos.DemandeRequest(
                        7L, 1L, debut, debut, null, null, "Test", null, null)));

        assertEquals("CHEVAUCHEMENT", erreur.getCode());
        verify(demandes, never()).save(any());
    }

    @Test
    void refuseUnePeriodeBloqueeParLaRh() {
        LocalDate debut = LocalDate.of(2026, 12, 7);
        when(employes.existe(7L)).thenReturn(true);
        when(types.findById(1L)).thenReturn(Optional.of(annuel));
        when(calendrier.estFerie(any(LocalDate.class))).thenReturn(false);
        when(demandes.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(false);
        when(absences.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(false);
        PeriodeBloquee bloque = new PeriodeBloquee();
        bloque.setLibelle("Cloture annuelle");
        bloque.setDateDebut(debut);
        bloque.setDateFin(debut);
        when(periodesBloquees.chevauchant(debut, debut)).thenReturn(List.of(bloque));

        var erreur = assertThrows(com.tovo.erp.shared.BusinessException.class,
                () -> service.creerDemande(new CongeDtos.DemandeRequest(
                        7L, 1L, debut, debut, null, null, "Test", null, null)));

        assertEquals("PERIODE_BLOQUEE", erreur.getCode());
        verify(demandes, never()).save(any());
    }

    @Test
    void refuseSiLEffectifMinimumNestPasRespecteSansRemplacant() {
        LocalDate debut = LocalDate.of(2026, 12, 7);
        when(employes.existe(7L)).thenReturn(true);
        when(types.findById(1L)).thenReturn(Optional.of(annuel));
        when(calendrier.estFerie(any(LocalDate.class))).thenReturn(false);
        when(demandes.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(false);
        when(absences.existeChevauchement(eq(7L), eq(debut), eq(debut), anyList())).thenReturn(false);
        when(employes.serviceDe(7L)).thenReturn(Optional.of(5L));
        when(employes.actifsDuService(5L)).thenReturn(List.of(
                new EmployeDto.Actif(7L, "Doe", "Jane", 5L, debut),
                new EmployeDto.Actif(8L, "Doe", "John", 5L, debut)));
        when(parametres.entier(eq("EFFECTIF_MIN_POURCENT"), anyInt())).thenReturn(60);
        when(demandes.findByIdEmployeInAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                anyList(), anyList(), any(), any())).thenReturn(List.of(demandeEnAttente(99L, 1)));

        var erreur = assertThrows(com.tovo.erp.shared.BusinessException.class,
                () -> service.creerDemande(new CongeDtos.DemandeRequest(
                        7L, 1L, debut, debut, null, null, "Test", null, null)));

        assertEquals("EFFECTIF_MINIMUM", erreur.getCode());
        verify(demandes, never()).save(any());
    }

    @Test
    void workflowManagerPuisRhPuisDirectionAuDelaDeQuinzeJours() {
        DemandeConge demande = demandeEnAttente(1L, 20.0);
        when(demandes.findById(1L)).thenReturn(Optional.of(demande));
        when(types.findById(1L)).thenReturn(Optional.of(annuel));
        when(demandes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(demandes.managerAffecteAEmploye(eq(10L), eq(7L), any(), any())).thenReturn(true);

        service.decision(1L, null, true, manager);
        assertEquals(StatutDemandeConge.VALIDEE_MANAGER, demande.getStatut());

        service.decision(1L, null, true, rh);
        assertEquals(StatutDemandeConge.VALIDEE_RH, demande.getStatut());

        SoldeConge lot2025 = solde(7L, 2025, "10.00", "0.00", "0.00");
        lot2025.setIdDroitConge(101L);
        SoldeConge lot2026 = solde(7L, 2026, "20.00", "0.00", "20.00");
        lot2026.setIdDroitConge(102L);
        when(soldes.findByIdEmployeAndIdTypeCongeAndAnnee(7L, 1L, 2026)).thenReturn(Optional.of(lot2026));
        when(soldes.findByIdEmployeAndIdTypeCongeOrderByAnneeAsc(7L, 1L))
                .thenReturn(List.of(lot2025, lot2026));

        service.decision(1L, null, true, direction);

        assertEquals(StatutDemandeConge.APPROUVEE, demande.getStatut());
        assertEquals(0, new BigDecimal("10.00").compareTo(lot2025.getJoursPris()));
        assertEquals(0, new BigDecimal("10.00").compareTo(lot2026.getJoursPris()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lot2026.getJoursReserves()));
        verify(mouvements, times(2)).save(argThat(
                m -> m.getTypeMouvement() == TypeMouvementConge.CONSOMMATION));
    }

    @Test
    void acquisitionMensuelleEstEcreteeAuPlafondDeQuatreVingtDixJours() {
        when(employes.actifs()).thenReturn(List.of(new EmployeDto.Actif(7L, "Doe", "Jane", null, null)));
        when(types.findByActifTrueOrderByLibelleAsc()).thenReturn(List.of(annuel));
        when(mouvements.existsByIdEmployeAndIdTypeCongeAndTypeMouvementAndDateEffet(
                7L, 1L, TypeMouvementConge.ACQUISITION, LocalDate.of(2026, 12, 1))).thenReturn(false);
        SoldeConge solde = solde(7L, 2026, "89.00", "0.00", "0.00");
        when(soldes.findByIdEmployeAndIdTypeCongeOrderByAnneeAsc(7L, 1L)).thenReturn(List.of(solde));
        when(soldes.findByIdEmployeAndIdTypeCongeAndAnnee(7L, 1L, 2026)).thenReturn(Optional.of(solde));
        when(soldes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mouvements.save(any())).thenAnswer(i -> i.getArgument(0));

        int credits = service.acquerirMois(2026, 12);

        assertEquals(1, credits);
        assertEquals(0, new BigDecimal("90.00").compareTo(solde.getJoursAcquis()));
        verify(mouvements).save(argThat(m ->
                m.getTypeMouvement() == TypeMouvementConge.ACQUISITION
                        && new BigDecimal("1.00").compareTo(m.getJours()) == 0));
    }

    private DemandeConge demandeEnAttente(Long id, double jours) {
        DemandeConge d = new DemandeConge();
        d.setId(id);
        d.setIdEmploye(7L);
        d.setIdTypeConge(1L);
        d.setDateDebut(LocalDate.now().plusDays(20));
        d.setDateFin(d.getDateDebut().plusDays((long) jours));
        d.setNombreJours(BigDecimal.valueOf(jours));
        d.setMotif("Test");
        d.setStatut(StatutDemandeConge.SOUMISE);
        return d;
    }

    private SoldeConge solde(Long employe, int annee, String acquis, String pris, String reserves) {
        SoldeConge s = new SoldeConge();
        s.setId((long) annee);
        s.setIdEmploye(employe);
        s.setIdTypeConge(1L);
        s.setAnnee(annee);
        s.setJoursAcquis(new BigDecimal(acquis));
        s.setJoursPris(new BigDecimal(pris));
        s.setJoursReserves(new BigDecimal(reserves));
        return s;
    }
}
