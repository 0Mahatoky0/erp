package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {

    List<Onboarding> findByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    Optional<Onboarding> findFirstByIdEmployeOrderByDateCreationDesc(Long idEmploye);

    Optional<Onboarding> findFirstByIdContrat(Long idContrat);

    List<Onboarding> findByIdContratIn(Collection<Long> idsContrat);

    List<Onboarding> findByStatutNotOrderByDateDebutAsc(OnboardingStatut statut);
}
