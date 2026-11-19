package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sanction_disciplinaire")
public class SanctionDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sanction")
    private Long id;

    @Column(name = "id_procedure", nullable = false)
    private Long idProcedure;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypeSanction type;

    @Column(name = "date_notification", nullable = false)
    private LocalDateTime dateNotification;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "duree_jours")
    private Integer dureeJours;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(name = "base_reglement", nullable = false, length = 100)
    private String baseReglement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatutSanction statut;

    @Column(name = "date_effacement", nullable = false)
    private LocalDate dateEffacement;

    @Column(name = "impact_paie_jours", precision = 6, scale = 2)
    private BigDecimal impactPaieJours;

    @Version
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdProcedure() { return idProcedure; }
    public void setIdProcedure(Long idProcedure) { this.idProcedure = idProcedure; }
    public Long getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Long idEmploye) { this.idEmploye = idEmploye; }
    public TypeSanction getType() { return type; }
    public void setType(TypeSanction type) { this.type = type; }
    public LocalDateTime getDateNotification() { return dateNotification; }
    public void setDateNotification(LocalDateTime dateNotification) { this.dateNotification = dateNotification; }
    public LocalDate getDateEffet() { return dateEffet; }
    public void setDateEffet(LocalDate dateEffet) { this.dateEffet = dateEffet; }
    public Integer getDureeJours() { return dureeJours; }
    public void setDureeJours(Integer dureeJours) { this.dureeJours = dureeJours; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getBaseReglement() { return baseReglement; }
    public void setBaseReglement(String baseReglement) { this.baseReglement = baseReglement; }
    public StatutSanction getStatut() { return statut; }
    public void setStatut(StatutSanction statut) { this.statut = statut; }
    public LocalDate getDateEffacement() { return dateEffacement; }
    public void setDateEffacement(LocalDate dateEffacement) { this.dateEffacement = dateEffacement; }
    public BigDecimal getImpactPaieJours() { return impactPaieJours; }
    public void setImpactPaieJours(BigDecimal impactPaieJours) { this.impactPaieJours = impactPaieJours; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
