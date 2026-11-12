package com.tovo.erp.conges.repository;

import com.tovo.erp.conges.model.MatriceRemplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatriceRemplacementRepository extends JpaRepository<MatriceRemplacement, Long> {

    @Query("""
        select m from MatriceRemplacement m
        where m.actif = true
          and ((m.idPosteRemplace = :idPoste and :idPoste is not null)
               or (m.idEmployeRemplace = :idEmploye and :idEmploye is not null))
        order by m.niveau asc, m.id asc
        """)
    List<MatriceRemplacement> pourRemplace(@Param("idPoste") Long idPoste,
                                           @Param("idEmploye") Long idEmploye);
}
