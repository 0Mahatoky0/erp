package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.HistoriqueStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {

    List<HistoriqueStatut> findByDemandeIdOrderByDateActionAsc(Long idDemande);

    List<HistoriqueStatut> findByDemandePosteIdOrderByDateActionAsc(Long idDemandePoste);
}
