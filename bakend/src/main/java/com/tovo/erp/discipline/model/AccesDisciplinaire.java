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
@Table(name = "acces_disciplinaire")
public class AccesDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acces")
    private Long id;

    @Column(name = "id_incident")
    private Long idIncident;

    @Column(name = "id_procedure")
    private Long idProcedure;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NiveauAccesDisciplinaire niveau;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(name = "accorde_par_id", nullable = false)
    private Long accordeParId;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(nullable = false)
    private boolean actif = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdIncident() { return idIncident; }
    public void setIdIncident(Long idIncident) { this.idIncident = idIncident; }
    public Long getIdProcedure() { return idProcedure; }
    public void setIdProcedure(Long idProcedure) { this.idProcedure = idProcedure; }
    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }
    public NiveauAccesDisciplinaire getNiveau() { return niveau; }
    public void setNiveau(NiveauAccesDisciplinaire niveau) { this.niveau = niveau; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public Long getAccordeParId() { return accordeParId; }
    public void setAccordeParId(Long accordeParId) { this.accordeParId = accordeParId; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
