package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {

    List<Onboarding> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    Optional<Onboarding> findFirstByIdEmploye(Long idEmploye);
}
