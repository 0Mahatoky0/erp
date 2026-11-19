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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incident")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "declarant_id", nullable = false)
    private Long declarantId;

    @Column(name = "date_declaration", nullable = false)
    private LocalDateTime dateDeclaration;

    @Column(name = "date_faits", nullable = false)
    private LocalDate dateFaits;

    @Column(name = "date_connaissance", nullable = false)
    private LocalDate dateConnaissance;

    @Column(length = 200)
    private String lieu;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategorieIncident categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravite_initiale", nullable = false, length = 20)
    private GraviteIncident graviteInitiale;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravite_retenue", length = 20)
    private GraviteIncident graviteRetenue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutIncident statut;

    @Column(name = "motif_classement", columnDefinition = "TEXT")
    private String motifClassement;

    @Column(name = "date_qualification")
    private LocalDateTime dateQualification;

    @Column(name = "qualifie_par")
    private Long qualifiePar;

    @Version
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Long getDeclarantId() { return declarantId; }
    public void setDeclarantId(Long declarantId) { this.declarantId = declarantId; }
    public LocalDateTime getDateDeclaration() { return dateDeclaration; }
    public void setDateDeclaration(LocalDateTime dateDeclaration) { this.dateDeclaration = dateDeclaration; }
    public LocalDate getDateFaits() { return dateFaits; }
    public void setDateFaits(LocalDate dateFaits) { this.dateFaits = dateFaits; }
    public LocalDate getDateConnaissance() { return dateConnaissance; }
    public void setDateConnaissance(LocalDate dateConnaissance) { this.dateConnaissance = dateConnaissance; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CategorieIncident getCategorie() { return categorie; }
    public void setCategorie(CategorieIncident categorie) { this.categorie = categorie; }
    public GraviteIncident getGraviteInitiale() { return graviteInitiale; }
    public void setGraviteInitiale(GraviteIncident graviteInitiale) { this.graviteInitiale = graviteInitiale; }
    public GraviteIncident getGraviteRetenue() { return graviteRetenue; }
    public void setGraviteRetenue(GraviteIncident graviteRetenue) { this.graviteRetenue = graviteRetenue; }
    public StatutIncident getStatut() { return statut; }
    public void setStatut(StatutIncident statut) { this.statut = statut; }
    public String getMotifClassement() { return motifClassement; }
    public void setMotifClassement(String motifClassement) { this.motifClassement = motifClassement; }
    public LocalDateTime getDateQualification() { return dateQualification; }
    public void setDateQualification(LocalDateTime dateQualification) { this.dateQualification = dateQualification; }
    public Long getQualifiePar() { return qualifiePar; }
    public void setQualifiePar(Long qualifiePar) { this.qualifiePar = qualifiePar; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
