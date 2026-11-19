package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_temoin")
public class IncidentTemoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_temoin")
    private Long id;

    @Column(name = "id_incident", nullable = false)
    private Long idIncident;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(nullable = false)
    private boolean protege;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdIncident() { return idIncident; }
    public void setIdIncident(Long idIncident) { this.idIncident = idIncident; }
    public Long getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Long idEmploye) { this.idEmploye = idEmploye; }
    public boolean isProtege() { return protege; }
    public void setProtege(boolean protege) { this.protege = protege; }
}
