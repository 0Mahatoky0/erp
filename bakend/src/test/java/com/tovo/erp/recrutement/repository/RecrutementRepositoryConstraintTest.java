package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class RecrutementRepositoryConstraintTest {
    @Autowired CandidatRepository candidats;

    @Test
    void appliqueLUniciteDeLeMailCandidat() {
        Candidat first = candidat("unique@example.com");
        candidats.saveAndFlush(first);

        Candidat duplicate = candidat("unique@example.com");
        assertThrows(DataIntegrityViolationException.class, () -> candidats.saveAndFlush(duplicate));
    }

    private Candidat candidat(String email) {
        Candidat candidat = new Candidat();
        candidat.setNom("Nom");
        candidat.setPrenom("Prenom");
        candidat.setEmail(email);
        return candidat;
    }
}
