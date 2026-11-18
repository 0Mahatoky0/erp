package com.tovo.erp.discipline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_discipline")
public class JournalDiscipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_journal")
    private Long id;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 60)
    private String ressource;

    @Column(name = "id_ressource")
    private Long idRessource;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "adresse_ip", length = 64)
    private String adresseIp;

    @Column(columnDefinition = "TEXT")
    private String details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRessource() { return ressource; }
    public void setRessource(String ressource) { this.ressource = ressource; }
    public Long getIdRessource() { return idRessource; }
    public void setIdRessource(Long idRessource) { this.idRessource = idRessource; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
