package com.tovo.erp.demande.repository;

import com.tovo.erp.demande.domain.Validation;
import com.tovo.erp.demande.domain.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

    List<Validation> findByDemandePosteIdOrderByDateActionAsc(Long idDemandePoste);

    List<Validation> findByDemandePosteIdAndEpisodeOrderByDateActionAsc(Long idDemandePoste, Integer episode);

    @Query("select distinct v.demandePoste.id from Validation v where v.decision = :decision")
    List<Long> findDemandePosteIdsByDecision(Decision decision);
}
