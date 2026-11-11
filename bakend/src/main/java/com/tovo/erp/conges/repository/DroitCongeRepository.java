package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.DroitConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DroitCongeRepository extends JpaRepository<DroitConge, Long> {
    Optional<DroitConge> findByIdEmployeAndIdTypeCongeAndAnnee(Long idEmploye, Long idTypeConge, int annee);
}
