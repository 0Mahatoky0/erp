package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.HistoriqueCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface HistoriqueCandidatureRepository extends JpaRepository<HistoriqueCandidature, Long> {
    @EntityGraph(attributePaths = "candidature")
    List<HistoriqueCandidature> findByCandidatureIdCandidatureOrderByDateActionAsc(Long idCandidature);
}
