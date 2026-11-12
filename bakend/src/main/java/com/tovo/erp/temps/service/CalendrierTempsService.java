package com.tovo.erp.temps.service;

import com.tovo.erp.temps.repository.JourFerieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Calendrier commun (jours feries) expose aux modules du dessus. */
@Service
public class CalendrierTempsService {

    private final JourFerieRepository joursFeries;

    public CalendrierTempsService(JourFerieRepository joursFeries) {
        this.joursFeries = joursFeries;
    }

    @Transactional(readOnly = true)
    public boolean estFerie(LocalDate date) {
        if (joursFeries.findByDate(date).isPresent()) {
            return true;
        }
        return joursFeries.findByRecurrentTrue().stream()
                .anyMatch(j -> j.getDate().getMonthValue() == date.getMonthValue()
                        && j.getDate().getDayOfMonth() == date.getDayOfMonth());
    }

    @Transactional(readOnly = true)
    public List<LocalDate> joursFeries(LocalDate debut, LocalDate fin) {
        return joursFeries.findByDateBetweenOrderByDateAsc(debut, fin).stream()
                .map(j -> j.getDate())
                .toList();
    }
}
