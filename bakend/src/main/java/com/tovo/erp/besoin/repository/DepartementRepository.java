package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartementRepository extends JpaRepository<Departement, Long> {

    Optional<Departement> findByNomIgnoreCase(String nom);
}
