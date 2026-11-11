package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.employe.model.StatutEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, Long> {

    List<Employe> findByStatutOrderByNomAscPrenomAsc(StatutEmploye statut);

    Optional<Employe> findFirstByEmailIgnoreCase(String email);

    Optional<Employe> findFirstByCinIgnoreCase(String cin);

    Optional<Employe> findFirstByNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissance(
            String nom, String prenom, LocalDate dateNaissance);

    Optional<Employe> findFirstByIdCandidat(Long idCandidat);

    Optional<Employe> findFirstByIdUtilisateur(Long idUtilisateur);

    List<Employe> findByIdInAndStatut(java.util.Collection<Long> ids, StatutEmploye statut);

    @Query("""
            select e from Employe e
            where (:statut is null or e.statut = :statut)
              and (:idService is null or e.idPoste in (
                   select p.id from PosteDetailVue p where p.idService = :idService))
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
