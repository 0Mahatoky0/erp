package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PassageTestRepository extends JpaRepository<PassageTest, Long> {
    List<PassageTest> findByCandidatureIdCandidature(Long idCandidature);
    List<PassageTest> findByStatut(TestStatut statut);
}
