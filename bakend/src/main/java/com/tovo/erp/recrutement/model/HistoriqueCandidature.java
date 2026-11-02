package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "historique_candidature") @Getter @Setter @NoArgsConstructor
public class HistoriqueCandidature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idHistorique;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    private Long idUtilisateur;
    @Enumerated(EnumType.STRING) @Column(name = "auteur_type", nullable = false) private AuteurType auteurType;
    @Enumerated(EnumType.STRING) @Column(name = "ancien_statut") private CandidatureStatut ancienStatut;
    @Enumerated(EnumType.STRING) @Column(name = "nouveau_statut", nullable = false) private CandidatureStatut nouveauStatut;
    private String commentaire;
    @Column(nullable = false, updatable = false) private LocalDateTime dateAction;
    @PrePersist void onCreate() { dateAction = LocalDateTime.now(); }
}
