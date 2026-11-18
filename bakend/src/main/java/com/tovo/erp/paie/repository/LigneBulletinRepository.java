package com.tovo.erp.paie.repository;

import com.tovo.erp.paie.model.LigneBulletin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneBulletinRepository extends JpaRepository<LigneBulletin, Long> {
    List<LigneBulletin> findByIdBulletinOrderByOrdreAsc(Long idBulletin);

    void deleteByIdBulletin(Long idBulletin);
}
