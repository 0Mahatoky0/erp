package com.tovo.erp.demande.repository;

import com.tovo.erp.demande.domain.VerificationBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationBudgetRepository extends JpaRepository<VerificationBudget, Long> {

    List<VerificationBudget> findByDemandePosteIdOrderByDateVerificationAsc(Long idDemandePoste);
}
