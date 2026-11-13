package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.Alerte;
import com.tovo.erp.employe.model.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    List<Alerte> findByIdEmployeOrderByNiveauDescDateEcheanceAsc(Long idEmploye);

    List<Alerte> findAllByOrderByDateCreationDesc();

    void deleteByIdEmployeAndTypeAlerteIn(Long idEmploye, Collection<TypeAlerte> types);
}
