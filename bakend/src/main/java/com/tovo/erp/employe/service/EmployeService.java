package com.tovo.erp.service;

import com.tovo.erp.common.Recherche;
import com.tovo.erp.domain.enums.StatutEmploye;
import com.tovo.erp.repository.EmployeRepository;
import com.tovo.erp.web.dto.EmployeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultation des employes (RM-057 a RM-060).
 *
 * <p>Un employe n'est jamais supprime : son historique reste rattache a la demande.
 */
@Service
public class EmployeService {

    private final EmployeRepository employeRepository;

    public EmployeService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    /** Liste des employes filtres par statut, service et recherche textuelle. */
    @Transactional(readOnly = true)
    public List<EmployeDto.Liste> lister(StatutEmploye statut, Long idService, String recherche) {
        return employeRepository.rechercher(statut, idService, Recherche.motif(recherche)).stream()
                .map(EmployeDto.Liste::de)
                .toList();
    }
}
