package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.DroitsRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DroitsRuptureRepository extends JpaRepository<DroitsRupture, Long> {

    Optional<DroitsRupture> findByIdDossier(Long idDossier);
}
