package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.OnboardingModele;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingModeleRepository extends JpaRepository<OnboardingModele, Long> {

    List<OnboardingModele> findByActifTrueOrderByIdAsc();
}
