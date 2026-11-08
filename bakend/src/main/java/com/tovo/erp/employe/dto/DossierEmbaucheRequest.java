package com.tovo.erp.employe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Informations minimales du dossier salarie cree a l'acceptation (RG-B3-07). */
public record DossierEmbaucheRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @Email String email,
        String telephone,
        String cin,
        LocalDate dateNaissance,
        String lieuNaissance,
        String adresse,
        String nationalite,
        String situationFamiliale,
        LocalDate datePremiereEmbauche) {
}
