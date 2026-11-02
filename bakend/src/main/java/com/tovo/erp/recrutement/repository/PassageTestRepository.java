package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface PassageTestRepository extends JpaRepository<PassageTest, Long> {
    @EntityGraph(attributePaths = {"test", "candidature"})
    List<PassageTest> findByCandidatureIdCandidature(Long idCandidature);
    List<PassageTest> findByStatut(TestStatut statut);
}
