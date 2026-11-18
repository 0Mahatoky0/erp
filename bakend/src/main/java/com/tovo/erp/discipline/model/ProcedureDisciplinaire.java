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
@Table(name = "procedure_disciplinaire")
public class ProcedureDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedure")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "id_incident", nullable = false)
    private Long idIncident;

    @Column(name = "id_employe", nullable = false)
    private Long idEmploye;

    @Column(name = "instructeur_id", nullable = false)
    private Long instructeurId;

    @Column(name = "decideur_id", nullable = false)
    private Long decideurId;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture;

    @Column(name = "date_limite_prescription", nullable = false)
    private LocalDate dateLimitePrescription;

    @Column(name = "mise_a_pied", nullable = false)
    private boolean miseAPied;

    @Column(name = "mise_a_pied_debut")
    private LocalDate miseAPiedDebut;

    @Column(name = "mise_a_pied_fin")
    private LocalDate miseAPiedFin;

    @Column(name = "mise_a_pied_remuneree")
    private Boolean miseAPiedRemuneree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutProcedure statut;

    @Column(length = 40)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "pieces_referencees", columnDefinition = "TEXT")
    private String piecesReferencees;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Version
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Long getIdIncident() { return idIncident; }
    public void setIdIncident(Long idIncident) { this.idIncident = idIncident; }
    public Long getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Long idEmploye) { this.idEmploye = idEmploye; }
    public Long getInstructeurId() { return instructeurId; }
    public void setInstructeurId(Long instructeurId) { this.instructeurId = instructeurId; }
    public Long getDecideurId() { return decideurId; }
    public void setDecideurId(Long decideurId) { this.decideurId = decideurId; }
    public LocalDateTime getDateOuverture() { return dateOuverture; }
    public void setDateOuverture(LocalDateTime dateOuverture) { this.dateOuverture = dateOuverture; }
    public LocalDate getDateLimitePrescription() { return dateLimitePrescription; }
    public void setDateLimitePrescription(LocalDate dateLimitePrescription) { this.dateLimitePrescription = dateLimitePrescription; }
    public boolean isMiseAPied() { return miseAPied; }
    public void setMiseAPied(boolean miseAPied) { this.miseAPied = miseAPied; }
    public LocalDate getMiseAPiedDebut() { return miseAPiedDebut; }
    public void setMiseAPiedDebut(LocalDate miseAPiedDebut) { this.miseAPiedDebut = miseAPiedDebut; }
    public LocalDate getMiseAPiedFin() { return miseAPiedFin; }
    public void setMiseAPiedFin(LocalDate miseAPiedFin) { this.miseAPiedFin = miseAPiedFin; }
    public Boolean getMiseAPiedRemuneree() { return miseAPiedRemuneree; }
    public void setMiseAPiedRemuneree(Boolean miseAPiedRemuneree) { this.miseAPiedRemuneree = miseAPiedRemuneree; }
    public StatutProcedure getStatut() { return statut; }
    public void setStatut(StatutProcedure statut) { this.statut = statut; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public String getPiecesReferencees() { return piecesReferencees; }
    public void setPiecesReferencees(String piecesReferencees) { this.piecesReferencees = piecesReferencees; }
    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public void setDateCloture(LocalDateTime dateCloture) { this.dateCloture = dateCloture; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
