package com.tovo.erp.shared.reference;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompteurReferenceRepository extends JpaRepository<CompteurReference, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompteurReference c where c.type = :type and c.annee = :annee")
    Optional<CompteurReference> verrouiller(@Param("type") String type, @Param("annee") int annee);
}
