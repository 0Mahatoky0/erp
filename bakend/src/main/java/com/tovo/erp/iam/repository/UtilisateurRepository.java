package com.tovo.erp.iam.repository;

import com.tovo.erp.iam.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from Utilisateur u
            where (:actif is null or u.actif = :actif)
              and (:role is null or u.role.nom = :role)
              and (:departementId is null or u.idDepartement = :departementId)
              and (:motif is null
                   or lower(u.nom) like :motif
                   or lower(u.email) like :motif)
            order by u.nom
            """)
    List<Utilisateur> rechercher(@Param("role") String role,
                                 @Param("departementId") Long departementId,
                                 @Param("motif") String motif,
                                 @Param("actif") Boolean actif);

    @Query("select u.id from Utilisateur u where lower(u.nom) like :motif")
    List<Long> idsParNom(@Param("motif") String motif);

    @Query(value = "select d.nom from departement d where d.id_departement = :id", nativeQuery = true)
    String trouverNomDepartement(@Param("id") Long id);

    @Query(value = "select id_departement, nom from departement", nativeQuery = true)
    List<Object[]> trouverDepartements();
}
