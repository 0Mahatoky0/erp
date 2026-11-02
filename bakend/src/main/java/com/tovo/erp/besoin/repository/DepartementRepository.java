package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartementRepository extends JpaRepository<Departement, Long> {

    Optional<Departement> findByNomIgnoreCase(String nom);
}
