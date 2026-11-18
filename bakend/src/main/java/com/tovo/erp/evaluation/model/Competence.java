package com.tovo.erp.evaluation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "competence")
@Getter
@Setter
public class Competence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_competence")
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 30)
    private String categorie;
    @Column(columnDefinition = "TEXT")
    private String echelle;
    @Column(name = "duree_validite_mois")
    private Integer dureeValiditeMois;
    @Column(nullable = false)
    private boolean actif = true;

    public enum Categorie {
        TECHNIQUE, COMPORTEMENTALE, MANAGERIALE, LINGUISTIQUE, REGLEMENTAIRE
    }
}
