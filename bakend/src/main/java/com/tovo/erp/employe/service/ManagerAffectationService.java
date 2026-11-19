package com.tovo.erp.employe.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerAffectationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public boolean estEncoreManager(Long idManager) {
        if (idManager == null) {
            return false;
        }
        Object resultat = entityManager.createNativeQuery("""
                        select count(*) from affectation a
                        where a.id_manager = :manager
                          and a.principale = true
                          and a.statut in ('ACTIVE', 'PLANIFIEE')
                        """)
                .setParameter("manager", idManager)
                .getSingleResult();
        return resultat instanceof Number nombre && nombre.longValue() > 0;
    }
}
