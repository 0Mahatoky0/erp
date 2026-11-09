package com.tovo.erp.contrat.repository;

import com.tovo.erp.contrat.model.Notification;
import com.tovo.erp.contrat.model.NotificationStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatutOrderByDateCreationDesc(NotificationStatut statut);

    List<Notification> findAllByOrderByDateCreationDesc();

    @Query("""
            select count(n) > 0 from Notification n
            where n.type = :type
              and ((:idContrat is null and n.idContrat is null) or n.idContrat = :idContrat)
              and ((:idTache is null and n.idTache is null) or n.idTache = :idTache)
              and ((:role is null and n.destinataireRole is null) or n.destinataireRole = :role)
              and ((:echeance is null and n.echeance is null) or n.echeance = :echeance)
            """)
    boolean existeEvenement(@Param("type") String type,
                            @Param("idContrat") Long idContrat,
                            @Param("idTache") Long idTache,
                            @Param("role") String role,
                            @Param("echeance") LocalDate echeance);
}
