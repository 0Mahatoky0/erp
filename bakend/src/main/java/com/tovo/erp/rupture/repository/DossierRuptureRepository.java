package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.DossierRupture;
import com.tovo.erp.rupture.model.StatutDossierRupture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DossierRuptureRepository extends JpaRepository<DossierRupture, Long> {

    List<DossierRupture> findAllByOrderByDateCreationDesc();

    Optional<DossierRupture> findByReference(String reference);

    boolean existsByIdEmployeAndStatutNotIn(Long idEmploye, Collection<StatutDossierRupture> statuts);

    List<DossierRupture> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    List<DossierRupture> findByStatutIn(Collection<StatutDossierRupture> statuts);

    List<DossierRupture> findByDateSortieBetweenOrderByDateSortieAsc(LocalDate debut, LocalDate fin);

    List<DossierRupture> findByStatutAndDateModificationBefore(StatutDossierRupture statut, LocalDateTime date);
}
