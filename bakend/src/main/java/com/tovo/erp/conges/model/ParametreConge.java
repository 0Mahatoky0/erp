package com.tovo.erp.conges.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "parametre_conge")
@Getter
@Setter
public class ParametreConge {
    @Id
    @Column(length = 60)
    private String cle;
    @Column(nullable = false, length = 120)
    private String valeur;
    @Column(length = 255)
    private String description;
}
