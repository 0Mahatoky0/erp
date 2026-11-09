package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.OnboardingModeleTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingModeleTacheRepository extends JpaRepository<OnboardingModeleTache, Long> {

    List<OnboardingModeleTache> findByIdModeleOrderByIdAsc(Long idModele);
}
