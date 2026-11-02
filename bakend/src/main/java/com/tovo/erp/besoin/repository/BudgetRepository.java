package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.Budget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByServiceIdAndExercice(Long idService, Integer exercice);

    List<Budget> findByExerciceOrderByServiceNom(Integer exercice);

    @Query("""
            select b from Budget b
            join fetch b.service s
            where (:idService is null or s.id = :idService)
              and (:exercice is null or b.exercice = :exercice)
            order by b.exercice desc, s.nom
            """)
    List<Budget> rechercher(@Param("idService") Long idService,
                                   @Param("exercice") Integer exercice);

    /**
     * Verrou pessimiste sur la ligne de budget : deux validations financieres
     * simultanees ne peuvent pas depasser le budget disponible (RM-056).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Budget b where b.service.id = :idService and b.exercice = :exercice")
    Optional<Budget> findForMiseAJour(@Param("idService") Long idService,
                                             @Param("exercice") Integer exercice);
}
