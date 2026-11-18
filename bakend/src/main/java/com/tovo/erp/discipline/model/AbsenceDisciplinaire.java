package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "absence_disciplinaire")
public class AbsenceDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_absence")
    private Long id;

    @Column(name = "id_sanction", nullable = false)
    private Long idSanction;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal jours;

    @Column(nullable = false)
    private boolean remuneree = true;

    @Column(nullable = false, length = 150)
    private String motif;

    @Column(name = "traitee_paie", nullable = false)
    private boolean traiteePaie;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdSanction() { return idSanction; }
    public void setIdSanction(Long idSanction) { this.idSanction = idSanction; }
    public Long getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Long idEmploye) { this.idEmploye = idEmploye; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public BigDecimal getJours() { return jours; }
    public void setJours(BigDecimal jours) { this.jours = jours; }
    public boolean isRemuneree() { return remuneree; }
    public void setRemuneree(boolean remuneree) { this.remuneree = remuneree; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public boolean isTraiteePaie() { return traiteePaie; }
    public void setTraiteePaie(boolean traiteePaie) { this.traiteePaie = traiteePaie; }
}
