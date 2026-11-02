package com.tovo.erp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Un departement de l'entreprise, regroupant un ou plusieurs services. */
@Entity
@Table(name = "departement")
@Getter
@Setter
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departement")
    private Long id;

    @Column(name = "nom", nullable = false, unique = true)
    private String nom;
}
