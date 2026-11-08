package com.tovo.erp.shared.reference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Generation des references sequentielles par annee.
 * <p>Le format produit est {@code PREFIXE-AAAA-NNNN}. L'increment est realise
 * dans la transaction appelante afin que la reference et l'objet metier soient
 * crees atomiquement.
 */
@Service
public class ReferenceService {

    private final CompteurReferenceRepository compteurs;

    public ReferenceService(CompteurReferenceRepository compteurs) {
        this.compteurs = compteurs;
    }

    @Transactional
    public String suivante(String type, String prefixe, int chiffres) {
        int annee = Year.now().getValue();
        CompteurReference compteur = compteurs.verrouiller(type, annee).orElseGet(() -> {
            CompteurReference nouveau = new CompteurReference();
            nouveau.setType(type);
            nouveau.setAnnee(annee);
            nouveau.setValeur(0);
            return nouveau;
        });
        compteur.setValeur(compteur.getValeur() + 1);
        compteurs.save(compteur);
        return String.format("%s-%d-%0" + chiffres + "d", prefixe, annee, compteur.getValeur());
    }
}
