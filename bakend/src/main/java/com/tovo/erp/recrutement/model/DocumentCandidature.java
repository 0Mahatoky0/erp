package com.tovo.erp.recrutement.domain;

import jakarta.persistence.*; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "document_candidature") @Getter @Setter @NoArgsConstructor
public class DocumentCandidature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idDocument;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_candidature", nullable = false) private Candidature candidature;
    @Enumerated(EnumType.STRING) @Column(name = "type_document", nullable = false) @NotNull private TypeDocument typeDocument;
    @NotBlank @Size(max = 255) @Column(name = "nom_fichier", nullable = false) private String nomFichier;
    @NotBlank @Column(name = "cle_stockage", nullable = false, unique = true) private String cleStockage;
    @NotBlank @Size(max = 150) @Column(name = "type_mime", nullable = false) private String typeMime;
    @Positive @Column(name = "taille_octets", nullable = false) private Long tailleOctets;
    @Column(nullable = false, updatable = false) private LocalDateTime dateDepot;
    @PrePersist void onCreate() { dateDepot = LocalDateTime.now(); }
}
