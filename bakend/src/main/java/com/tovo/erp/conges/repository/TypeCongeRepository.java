package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeCongeRepository extends JpaRepository<TypeConge, Long> {
    List<TypeConge> findByActifTrueOrderByLibelleAsc();
    Optional<TypeConge> findByCode(String code);
}
