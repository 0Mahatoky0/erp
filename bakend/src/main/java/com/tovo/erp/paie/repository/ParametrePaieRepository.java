package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.ParametrePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametrePaieRepository extends JpaRepository<ParametrePaie, Long> {
    Optional<ParametrePaie> findFirstByActifTrueOrderByDateModificationDesc();
}
