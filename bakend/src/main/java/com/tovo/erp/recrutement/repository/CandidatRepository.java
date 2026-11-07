package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
    Optional<Candidat> findByEmailIgnoreCase(String email);
    Optional<Candidat> findByIdUtilisateur(Long idUtilisateur);
}
