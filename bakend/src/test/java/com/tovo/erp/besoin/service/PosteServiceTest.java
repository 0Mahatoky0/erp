package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.dto.PosteDto;
import com.tovo.erp.besoin.model.Departement;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.besoin.repository.DemandeRecrutementPosteRepository;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosteServiceTest {

    @Mock private PosteRepository posteRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private DemandeRecrutementPosteRepository ligneRepository;
    @InjectMocks private PosteService service;

    @Test
    void calculeLesPlacesDisponiblesDansLeDetail() {
        when(posteRepository.findById(1L)).thenReturn(Optional.of(poste()));
        when(ligneRepository.countByPosteId(1L)).thenReturn(2L);
        when(posteRepository.countAffectationsActives(1L)).thenReturn(3L);
        when(ligneRepository.countPlacesEnCours(1L)).thenReturn(1L);

        PosteDto.Liste resultat = service.trouver(1L);

        assertEquals(3L, resultat.postesPourvus());
        assertEquals(1L, resultat.placesEnCoursRecrutement());
        assertEquals(1L, resultat.placesDisponibles());
        assertEquals(2L, resultat.nbPostesLies());
    }

    @Test
    void calculeLesPlacesDisponiblesDansLaListe() {
        when(posteRepository.countAffectationsActivesParPoste())
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(ligneRepository.countPlacesEnCoursParPoste())
                .thenReturn(List.<Object[]>of(new Object[]{1L, 1L}));
        when(posteRepository.rechercher(isNull(), isNull(), isNull()))
                .thenReturn(List.of(poste()));
        when(ligneRepository.countByPosteId(1L)).thenReturn(2L);

        List<PosteDto.Liste> resultat = service.lister(null, null, null);

        assertEquals(1, resultat.size());
        assertEquals(3L, resultat.get(0).postesPourvus());
        assertEquals(1L, resultat.get(0).placesEnCoursRecrutement());
        assertEquals(1L, resultat.get(0).placesDisponibles());
    }

    private Poste poste() {
        Departement departement = new Departement();
        departement.setId(10L);
        departement.setNom("Technique");
        Service service = new Service();
        service.setId(20L);
        service.setNom("Informatique");
        service.setDepartement(departement);
        Poste poste = new Poste();
        poste.setId(1L);
        poste.setService(service);
        poste.setIntitule("Developpeur");
        poste.setEffectifAutorise(5);
        poste.setActif(true);
        return poste;
    }
}
