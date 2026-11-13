package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.dto.DemandeDto;
import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.Departement;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.besoin.model.TypeBesoin;
import com.tovo.erp.besoin.model.TypeRemplacement;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.UtilisateurConnecte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LigneReglesTest {

    @Mock
    private ServiceRepository services;

    @Mock
    private PosteRepository postes;

    @Mock
    private EmployeService employes;

    @Mock
    private OccupationPosteProvider occupation;

    private LigneRegles regles;
    private DemandeRecrutement demande;
    private UtilisateurConnecte auteur;

    @BeforeEach
    void setUp() {
        regles = new LigneRegles(services, postes, employes, occupation,
                new DemandeProperties(null, 50, 12, new java.math.BigDecimal("0.18"), 60, 3, 5));
        auteur = new UtilisateurConnecte(1L, "RH", "rh@tovo.mg", "RH", null, null);
        Departement departement = new Departement();
        departement.setId(1L);
        Service service = new Service();
        service.setId(1L);
        service.setNom("Ventes");
        service.setDepartement(departement);
        lenient().when(services.findById(1L)).thenReturn(Optional.of(service));

        Poste poste = new Poste();
        poste.setId(10L);
        poste.setIntitule("Developpeur");
        poste.setService(service);
        poste.setEffectifAutorise(2);
        lenient().when(postes.findById(10L)).thenReturn(Optional.of(poste));

        demande = new DemandeRecrutement();
        demande.setId(1L);
        demande.setIdUtilisateur(1L);
        demande.setMotif("Test");
        demande.setDateSouhaitee(LocalDate.now().plusMonths(1));
    }

    @Test
    void refuseUnEffectifSuperieurAuMaximum() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, auteur, creation(TypeBesoin.CREATION, 51)));
        assertEquals("EFFECTIF_INVALIDE", ex.getCode());
    }

    @Test
    void accepteUnEffectifJusquAuMaximum() {
        var ligne = regles.construire(demande, auteur, creation(TypeBesoin.CREATION, 50));
        assertEquals(50, ligne.getEffectif());
    }

    @Test
    void refuseUnRemplacementDePlusieursPostes() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, auteur, remplacement(2)));
        assertEquals("EFFECTIF_REMPLACEMENT_INVALIDE", ex.getCode());
    }

    @Test
    void accepteUnRemplacementDUnSeulPoste() {
        var ligne = regles.construire(demande, auteur, remplacement(1));
        assertEquals(1, ligne.getEffectif());
    }

    @Test
    void refuseUneDemandeDUnManagerHorsDeSonDepartement() {
        UtilisateurConnecte manager = new UtilisateurConnecte(2L, "Manager", "m@tovo.mg", "MANAGER", 2L, "Achats");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, manager, creation(TypeBesoin.CREATION, 1)));
        assertEquals("HORS_PERIMETRE", ex.getCode());
    }

    @Test
    void refuseUnEffectifSuperieurAuxPlacesDisponiblesDuPoste() {
        lenient().when(occupation.affectationsActives(eq(10L), any(), any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> regles.construire(demande, auteur, creationAvecPoste(2)));
        assertEquals("CAPACITE_POSTE_INSUFFISANTE", ex.getCode());
    }

    @Test
    void accepteUnEffectifQuiTientDansLaCapaciteDuPoste() {
        lenient().when(occupation.affectationsActives(anyLong(), any(), any())).thenReturn(0L);
        var ligne = regles.construire(demande, auteur, creationAvecPoste(2));
        assertEquals(2, ligne.getEffectif());
        assertEquals(10L, ligne.getPoste().getId());
    }

    private DemandeDto.LigneCreation creationAvecPoste(int effectif) {
        return new DemandeDto.LigneCreation(1L, 10L, TypeBesoin.CREATION, null, null, null,
                null, null, "Developpeur", "Description", "Profil", effectif);
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
