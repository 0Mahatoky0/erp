package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.HistoriqueStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {

    List<HistoriqueStatut> findByDemandeIdOrderByDateActionAsc(Long idDemande);

    List<HistoriqueStatut> findByDemandePosteIdOrderByDateActionAsc(Long idDemandePoste);
}
