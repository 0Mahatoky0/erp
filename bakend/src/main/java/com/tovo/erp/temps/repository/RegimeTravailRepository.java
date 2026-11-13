package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.RegimeTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegimeTravailRepository extends JpaRepository<RegimeTravail, Long> {
    List<RegimeTravail> findAllByOrderByLibelleAsc();
    Optional<RegimeTravail> findByCodeIgnoreCase(String code);
}
