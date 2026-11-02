package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.VerificationBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationBudgetRepository extends JpaRepository<VerificationBudget, Long> {

    List<VerificationBudget> findByDemandePosteIdOrderByDateVerificationAsc(Long idDemandePoste);
}
