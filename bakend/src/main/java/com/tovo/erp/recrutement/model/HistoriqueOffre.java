package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "historique_offre") @Getter @Setter @NoArgsConstructor
public class HistoriqueOffre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idHistorique;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_offre", nullable = false) private OffreEmploi offre;
    private Long idUtilisateur;
    @Enumerated(EnumType.STRING) @Column(name = "ancien_statut") private OffreStatut ancienStatut;
    @Enumerated(EnumType.STRING) @Column(name = "nouveau_statut", nullable = false) private OffreStatut nouveauStatut;
    private String commentaire;
    @Column(nullable = false, updatable = false) private LocalDateTime dateAction;
    @PrePersist void onCreate() { dateAction = LocalDateTime.now(); }
}
