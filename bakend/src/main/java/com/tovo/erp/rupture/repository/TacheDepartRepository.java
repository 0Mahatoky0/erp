package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.StatutTacheRupture;
import com.tovo.erp.rupture.model.TacheDepart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TacheDepartRepository extends JpaRepository<TacheDepart, Long> {

    List<TacheDepart> findByIdDossierOrderByIdAsc(Long idDossier);

    long countByIdDossierAndBloquanteTrueAndStatutNot(Long idDossier, StatutTacheRupture statut);
}
