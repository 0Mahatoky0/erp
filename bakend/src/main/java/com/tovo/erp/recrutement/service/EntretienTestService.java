package com.tovo.erp.recrutement.service;

import com.tovo.erp.recrutement.model.*;
import com.tovo.erp.recrutement.repository.*;
import com.tovo.erp.recrutement.dto.EntretienDto;
import com.tovo.erp.recrutement.dto.PassageTestDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.dto.TestRecrutementDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

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
    public EntretienDto planifierEntretien(RecrutementRequests.EntretienRequest request, Long idOrganisateur) {
        Entretien entretien = new Entretien();
        entretien.setCandidature(candidatures.getReferenceById(request.idCandidature()));
        entretien.setIdUtilisateurOrganisateur(idOrganisateur);
        entretien.setTypeEntretien(request.typeEntretien());
        entretien.setDateDebut(request.dateDebut());
        entretien.setDateFin(request.dateFin());
        entretien.setLieu(request.lieu());
        entretien.setLienVisio(request.lienVisio());
        return EntretienDto.de(planifierEntretien(entretien));
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
    public List<EntretienDto> entretiensDe(Long idCandidature) {
        return entretiens.findByCandidatureIdCandidature(idCandidature).stream()
                .map(EntretienDto::de)
                .toList();
    }

    @Transactional
    public TestRecrutementDto creerTest(RecrutementRequests.TestRequest request) {
        TestRecrutement test = new TestRecrutement();
        test.setOffre(offres.getReferenceById(request.idOffre()));
        test.setTitre(request.titre());
        test.setDescription(request.description());
        test.setDureeMinutes(request.dureeMinutes());
        if (request.noteMax() != null) {
            test.setNoteMax(request.noteMax());
        }
        return TestRecrutementDto.de(creerTest(test));
    }

    @Transactional
    public TestRecrutement creerTest(TestRecrutement test) {
        if (!offres.existsById(test.getOffre().getIdOffre()))
            throw new IllegalArgumentException("Offre introuvable");
        return tests.save(test);
    }

    @Transactional
    public List<TestRecrutementDto> testsParOffre(Long idOffre) {
        return tests.findByOffreIdOffre(idOffre).stream()
                .map(TestRecrutementDto::de)
                .toList();
    }

    @Transactional
    public PassageTestDto planifierPassage(RecrutementRequests.PassageTestRequest request) {
        PassageTest passage = new PassageTest();
        passage.setTest(tests.getReferenceById(request.idTest()));
        passage.setCandidature(candidatures.getReferenceById(request.idCandidature()));
        if (request.numeroTentative() != null) {
            passage.setNumeroTentative(request.numeroTentative());
        }
        passage.setDatePrevue(request.datePrevue());
        passage.setDateDebut(request.dateDebut());
        passage.setDateFin(request.dateFin());
        passage.setCommentaire(request.commentaire());
        return PassageTestDto.de(planifierPassage(passage));
    }

    @Transactional
    public List<PassageTestDto> passagesParCandidature(Long idCandidature) {
        return passages.findByCandidatureIdCandidature(idCandidature).stream()
                .map(PassageTestDto::de)
                .toList();
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
