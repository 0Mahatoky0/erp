package com.tovo.erp.temps.service;

import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.temps.dto.TempsDtos;
import com.tovo.erp.temps.model.PointageBrut;
import com.tovo.erp.temps.repository.PointageBrutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ingestion des badgeages bruts et appariement entree/sortie (RG-B6-05 a 07, 21).
 * Les lignes sont immuables : une correction est un nouveau badgeage MANUEL.
 */
@Service
public class PointageBrutService {

    private static final int FENETRE_DOUBLON_MINUTES = 2;

    private final PointageBrutRepository pointagesBruts;

    public PointageBrutService(PointageBrutRepository pointagesBruts) {
        this.pointagesBruts = pointagesBruts;
    }

    @Transactional
    public TempsDtos.PointageBrutDto enregistrer(TempsDtos.PointageBrutRequest demande, Long idUtilisateur) {
        PointageBrut.Source source = parserSource(demande.source());
        PointageBrut.Sens sens = parserSens(demande.sens());
        boolean manuel = source == PointageBrut.Source.MANUEL;
        if (manuel) {
            if (demande.motif() == null || demande.motif().isBlank() || idUtilisateur == null) {
                throw new BusinessException("CORRECTION_NON_JUSTIFIEE",
                        "Une correction manuelle exige un motif et un utilisateur.");
            }
        } else if (source != PointageBrut.Source.WEB && source != PointageBrut.Source.MOBILE
                && (demande.terminalId() == null || demande.terminalId().isBlank()
                || !Boolean.TRUE.equals(demande.terminalAuthentifie()))) {
            throw new BusinessException("TERMINAL_NON_AUTHENTIFIE",
                    "Le terminal doit etre identifie et authentifie.");
        }
        if (demande.idPointageBrutRemplace() != null
                && !pointagesBruts.existsById(demande.idPointageBrutRemplace())) {
            throw new BusinessException("POINTAGE_REMPLACE_INTROUVABLE",
                    "Le pointage brut a remplacer est introuvable.");
        }
        if (demande.latitude() != null && demande.latitude().abs().compareTo(java.math.BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException("COORDONNEES_INVALIDES", "Latitude invalide.");
        }
        if (demande.longitude() != null && demande.longitude().abs().compareTo(java.math.BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException("COORDONNEES_INVALIDES", "Longitude invalide.");
        }

        PointageBrut pointage = new PointageBrut();
        pointage.setIdEmploye(demande.idEmploye());
        pointage.setHorodatage(demande.horodatage());
        pointage.setSens(sens);
        pointage.setSource(source);
        pointage.setTerminalId(demande.terminalId());
        pointage.setTerminalAuthentifie(Boolean.TRUE.equals(demande.terminalAuthentifie()));
        pointage.setLieuId(demande.lieuId());
        pointage.setLatitude(demande.latitude());
        pointage.setLongitude(demande.longitude());
        pointage.setSaisiPar(manuel ? idUtilisateur : null);
        pointage.setMotif(demande.motif());
        pointage.setIdPointageBrutRemplace(demande.idPointageBrutRemplace());
        pointage.setStatut(PointageBrut.Statut.VALIDE);
        return TempsDtos.PointageBrutDto.de(pointagesBruts.save(pointage));
    }

    @Transactional(readOnly = true)
    public List<TempsDtos.PointageBrutDto> evenements(Long idEmploye, LocalDate date) {
        return evenementsDeLaFenetre(idEmploye, date).stream()
                .map(TempsDtos.PointageBrutDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TempsDtos.PointageBrutJourDto syntheseJour(Long idEmploye, LocalDate date) {
        LocalDate jour = date == null ? LocalDate.now() : date;
        List<PointageBrut> evenements = evenementsDeLaFenetre(idEmploye, jour).stream()
                .filter(e -> e.getStatut() != PointageBrut.Statut.REJETE)
                .toList();
        List<String> anomalies = new ArrayList<>();
        List<TempsDtos.PairePointageDto> paires = new ArrayList<>();
        PointageBrut ouverte = null;
        PointageBrut dernierAccepte = null;
        int total = 0;

        for (PointageBrut evenement : evenements) {
            if (evenement.getLieuId() == null && evenement.getLatitude() == null
                    && evenement.getSource() != PointageBrut.Source.WEB
                    && evenement.getSource() != PointageBrut.Source.MOBILE) {
                anomalies.add("LIEU_INCOHERENT");
            }
            PointageBrut.Sens sens = evenement.getSens() == PointageBrut.Sens.INDETERMINE
                    ? (ouverte == null ? PointageBrut.Sens.ENTREE : PointageBrut.Sens.SORTIE)
                    : evenement.getSens();
            if (dernierAccepte != null && sens == dernierAccepte.getSens()
                    && Math.abs(Duration.between(dernierAccepte.getHorodatage(),
                    evenement.getHorodatage()).toMinutes()) < FENETRE_DOUBLON_MINUTES) {
                anomalies.add("DOUBLON_IGNORE");
                continue;
            }
            if (sens == PointageBrut.Sens.ENTREE) {
                if (ouverte != null) {
                    anomalies.add("SEQUENCE_INVALIDE");
                    continue;
                }
                ouverte = evenement;
            } else {
                if (ouverte == null) {
                    anomalies.add("SORTIE_SANS_ENTREE");
                    continue;
                }
                int minutes = (int) Math.max(0,
                        Duration.between(ouverte.getHorodatage(), evenement.getHorodatage()).toMinutes());
                LocalDate jourEntree = ouverte.getHorodatage().toLocalDate();
                boolean appartientAuJour = jourEntree.equals(jour)
                        || (jourEntree.equals(jour.minusDays(1))
                        && evenement.getHorodatage().toLocalDate().equals(jour));
                if (appartientAuJour) {
                    paires.add(new TempsDtos.PairePointageDto(ouverte.getHorodatage(),
                            evenement.getHorodatage(), minutes, List.of()));
                    total += minutes;
                }
                ouverte = null;
            }
            dernierAccepte = evenement;
        }
        if (ouverte != null) {
            anomalies.add("SORTIE_MANQUANTE");
        }
        return new TempsDtos.PointageBrutJourDto(idEmploye, jour,
                evenements.stream().map(TempsDtos.PointageBrutDto::de).toList(),
                paires, total, anomalies);
    }

    private List<PointageBrut> evenementsDeLaFenetre(Long idEmploye, LocalDate date) {
        LocalDate jour = date == null ? LocalDate.now() : date;
        LocalDateTime debut = jour.atStartOfDay().minusHours(12);
        LocalDateTime fin = jour.plusDays(1).atStartOfDay().plusHours(12);
        return pointagesBruts.findByIdEmployeAndHorodatageBetweenOrderByHorodatageAsc(idEmploye, debut, fin);
    }

    private static PointageBrut.Source parserSource(String source) {
        try {
            return PointageBrut.Source.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("SOURCE_POINTAGE_INVALIDE", "Source de pointage invalide.");
        }
    }

    private static PointageBrut.Sens parserSens(String sens) {
        if (sens == null || sens.isBlank()) {
            return PointageBrut.Sens.INDETERMINE;
        }
        try {
            return PointageBrut.Sens.valueOf(sens.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("SENS_POINTAGE_INVALIDE", "Sens de pointage invalide.");
        }
    }
}
