package com.tovo.erp.shared.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    List<JournalAudit> findByRessourceAndIdRessourceOrderByDateActionDesc(String ressource, Long idRessource);
}
