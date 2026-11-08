package com.tovo.erp.employe.repository;

import com.tovo.erp.employe.model.DocumentEmploye;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentEmployeRepository extends JpaRepository<DocumentEmploye, Long> {

    List<DocumentEmploye> findByEmployeIdOrderByDateCreationDesc(Long idEmploye);

    Optional<DocumentEmploye> findFirstByEmployeIdAndTypeDocumentOrderByDateCreationDesc(
            Long idEmploye, TypeDocumentEmploye typeDocument);
}
