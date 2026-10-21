package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.domain.*;
import com.tovo.erp.recrutement.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class EntretienTestService {
    private final EntretienRepository entretiens;
    private final CandidatureRepository candidatures;
    private final TestRecrutementRepository tests;
    private final PassageTestRepository passages;
    private final OffreEmploiRepository offres;

    public EntretienTestService(EntretienRepository entretiens, CandidatureRepository candidatures,
                                TestRecrutementRepository tests, PassageTestRepository passages,
                                OffreEmploiRepository offres) {
        this.entretiens = entretiens; this.candidatures = candidatures;
        this.tests = tests; this.passages = passages; this.offres = offres;
    }

    @Transactional
    public Entretien planifierEntretien(Entretien entretien) {
        verifierDates(entretien.getDateDebut(), entretien.getDateFin());
        if (!candidatures.existsById(entretien.getCandidature().getIdCandidature()))
            throw new IllegalArgumentException("Candidature introuvable");
        entretien.setStatut(EntretienStatut.PLANIFIE);
        return entretiens.save(entretien);
    }

    @Transactional
    public TestRecrutement creerTest(TestRecrutement test) {
        if (!offres.existsById(test.getOffre().getIdOffre()))
            throw new IllegalArgumentException("Offre introuvable");
        return tests.save(test);
    }

    @Transactional
    public PassageTest planifierPassage(PassageTest passage) {
        if (!tests.existsById(passage.getTest().getIdTest()) ||
                !candidatures.existsById(passage.getCandidature().getIdCandidature()))
            throw new IllegalArgumentException("Test ou candidature introuvable");
        passage.setStatut(TestStatut.PLANIFIE);
        return passages.save(passage);
    }

    private void verifierDates(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !fin.isAfter(debut))
            throw new IllegalArgumentException("La fin doit être postérieure au début");
    }
}
