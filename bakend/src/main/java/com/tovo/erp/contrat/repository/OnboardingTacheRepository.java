package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.TacheStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTacheRepository extends JpaRepository<OnboardingTache, Long> {

    List<OnboardingTache> findByOnboardingIdOrderById(Long idOnboarding);

    long countByOnboardingId(Long idOnboarding);

    long countByOnboardingIdAndStatut(Long idOnboarding, TacheStatut statut);

    long countByOnboardingIdAndObligatoireTrueAndBloquanteTrueAndStatutNotIn(
            Long idOnboarding, List<TacheStatut> statuts);
}
