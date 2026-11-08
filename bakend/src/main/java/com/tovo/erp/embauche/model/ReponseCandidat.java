package com.tovo.erp.embauche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Trace de la reponse du candidat (acceptation, refus ou contre-proposition). */
@Entity
@Table(name = "reponse_candidat")
@Getter
@Setter
public class ReponseCandidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reponse")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proposition", nullable = false)
    private PropositionEmbauche proposition;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_reponse", nullable = false, length = 30)
    private TypeReponseCandidat typeReponse;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "empreinte", length = 128)
    private String empreinte;

    @Column(name = "preuve", length = 255)
    private String preuve;

    @Column(name = "date_reponse", nullable = false)
    private LocalDateTime dateReponse = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateReponse == null) {
            dateReponse = LocalDateTime.now();
        }
    }
}
