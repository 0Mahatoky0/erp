package com.tovo.erp.rupture.repository;

import com.tovo.erp.rupture.model.SoldeToutCompte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SoldeToutCompteRepository extends JpaRepository<SoldeToutCompte, Long> {

    Optional<SoldeToutCompte> findByIdDossier(Long idDossier);
}
