package com.tovo.erp.conges.service;

import com.tovo.erp.conges.model.ParametreConge;
import com.tovo.erp.conges.repository.ParametreCongeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Parametres du bloc 7, avec valeurs de repli si la base n'est pas encore alimentee. */
@Service
public class ParametreCongeService {

    private final ParametreCongeRepository parametres;

    public ParametreCongeService(ParametreCongeRepository parametres) {
        this.parametres = parametres;
    }

    @Transactional(readOnly = true)
    public String texte(String cle, String defaut) {
        return parametres.findById(cle).map(ParametreConge::getValeur).orElse(defaut);
    }

    @Transactional(readOnly = true)
    public int entier(String cle, int defaut) {
        try {
            return Integer.parseInt(texte(cle, String.valueOf(defaut)).trim());
        } catch (NumberFormatException ex) {
            return defaut;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal decimal(String cle, BigDecimal defaut) {
        try {
            return new BigDecimal(texte(cle, defaut.toPlainString()).trim());
        } catch (NumberFormatException ex) {
            return defaut;
        }
    }
}
