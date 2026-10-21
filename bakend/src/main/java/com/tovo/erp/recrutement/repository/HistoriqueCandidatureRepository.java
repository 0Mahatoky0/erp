package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.HistoriqueCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoriqueCandidatureRepository extends JpaRepository<HistoriqueCandidature, Long> {
    List<HistoriqueCandidature> findByCandidatureIdCandidatureOrderByDateActionAsc(Long idCandidature);
}
