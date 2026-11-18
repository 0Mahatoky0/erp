package com.tovo.erp.discipline.repository;

import com.tovo.erp.discipline.model.AbsenceDisciplinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceDisciplinaireRepository extends JpaRepository<AbsenceDisciplinaire, Long> {
    List<AbsenceDisciplinaire> findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            LocalDate fin, LocalDate debut);

    List<AbsenceDisciplinaire> findByTraiteePaieFalseAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            LocalDate fin, LocalDate debut);

    List<AbsenceDisciplinaire> findByIdSanctionIn(List<Long> idsSanction);
}
