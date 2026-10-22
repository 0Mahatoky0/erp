package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "passage_test") @Getter @Setter @NoArgsConstructor
public class PassageTest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idPassage;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_test", nullable = false) private TestRecrutement test;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    @Positive @Column(name = "numero_tentative", nullable = false) private Integer numeroTentative = 1;
    private LocalDateTime datePrevue; private LocalDateTime dateDebut; private LocalDateTime dateFin;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TestStatut statut = TestStatut.PLANIFIE;
    private String cleStockageReponse;
    @Column(columnDefinition = "TEXT") private String commentaire;

    @AssertTrue(message = "La fin du test doit être postérieure ou égale au début")
    boolean datesCoherentes() {
        return dateFin == null || (dateDebut != null && !dateFin.isBefore(dateDebut));
    }
}
