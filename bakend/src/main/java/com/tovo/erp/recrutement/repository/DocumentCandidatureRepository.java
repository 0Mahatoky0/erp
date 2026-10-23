package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.DocumentCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface DocumentCandidatureRepository extends JpaRepository<DocumentCandidature, Long> {
    @EntityGraph(attributePaths = "candidature")
    List<DocumentCandidature> findByCandidatureIdCandidature(Long idCandidature);
}
