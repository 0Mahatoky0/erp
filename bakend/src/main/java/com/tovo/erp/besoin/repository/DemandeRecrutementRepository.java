package com.tovo.erp.demande.repository;

import com.tovo.erp.demande.domain.DemandeRecrutement;
import com.tovo.erp.demande.domain.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutement, Long> {

    List<DemandeRecrutement> findByUtilisateurIdOrderByDateCreationDesc(Long idUtilisateur);

    List<DemandeRecrutement> findByStatutOrderByDateCreationDesc(StatutDemande statut);

    /**
     * Demandes visibles par un utilisateur. Un MANAGER voit ses propres demandes,
     * les autres roles (RH, FINANCE, DIRECTION) voient l'ensemble du perimetre.
     */
    @Query("""
            select distinct d from DemandeRecrutement d
            left join fetch d.utilisateur u
            where (:mesDemandes = false or u.id = :idUtilisateur)
              and (:statut is null or d.statut = :statut)
              and (:motif is null
                   or lower(d.motif) like :motif
                   or lower(u.nom) like :motif)
            order by d.dateCreation desc
            """)
    List<DemandeRecrutement> rechercher(@Param("idUtilisateur") Long idUtilisateur,
                                         @Param("mesDemandes") boolean mesDemandes,
                                         @Param("statut") StatutDemande statut,
                                         @Param("motif") String motif);
}
