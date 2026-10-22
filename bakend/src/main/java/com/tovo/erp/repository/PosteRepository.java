package com.tovo.erp.repository;

import com.tovo.erp.domain.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PosteRepository extends JpaRepository<Poste, Long> {

    List<Poste> findByServiceIdOrderByIntitule(Long idService);

    Optional<Poste> findByServiceIdAndIntituleIgnoreCase(Long idService, String intitule);

    /**
     * Recherche d'un poste equivalent : meme service et intitule comparable
     * (casse et espaces ignores) - protection doublons (RM-010).
     */
    @Query("""
            select p from Poste p
            where p.service.id = :idService
              and lower(trim(p.intitule)) = lower(trim(:intitule))
              and (:idPosteExclu is null or p.id <> :idPosteExclu)
            """)
    List<Poste> findEquivalents(@Param("idService") Long idService,
                                @Param("intitule") String intitule,
                                @Param("idPosteExclu") Long idPosteExclu);

    @Query("""
            select p from Poste p
            where (:idService is null or p.service.id = :idService)
              and (:actif is null or p.actif = :actif)
              and (:motif is null
                   or lower(p.intitule) like :motif
                   or lower(coalesce(p.description, '')) like :motif)
            order by p.service.nom, p.intitule
            """)
    List<Poste> rechercher(@Param("idService") Long idService,
                           @Param("actif") Boolean actif,
                           @Param("motif") String motif);
}
