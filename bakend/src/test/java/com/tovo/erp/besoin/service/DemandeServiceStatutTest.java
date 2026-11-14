package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.DemandeRecrutementPoste;
import com.tovo.erp.besoin.model.StatutDemande;
import com.tovo.erp.besoin.model.StatutLigne;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.besoin.repository.DemandeRecrutementRepository;
import com.tovo.erp.besoin.repository.HistoriqueStatutRepository;
import com.tovo.erp.besoin.repository.ValidationRepository;
import com.tovo.erp.besoin.repository.VerificationBudgetRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.iam.service.UtilisateurService;
import com.tovo.erp.shared.reference.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeServiceStatutTest {

    @Mock private DemandeRecrutementRepository demandes;
    @Mock private DemandeRecrutementPosteRepository lignes;
    @Mock private ValidationRepository validations;
    @Mock private VerificationBudgetRepository verifications;
    @Mock private HistoriqueStatutRepository historiques;
    @Mock private LigneRegles regles;
    @Mock private StatutService statutService;
    @Mock private CircuitValidationProperties circuit;
    @Mock private UtilisateurService utilisateurs;
    @Mock private EmployeService employes;
    @Mock private ReferenceService references;
    @Mock private OffrePublieeProvider offres;

    private DemandeService service;
    private DemandeProperties proprietes;

    @BeforeEach
    void setUp() {
        proprietes = new DemandeProperties(null, 50, 12, new BigDecimal("0.18"), 60, 3, 5);
        service = new DemandeService(demandes, lignes, validations, verifications, historiques,
                regles, statutService, circuit, proprietes, utilisateurs, employes, references, offres);
    }

    @Test
    void expireUneDemandeFinaliseeSansOffrePubliee() {
        DemandeRecrutement demande = demande(StatutDemande.FINALISEE);
        DemandeRecrutementPoste ligne = ligne(demande, StatutLigne.ACCEPTEE);
        when(demandes.findByStatutInAndDateSoumissionBefore(any(), any())).thenReturn(List.of(demande));
        when(lignes.findByDemandeIdOrderById(1L)).thenReturn(List.of(ligne));
        when(offres.existeOffrePubliee(11L)).thenReturn(false);

        int expirees = service.expirerDemandes();

        assertEquals(1, expirees);
        assertEquals(StatutDemande.EXPIREE, demande.getStatut());
        verify(historiques).save(any());
    }

    @Test
    void nexpirePasUneDemandeAyantUneOffrePubliee() {
        DemandeRecrutement demande = demande(StatutDemande.FINALISEE);
        DemandeRecrutementPoste ligne = ligne(demande, StatutLigne.ACCEPTEE);
        when(demandes.findByStatutInAndDateSoumissionBefore(any(), any())).thenReturn(List.of(demande));
        when(lignes.findByDemandeIdOrderById(1L)).thenReturn(List.of(ligne));
        when(offres.existeOffrePubliee(11L)).thenReturn(true);

        assertEquals(0, service.expirerDemandes());
        assertEquals(StatutDemande.FINALISEE, demande.getStatut());
    }

    @Test
    void clotureLaDemandeQuandTousLesPostesAcceptesSontPourvus() {
        DemandeRecrutement demande = demande(StatutDemande.FINALISEE);
        DemandeRecrutementPoste ligne = ligne(demande, StatutLigne.ACCEPTEE);
        ligne.setEffectif(1);
        ligne.setNombrePourvus(0);
        when(lignes.findById(11L)).thenReturn(Optional.of(ligne));
        lenient().when(lignes.findByDemandeIdOrderById(1L)).thenReturn(List.of(ligne));

        int pourvus = service.enregistrerEmbauche(11L, 3L);

        assertEquals(1, pourvus);
        assertEquals(StatutDemande.CLOTUREE, demande.getStatut());
        verify(historiques).save(any());
    }

    private DemandeRecrutement demande(StatutDemande statut) {
        DemandeRecrutement demande = new DemandeRecrutement();
        demande.setId(1L);
        demande.setIdUtilisateur(2L);
        demande.setStatut(statut);
        demande.setDateSoumission(LocalDateTime.now().minusDays(90));
        demande.setDateSouhaitee(LocalDate.now().plusMonths(1));
        return demande;
    }

    private DemandeRecrutementPoste ligne(DemandeRecrutement demande, StatutLigne statut) {
        DemandeRecrutementPoste ligne = new DemandeRecrutementPoste();
        ligne.setId(11L);
        ligne.setDemande(demande);
        ligne.setStatut(statut);
        ligne.setEffectif(1);
        return ligne;
    }
}
