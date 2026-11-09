package com.tovo.erp.besoin.service;

import com.tovo.erp.besoin.dto.ReferentielDto;
import com.tovo.erp.besoin.repository.DepartementRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lecture des referentiels departements et services. */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final DepartementRepository departements;
    private final ServiceRepository services;

    public ReferentielService(DepartementRepository departements, ServiceRepository services) {
        this.departements = departements;
        this.services = services;
    }

    public List<ReferentielDto.DepartementListe> departements() {
        return departements.findAll().stream().map(ReferentielDto.DepartementListe::de).toList();
    }

    public List<ReferentielDto.ServiceListe> services(Long idDepartement) {
        List<com.tovo.erp.besoin.model.Service> resultat = idDepartement == null
                ? services.findAll()
                : services.findByDepartementIdOrderByNom(idDepartement);
        return resultat.stream().map(ReferentielDto.ServiceListe::de).toList();
    }
}
