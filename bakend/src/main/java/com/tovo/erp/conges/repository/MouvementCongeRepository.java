package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.MouvementConge;
import com.tovo.erp.conges.model.TypeMouvementConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MouvementCongeRepository extends JpaRepository<MouvementConge, Long> {
    List<MouvementConge> findByIdEmployeAndIdTypeCongeOrderByDateEffetAscDateCreationAsc(
            Long idEmploye, Long idTypeConge);

    List<MouvementConge> findByIdEmployeOrderByDateEffetDescDateCreationDesc(Long idEmploye);

    boolean existsByIdEmployeAndIdTypeCongeAndTypeMouvementAndDateEffet(
            Long idEmploye, Long idTypeConge, TypeMouvementConge typeMouvement, LocalDate dateEffet);

    @Query("""
        select coalesce(sum(m.jours), 0) from MouvementConge m
        where m.idEmploye = :employe and m.idTypeConge = :type
          and m.typeMouvement = :typeMouvement
        """)
    java.math.BigDecimal sommeParType(@Param("employe") Long employe,
                                      @Param("type") Long type,
                                      @Param("typeMouvement") TypeMouvementConge typeMouvement);
}
