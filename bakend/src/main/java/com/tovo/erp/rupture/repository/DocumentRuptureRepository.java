package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.DocumentRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRuptureRepository extends JpaRepository<DocumentRupture, Long> {

    List<DocumentRupture> findByIdDossierOrderByIdAsc(Long idDossier);

    Optional<DocumentRupture> findByIdAndIdDossier(Long id, Long idDossier);
}
