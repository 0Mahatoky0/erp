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
@Table(name = "piece_disciplinaire")
public class PieceDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piece")
    private Long id;

    @Column(name = "id_incident")
    private Long idIncident;

    @Column(name = "id_procedure")
    private Long idProcedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypePieceDisciplinaire type;

    @Column(nullable = false, length = 500)
    private String fichier;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "auteur_id", nullable = false)
    private Long auteurId;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @Column(nullable = false, length = 30)
    private String confidentialite = "TRES_CONFIDENTIEL";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdIncident() { return idIncident; }
    public void setIdIncident(Long idIncident) { this.idIncident = idIncident; }
    public Long getIdProcedure() { return idProcedure; }
    public void setIdProcedure(Long idProcedure) { this.idProcedure = idProcedure; }
    public TypePieceDisciplinaire getType() { return type; }
    public void setType(TypePieceDisciplinaire type) { this.type = type; }
    public String getFichier() { return fichier; }
    public void setFichier(String fichier) { this.fichier = fichier; }
    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getAuteurId() { return auteurId; }
    public void setAuteurId(Long auteurId) { this.auteurId = auteurId; }
    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime dateAjout) { this.dateAjout = dateAjout; }
    public String getConfidentialite() { return confidentialite; }
    public void setConfidentialite(String confidentialite) { this.confidentialite = confidentialite; }
}
