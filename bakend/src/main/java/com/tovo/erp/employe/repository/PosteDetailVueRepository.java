package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.PosteDetailVue;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lecture seule de la vue des postes, sans save ni modification. */
public interface PosteDetailVueRepository extends JpaRepository<PosteDetailVue, Long> {
}
