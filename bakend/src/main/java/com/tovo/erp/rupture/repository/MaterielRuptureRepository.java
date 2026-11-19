package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.MaterielRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterielRuptureRepository extends JpaRepository<MaterielRupture, Long> {

    List<MaterielRupture> findByIdDossierOrderByIdAsc(Long idDossier);
}
