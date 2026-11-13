package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos.JourFerieDto;
import com.tovo.erp.temps.dto.TempsDtos.JourFerieRequest;
import com.tovo.erp.temps.model.JourFerie;
import com.tovo.erp.temps.repository.JourFerieRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Calendrier des jours feries utilise par le decompte des conges (blocs 6/7). */
@Service
public class JourFerieService {

    private final JourFerieRepository joursFeries;

    public JourFerieService(JourFerieRepository joursFeries) {
        this.joursFeries = joursFeries;
    }

    @Transactional(readOnly = true)
    public List<JourFerieDto> lister(Integer annee) {
        List<JourFerie> jours = joursFeries.findAll(Sort.by("date"));
        if (annee == null) {
            return jours.stream().map(JourFerieDto::de).toList();
        }
        return jours.stream()
                .filter(jour -> jour.isRecurrent() || jour.getDate().getYear() == annee)
                .map(JourFerieDto::de)
                .toList();
    }

    @Transactional
    public JourFerieDto creer(JourFerieRequest requete) {
        if (joursFeries.findByDate(requete.date()).isPresent()) {
            throw new BusinessException("JOUR_FERIE_EXISTANT",
                    "Un jour ferie existe deja a la date " + requete.date() + ".");
        }
        JourFerie jour = new JourFerie();
        jour.setDate(requete.date());
        jour.setLibelle(requete.libelle().trim());
        jour.setRecurrent(requete.recurrent());
        return JourFerieDto.de(joursFeries.save(jour));
    }

    @Transactional
    public void supprimer(Long id) {
        if (!joursFeries.existsById(id)) {
            throw BusinessException.notFound("Le jour ferie " + id + " n'existe pas.");
        }
        joursFeries.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean estFerie(LocalDate date) {
        return joursFeries.findByDate(date)
                .or(() -> joursFeries.findByRecurrentTrue().stream()
                        .filter(jour -> jour.getDate().getMonth() == date.getMonth()
                                && jour.getDate().getDayOfMonth() == date.getDayOfMonth())
                        .findFirst())
                .isPresent();
    }
}
