package com.tovo.erp.besoin.repository;

import com.tovo.erp.besoin.model.DemandeRecrutement;
import com.tovo.erp.besoin.model.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutement, Long> {

    List<DemandeRecrutement> findByIdUtilisateurOrderByDateCreationDesc(Long idUtilisateur);

    List<DemandeRecrutement> findByStatutOrderByDateCreationDesc(StatutDemande statut);

    /**
     * Demandes visibles par un utilisateur. Un MANAGER voit ses propres demandes,
     * les autres roles (RH, FINANCE, DIRECTION) voient l'ensemble du perimetre.
     */
    @Query("""
            select d from DemandeRecrutement d
            where (:mesDemandes = false or d.idUtilisateur = :idUtilisateur)
              and (:statut is null or d.statut = :statut)
              and (:motif is null
                   or lower(d.motif) like :motif
                   or d.idUtilisateur in :idsUtilisateurs)
            order by d.dateCreation desc
            """)
    List<DemandeRecrutement> rechercher(@Param("idUtilisateur") Long idUtilisateur,
                                         @Param("mesDemandes") boolean mesDemandes,
                                         @Param("statut") StatutDemande statut,
                                         @Param("motif") String motif,
                                         @Param("idsUtilisateurs") Collection<Long> idsUtilisateurs);
}
