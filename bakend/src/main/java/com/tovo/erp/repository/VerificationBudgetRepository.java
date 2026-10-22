package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.VerificationBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationBudgetRepository extends JpaRepository<VerificationBudget, Long> {

    List<VerificationBudget> findByDemandePosteIdOrderByDateVerificationAsc(Long idDemandePoste);
}
