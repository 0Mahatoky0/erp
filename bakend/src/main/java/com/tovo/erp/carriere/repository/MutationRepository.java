package com.tovo.erp.carriere.repository;

import com.tovo.erp.carriere.model.Mutation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MutationRepository extends JpaRepository<Mutation, Long> {
    Optional<Mutation> findByIdMouvement(Long idMouvement);

    List<Mutation> findByDateRetourPrevueIsNotNull();
}
