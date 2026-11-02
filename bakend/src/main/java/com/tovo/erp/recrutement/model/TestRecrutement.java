package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.LocalDateTime;

@Entity @Table(name = "test_recrutement") @Getter @Setter @NoArgsConstructor
public class TestRecrutement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idTest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_offre", nullable = false) private OffreEmploi offre;
    @NotBlank @Size(max = 150) @Column(nullable = false) private String titre;
    @Column(columnDefinition = "TEXT") private String description;
    @Positive @Column(name = "duree_minutes") private Integer dureeMinutes;
    @Positive @Column(name = "note_max", nullable = false) private BigDecimal noteMax = BigDecimal.valueOf(20);
    @Column(nullable = false, updatable = false) private LocalDateTime dateCreation;
    @PrePersist void onCreate() { dateCreation = LocalDateTime.now(); }
}
