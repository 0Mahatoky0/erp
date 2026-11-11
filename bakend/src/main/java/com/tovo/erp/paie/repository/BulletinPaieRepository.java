package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.BulletinPaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BulletinPaieRepository extends JpaRepository<BulletinPaie, Long> {
    List<BulletinPaie> findByIdPeriodeOrderByIdEmployeAsc(Long idPeriode);
    Optional<BulletinPaie> findByIdPeriodeAndIdEmploye(Long idPeriode, Long idEmploye);
}
