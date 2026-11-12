package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "matrice_remplacement")
@Getter
@Setter
public class MatriceRemplacement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matrice")
    private Long id;
    @Column(name = "id_poste_remplace")
    private Long idPosteRemplace;
    @Column(name = "id_employe_remplace")
    private Long idEmployeRemplace;
    @Column(name = "id_poste_remplacant")
    private Long idPosteRemplacant;
    @Column(name = "id_employe_remplacant")
    private Long idEmployeRemplacant;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauRemplacement niveau = NiveauRemplacement.PRIMAIRE;
    @Column(name = "duree_min_jours")
    private Integer dureeMinJours;
    @Column(name = "duree_max_jours")
    private Integer dureeMaxJours;
    @Column(name = "competences_requises", length = 500)
    private String competencesRequises;
    @Column(length = 500)
    private String habilitations;
    @Column(nullable = false)
    private boolean actif = true;
}
