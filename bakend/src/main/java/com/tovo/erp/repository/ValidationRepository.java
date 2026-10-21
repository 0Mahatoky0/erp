package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

    List<Validation> findByDemandePosteIdOrderByDateActionAsc(Long idDemandePoste);

    List<Validation> findByDemandePosteIdAndEpisodeOrderByDateActionAsc(Long idDemandePoste, Integer episode);
}
