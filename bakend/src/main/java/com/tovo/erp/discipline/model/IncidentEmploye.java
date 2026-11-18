package com.tovo.erp.discipline.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_employe")
public class IncidentEmploye {

    @EmbeddedId
    private IncidentEmployeId id;

    public IncidentEmploye() {
    }

    public IncidentEmploye(Long idIncident, Long idEmploye) {
        this.id = new IncidentEmployeId(idIncident, idEmploye);
    }

    public IncidentEmployeId getId() { return id; }
    public void setId(IncidentEmployeId id) { this.id = id; }
}
