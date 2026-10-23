package com.tovo.erp.config;

import com.tovo.erp.domain.enums.EtapeValidation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuration du circuit de validation (RM-034).
 *
 * @param etapes          etapes obligatoires, dans l'ordre. Par exemple RH,FINANCE,DIRECTION
 * @param seuilDirection  montant de budget au-dela duquel l'accord de la direction est requis.
 *                        A 0, la direction est systematique.
 */
@ConfigurationProperties(prefix = "erp.validation")
public record CircuitValidationProperties(List<String> etapes, BigDecimal seuilDirection) {

    public CircuitValidationProperties {
        if (etapes == null || etapes.isEmpty()) {
            etapes = List.of("RH", "FINANCE", "DIRECTION");
        }
        if (seuilDirection == null) {
            seuilDirection = BigDecimal.ZERO;
        }
    }

    /** Liste des etapes configurees, converties en enums. */
    public List<EtapeValidation> etapesEnum() {
        return etapes.stream()
                .map(EtapeValidation::valueOf)
                .toList();
    }
}
