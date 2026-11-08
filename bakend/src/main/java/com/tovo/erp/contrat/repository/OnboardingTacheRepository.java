package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.OnboardingTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTacheRepository extends JpaRepository<OnboardingTache, Long> {

    List<OnboardingTache> findByOnboardingIdOrderById(Long idOnboarding);

    long countByOnboardingId(Long idOnboarding);
}
