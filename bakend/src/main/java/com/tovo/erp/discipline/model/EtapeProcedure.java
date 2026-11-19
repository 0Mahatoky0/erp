package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "etape_procedure")
public class EtapeProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape")
    private Long id;

    @Column(name = "id_procedure", nullable = false)
    private Long idProcedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeEtapeProcedure type;

    @Column(name = "date_etape", nullable = false)
    private LocalDateTime dateEtape;

    @Column(name = "acteur_id", nullable = false)
    private Long acteurId;

    @Column(name = "mode_remise", length = 50)
    private String modeRemise;

    @Column(length = 255)
    private String preuve;

    @Column(name = "delai_reponse_jours")
    private Integer delaiReponseJours;

    @Column(name = "date_limite")
    private LocalDateTime dateLimite;

    @Column(name = "id_document")
    private Long idDocument;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdProcedure() { return idProcedure; }
    public void setIdProcedure(Long idProcedure) { this.idProcedure = idProcedure; }
    public TypeEtapeProcedure getType() { return type; }
    public void setType(TypeEtapeProcedure type) { this.type = type; }
    public LocalDateTime getDateEtape() { return dateEtape; }
    public void setDateEtape(LocalDateTime dateEtape) { this.dateEtape = dateEtape; }
    public Long getActeurId() { return acteurId; }
    public void setActeurId(Long acteurId) { this.acteurId = acteurId; }
    public String getModeRemise() { return modeRemise; }
    public void setModeRemise(String modeRemise) { this.modeRemise = modeRemise; }
    public String getPreuve() { return preuve; }
    public void setPreuve(String preuve) { this.preuve = preuve; }
    public Integer getDelaiReponseJours() { return delaiReponseJours; }
    public void setDelaiReponseJours(Integer delaiReponseJours) { this.delaiReponseJours = delaiReponseJours; }
    public LocalDateTime getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDateTime dateLimite) { this.dateLimite = dateLimite; }
    public Long getIdDocument() { return idDocument; }
    public void setIdDocument(Long idDocument) { this.idDocument = idDocument; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}
