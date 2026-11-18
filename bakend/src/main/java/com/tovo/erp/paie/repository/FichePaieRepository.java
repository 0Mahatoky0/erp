package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.FichePaie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FichePaieRepository extends JpaRepository<FichePaie, Long> {
    List<FichePaie> findByIdPeriodeOrderByIdEmployeAsc(Long idPeriode);

    List<FichePaie> findByIdEmployeOrderByMoisDesc(Long idEmploye);

    Optional<FichePaie> findByIdPeriodeAndIdEmploye(Long idPeriode, Long idEmploye);
}
