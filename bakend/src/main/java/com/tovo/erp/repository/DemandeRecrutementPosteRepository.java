package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.DemandeRecrutementPoste;
import com.tovo.erp.domain.enums.StatutLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemandeRecrutementPosteRepository extends JpaRepository<DemandeRecrutementPoste, Long> {

    List<DemandeRecrutementPoste> findByDemandeIdOrderById(Long idDemande);

    /** Nombre de lignes de demande qui referencent un poste donne. */
    long countByPosteId(Long idPoste);

    List<DemandeRecrutementPoste> findByStatutOrderByDateCreationDesc(StatutLigne statut);

    /**
     * Lignes en attente d'une decision a l'etape donnee : c'est la file de travail
     * d'un validateur (RM-067).
     */
    @Query("""
            select l from DemandeRecrutementPoste l
            join fetch l.service s
            join fetch l.demande d
            where l.etapeCourante = :etape
              and l.statut in (com.tovo.erp.domain.enums.StatutLigne.EN_ATTENTE_RH,
                               com.tovo.erp.domain.enums.StatutLigne.EN_ATTENTE_FINANCE,
                               com.tovo.erp.domain.enums.StatutLigne.EN_ATTENTE_DIRECTION)
            order by l.dateCreation
            """)
    List<DemandeRecrutementPoste> findFileAttente(
            @Param("etape") com.tovo.erp.domain.enums.EtapeValidation etape);

    /** Lignes rattachees a une demande, avec les jointures utiles au detail. */
    @Query("""
            select l from DemandeRecrutementPoste l
            join fetch l.service s
            join fetch s.departement
            left join fetch l.poste
            left join fetch l.employeRemplace e
            left join fetch e.poste
            where l.demande.id = :idDemande
            order by l.id
            """)
    List<DemandeRecrutementPoste> findParDemande(@Param("idDemande") Long idDemande);

    @Query("""
            select l from DemandeRecrutementPoste l
            join fetch l.service s
            join fetch s.departement
            join fetch l.demande d
            left join fetch l.poste
            where l.id = :id
            """)
    java.util.Optional<DemandeRecrutementPoste> findDetailParId(@Param("id") Long id);
}
