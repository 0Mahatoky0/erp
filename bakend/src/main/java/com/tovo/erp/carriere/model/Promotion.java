package com.tovo.erp.carriere.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promotion")
    private Long id;

    @Column(name = "id_mouvement", nullable = false, unique = true)
    private Long idMouvement;

    @Column(name = "nature", nullable = false, length = 40)
    private String nature;

    @Column(name = "id_ancien_poste", nullable = false)
    private Long idAncienPoste;

    @Column(name = "id_nouveau_poste", nullable = false)
    private Long idNouveauPoste;

    @Column(name = "ancienne_categorie", length = 50)
    private String ancienneCategorie;

    @Column(name = "nouvelle_categorie", length = 50)
    private String nouvelleCategorie;

    @Column(name = "ancien_echelon", length = 50)
    private String ancienEchelon;

    @Column(name = "nouveau_echelon", length = 50)
    private String nouveauEchelon;

    @Column(name = "ancien_salaire", nullable = false, precision = 15, scale = 2)
    private BigDecimal ancienSalaire;

    @Column(name = "nouveau_salaire", nullable = false, precision = 15, scale = 2)
    private BigDecimal nouveauSalaire;
}
