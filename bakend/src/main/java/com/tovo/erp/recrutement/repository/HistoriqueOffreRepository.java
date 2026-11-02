package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.model.HistoriqueOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoriqueOffreRepository extends JpaRepository<HistoriqueOffre, Long> {
    List<HistoriqueOffre> findByOffreIdOffreOrderByDateActionAsc(Long idOffre);
}
