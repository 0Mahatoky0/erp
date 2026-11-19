package com.tovo.erp.carriere.repository;

import com.tovo.erp.carriere.model.GrilleSalariale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrilleSalarialeRepository extends JpaRepository<GrilleSalariale, Long> {
    List<GrilleSalariale> findByCategorieAndEchelonOrderByDateEffetDesc(String categorie, String echelon);
}
