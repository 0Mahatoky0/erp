package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeRepository extends JpaRepository<Employe, Long> {

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMatricule(String matricule);

    List<Employe> findByStatutOrderByNomAscPrenomAsc(StatutEmploye statut);

    @Query("""
            select e from Employe e
            where (:statut is null or e.statut = :statut)
              and (:idService is null or e.poste.service.id = :idService)
              and (:motif is null
                   or lower(e.nom) like :motif
                   or lower(e.prenom) like :motif
                   or lower(e.matricule) like :motif)
            order by e.nom, e.prenom
            """)
    List<Employe> rechercher(@Param("statut") StatutEmploye statut,
                              @Param("idService") Long idService,
                              @Param("motif") String motif);
}
