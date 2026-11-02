package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.Utilisateur;
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
              and (:departementId is null or u.departement.id = :departementId)
              and (:motif is null
                   or lower(u.nom) like :motif
                   or lower(u.email) like :motif)
            order by u.nom
            """)
    List<Utilisateur> rechercher(@Param("role") String role,
                                 @Param("departementId") Long departementId,
                                 @Param("motif") String motif,
                                 @Param("actif") Boolean actif);
}
