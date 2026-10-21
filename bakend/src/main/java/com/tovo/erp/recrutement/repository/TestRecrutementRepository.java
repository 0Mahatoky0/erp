package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.TestRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRecrutementRepository extends JpaRepository<TestRecrutement, Long> {
    List<TestRecrutement> findByOffreIdOffre(Long idOffre);
}
