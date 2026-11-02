package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByDepartementIdOrderByNom(Long idDepartement);

    Optional<Service> findByIdAndDepartementId(Long id, Long idDepartement);

    /** Recherche insensible a la casse sur le nom du service (RM-010). */
    @Query("""
            select s from Service s
            where lower(trim(s.nom)) = lower(trim(:nom))
              and s.id <> :idServiceExclu
            """)
    List<Service> findEquivalents(@Param("nom") String nom,
                                   @Param("idServiceExclu") Long idServiceExclu);
}
