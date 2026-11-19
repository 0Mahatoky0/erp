package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IncidentEmployeId implements Serializable {

    @Column(name = "id_incident")
    private Long idIncident;

    @Column(name = "id_employe")
    private Long idEmploye;

    public IncidentEmployeId() {
    }

    public IncidentEmployeId(Long idIncident, Long idEmploye) {
        this.idIncident = idIncident;
        this.idEmploye = idEmploye;
    }

    public Long getIdIncident() { return idIncident; }
    public void setIdIncident(Long idIncident) { this.idIncident = idIncident; }
    public Long getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Long idEmploye) { this.idEmploye = idEmploye; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IncidentEmployeId autre)) return false;
        return Objects.equals(idIncident, autre.idIncident) && Objects.equals(idEmploye, autre.idEmploye);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idIncident, idEmploye);
    }
}
