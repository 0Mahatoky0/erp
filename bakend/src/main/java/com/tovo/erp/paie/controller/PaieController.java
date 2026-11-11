package com.tovo.erp.paie.controller;

import com.tovo.erp.paie.dto.PaieDtos;
import com.tovo.erp.paie.service.PaieService;
import com.tovo.erp.shared.BusinessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/paie")
public class PaieController {
    private final PaieService service;

    public PaieController(PaieService service) {
        this.service = service;
    }

    @GetMapping("/variables")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
    public List<PaieDtos.VariableCongeDto> variables(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate aujourdHui = LocalDate.now();
        LocalDate periodeDebut = debut == null ? aujourdHui.withDayOfMonth(1) : debut;
        LocalDate periodeFin = fin == null ? aujourdHui.withDayOfMonth(1).plusMonths(1).minusDays(1) : fin;
        if (periodeFin.isBefore(periodeDebut)) {
            throw new BusinessException("PERIODE_INVALIDE",
                    "La fin de période doit être postérieure ou égale au début.");
        }
        return service.variables(periodeDebut, periodeFin);
    }
}
