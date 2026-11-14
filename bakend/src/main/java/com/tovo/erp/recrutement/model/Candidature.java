package com.tovo.erp.recrutement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "candidature", uniqueConstraints = @UniqueConstraint(columnNames = {"id_offre", "id_candidat"}))
@Getter @Setter @NoArgsConstructor
public class Candidature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idCandidature;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_offre", nullable = false) private OffreEmploi offre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidat", nullable = false) private Candidat candidat;
    @Column(columnDefinition = "TEXT") private String messageCandidat;
    @Size(max = 100) private String source;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CandidatureStatut statut = CandidatureStatut.RECUE;
    @Column(nullable = false, updatable = false) private LocalDateTime dateCandidature;
    @Column(nullable = false) private LocalDateTime dateModification;
    @Column(name = "score_dossier") private BigDecimal scoreDossier;
    @Column(name = "score_global") private BigDecimal scoreGlobal;
    private Integer rang;
    @PrePersist void onCreate() { dateCandidature = LocalDateTime.now(); dateModification = dateCandidature; }
    @PreUpdate void onUpdate() { dateModification = LocalDateTime.now(); }
}
