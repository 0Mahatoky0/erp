package com.tovo.erp.evaluation.repository;

import com.tovo.erp.evaluation.model.PosteCompetence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosteCompetenceRepository extends JpaRepository<PosteCompetence, Long> {
    List<PosteCompetence> findByIdPosteOrderByPoidsDesc(Long idPoste);
}
