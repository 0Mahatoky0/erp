package com.tovo.erp.employe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Vue SQL en lecture seule des informations de poste utiles au dossier salarie.
 *
 * <p>Le module employe ne depend pas du module besoin : il lit uniquement une
 * vue de reference, sans relation JPA inter-modules.
 */
@Entity
@Immutable
@Table(name = "vue_employe_poste_detail")
@Getter
public class PosteDetailVue {

    @Id
    @Column(name = "id_poste")
    private Long id;

    @Column(name = "intitule")
    private String intitule;

    @Column(name = "id_service")
    private Long idService;

    @Column(name = "service_nom")
    private String serviceNom;

    @Column(name = "id_departement")
    private Long idDepartement;

    @Column(name = "departement_nom")
    private String departementNom;
}
