package com.tovo.erp.carriere.repository;

import com.tovo.erp.carriere.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByIdMouvement(Long idMouvement);
}
