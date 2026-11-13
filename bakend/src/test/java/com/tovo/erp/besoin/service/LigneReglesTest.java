package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.TypeRemplacement;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LigneReglesTest {

    @Mock
    private ServiceRepository services;

    @Mock
    private PosteRepository postes;

    @Mock
    private EmployeService employes;

    private LigneRegles regles;
    private DemandeRecrutement demande;

    @BeforeEach
    void setUp() {
        regles = new LigneRegles(services, postes, employes,
                new DemandeProperties(null, 50, 12, new java.math.BigDecimal("0.18")));
        Service service = new Service();
        service.setId(1L);
        service.setNom("Ventes");
        lenient().when(services.findById(1L)).thenReturn(Optional.of(service));

        demande = new DemandeRecrutement();
        demande.setId(1L);
        demande.setIdUtilisateur(1L);
        demande.setMotif("Test");
        demande.setDateSouhaitee(LocalDate.now().plusMonths(1));
    }

    @Test
    void refuseUnEffectifSuperieurAuMaximum() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, creation(TypeBesoin.CREATION, 51)));
        assertEquals("EFFECTIF_INVALIDE", ex.getCode());
    }

    @Test
    void accepteUnEffectifJusquAuMaximum() {
        var ligne = regles.construire(demande, creation(TypeBesoin.CREATION, 50));
        assertEquals(50, ligne.getEffectif());
    }

    @Test
    void refuseUnRemplacementDePlusieursPostes() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, remplacement(2)));
        assertEquals("EFFECTIF_REMPLACEMENT_INVALIDE", ex.getCode());
    }

    @Test
    void accepteUnRemplacementDUnSeulPoste() {
        var ligne = regles.construire(demande, remplacement(1));
        assertEquals(1, ligne.getEffectif());
    }

    private DemandeDto.LigneCreation creation(TypeBesoin typeBesoin, int effectif) {
        return new DemandeDto.LigneCreation(1L, null, typeBesoin, null, null, null,
                null, null, "Developpeur", "Description", "Profil", effectif);
    }

    private DemandeDto.LigneCreation remplacement(int effectif) {
        return new DemandeDto.LigneCreation(1L, null, TypeBesoin.REMPLACEMENT,
                TypeRemplacement.TEMPORAIRE, null, "Remplacement temporaire",
                LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                "Developpeur", "Description", "Profil", effectif);
    }
}
