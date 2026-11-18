package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.EtapeProcedure;
import com.tovo.erp.discipline.model.TypeEtapeProcedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtapeRepository extends JpaRepository<EtapeProcedure, Long> {
    List<EtapeProcedure> findByIdProcedureOrderByDateEtapeAsc(Long idProcedure);
    boolean existsByIdProcedureAndType(Long idProcedure, TypeEtapeProcedure type);
}
