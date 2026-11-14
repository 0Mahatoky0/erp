package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.contrat.dto.CarriereDtos;
import com.tovo.erp.contrat.model.EvenementCarriere;
import com.tovo.erp.contrat.model.SalaireHistorique;
import com.tovo.erp.contrat.repository.EvenementCarriereRepository;
import com.tovo.erp.contrat.repository.SalaireHistoriqueRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.employe.service.EmployeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarriereServiceTest {

    @Mock private SalaireHistoriqueRepository salaires;
    @Mock private EvenementCarriereRepository evenements;
    @Mock private ContratRepository contrats;
    @Mock private EmployeService employes;

    private CarriereService service;

    @BeforeEach
    void setUp() {
        service = new CarriereService(salaires, evenements, contrats, employes);
    }

    @Test
    void fermeLeSalaireCourantEtTraceLEvolution() {
        when(employes.existe(3L)).thenReturn(true);
        when(contrats.findByIdEmployeOrderByDateDebutDesc(3L)).thenReturn(List.of());
        SalaireHistorique courant = new SalaireHistorique();
        courant.setId(1L);
        courant.setIdEmploye(3L);
        courant.setMontantBrut(new BigDecimal("800000"));
        courant.setDateEffet(LocalDate.of(2026, 1, 1));
        when(salaires.findFirstByIdEmployeOrderByDateEffetDesc(3L))
                .thenReturn(Optional.of(courant));
        when(salaires.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evenements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarriereDtos.SalaireDto resultat = service.enregistrerSalaire(3L,
                new CarriereDtos.SalaireRequest(new BigDecimal("900000"), "MGA",
                        LocalDate.of(2026, 4, 1), "Augmentation annuelle", null), 9L);

        assertEquals(new BigDecimal("900000"), resultat.montantBrut());
        assertEquals(LocalDate.of(2026, 3, 31), courant.getDateFin());
        verify(salaires).save(courant);
        verify(evenements).save(any(EvenementCarriere.class));
    }

    @Test
    void initialiseLHistoriqueDepuisLesContrats() {
        Contrat contrat = new Contrat();
        contrat.setId(5L);
        contrat.setNumero("CT-2026-00001");
        contrat.setDateDebut(LocalDate.of(2026, 1, 1));
        contrat.setSalaireBase(new BigDecimal("700000"));
        contrat.setDevise("MGA");
        when(contrats.findByIdEmployeOrderByDateDebutDesc(3L)).thenReturn(List.of(contrat));
        when(salaires.existsByIdContrat(5L)).thenReturn(false);
        when(evenements.existsByIdEmployeAndType(any(), any())).thenReturn(false);
        when(salaires.findByIdEmployeOrderByDateEffetDesc(3L)).thenReturn(List.of());
        when(salaires.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evenements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.salaires(3L);

        verify(salaires).save(any(SalaireHistorique.class));
        verify(evenements).save(any(EvenementCarriere.class));
    }
}
