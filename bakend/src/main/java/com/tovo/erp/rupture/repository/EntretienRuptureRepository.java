package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.EntretienRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntretienRuptureRepository extends JpaRepository<EntretienRupture, Long> {

    Optional<EntretienRupture> findByIdDossier(Long idDossier);
}
