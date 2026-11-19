package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.LigneSoldeRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneSoldeRuptureRepository extends JpaRepository<LigneSoldeRupture, Long> {

    List<LigneSoldeRupture> findByIdSoldeOrderByIdAsc(Long idSolde);

    void deleteByIdSolde(Long idSolde);
}
