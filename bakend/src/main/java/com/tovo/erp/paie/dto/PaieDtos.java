package com.tovo.erp.paie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PaieDtos {
    private PaieDtos() {}

    public record VariableCongeDto(
            Long idEmploye,
            LocalDate dateDebut,
            LocalDate dateFin,
            BigDecimal joursConges,
            String typeConge,
            String motif,
            BigDecimal salaireBase,
            BigDecimal montantConges,
            String devise,
            boolean contratDisponible) {}
}
