package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Notification;
import com.tovo.erp.contrat.model.NotificationStatut;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.OnboardingStatut;
import com.tovo.erp.contrat.model.OnboardingTache;
import com.tovo.erp.contrat.model.TacheStatut;
import com.tovo.erp.contrat.repository.NotificationRepository;
import com.tovo.erp.contrat.repository.OnboardingRepository;
import com.tovo.erp.contrat.repository.OnboardingTacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Genere les notifications internes d'alerte, de relance et d'escalade
 * (RG-B4-11, RG-B4-13 et RG-B4-14). Les notifications sont idempotentes :
 * un meme evenement n'est cree qu'une fois.
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final OnboardingRepository onboardings;
    private final OnboardingTacheRepository taches;

    public NotificationService(NotificationRepository notifications,
                               OnboardingRepository onboardings,
                               OnboardingTacheRepository taches) {
        this.notifications = notifications;
        this.onboardings = onboardings;
        this.taches = taches;
    }

    @Transactional
    public int genererDepuisAlertes(List<ContratDtos.AlerteDto> alertes) {
        int creees = 0;
        for (ContratDtos.AlerteDto alerte : alertes) {
            String role = destinataire(alerte.categorie());
            creees += creer(alerte.categorie(), alerte.niveau(), alerte.message(),
                    alerte.idContrat(), alerte.idEmploye(), null, role, alerte.echeance());
            if ("FIN_ESSAI".equals(alerte.categorie())) {
                creees += creer(alerte.categorie(), alerte.niveau(), alerte.message(),
                        alerte.idContrat(), alerte.idEmploye(), null, "MANAGER",
                        alerte.echeance());
            }
        }
        return creees;
    }

    @Transactional
    public int genererRelancesOnboarding() {
        LocalDate aujourdHui = LocalDate.now();
        int creees = 0;
        for (Onboarding onboarding : onboardings.findAll()) {
            if (onboarding.getStatut() == OnboardingStatut.TERMINE) {
                continue;
            }
            for (OnboardingTache tache : taches.findByOnboardingIdOrderById(onboarding.getId())) {
                if (tache.getEcheance() == null
                        || !tache.getEcheance().isBefore(aujourdHui)
                        || tache.getStatut() == TacheStatut.FAIT
                        || tache.getStatut() == TacheStatut.NON_APPLICABLE) {
                    continue;
                }
                long retard = ChronoUnit.DAYS.between(tache.getEcheance(), aujourdHui);
                String responsable = tache.getResponsable() == null
                        ? "RH" : tache.getResponsable();
                creees += creer("RELANCE_TACHE", "ATTENTION",
                        "Tache en retard : " + tache.getLibelle() + " (retard " + retard
                                + " jour(s), responsable " + responsable + ").",
                        onboarding.getIdContrat(), onboarding.getIdEmploye(), tache.getId(),
                        responsable, tache.getEcheance());
                if (retard >= 3) {
                    creees += creer("ESCALADE_TACHE", "DANGER",
                            "Tache en retard de " + retard + " jour(s) : " + tache.getLibelle()
                                    + ". Escalade a la RH.",
                            onboarding.getIdContrat(), onboarding.getIdEmploye(), tache.getId(),
                            "RH", tache.getEcheance());
                    if (tache.isBloquante()) {
                        creees += creer("ALERTE_TACHE_BLOQUANTE", "DANGER",
                                "Tache bloquante en retard : " + tache.getLibelle()
                                        + ". Alerte au Manager.",
                                onboarding.getIdContrat(), onboarding.getIdEmploye(),
                                tache.getId(), "MANAGER", tache.getEcheance());
                    }
                }
            }
        }
        return creees;
    }

    @Transactional(readOnly = true)
    public List<ContratDtos.NotificationDto> lister(String statut) {
        NotificationStatut filtre = null;
        if (statut != null && !statut.isBlank()) {
            try {
                filtre = NotificationStatut.valueOf(statut.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new com.tovo.erp.shared.BusinessException("STATUT_NOTIFICATION_INVALIDE",
                        "Statut de notification inconnu : " + statut);
            }
        }
        List<Notification> liste = filtre == null
                ? notifications.findAllByOrderByDateCreationDesc()
                : notifications.findByStatutOrderByDateCreationDesc(filtre);
        return liste.stream().map(ContratDtos.NotificationDto::de).toList();
    }

    @Transactional
    public ContratDtos.NotificationDto marquerLue(Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> com.tovo.erp.shared.BusinessException.notFound(
                        "La notification " + id + " n'existe pas."));
        notification.setStatut(NotificationStatut.LUE);
        notification.setDateLecture(java.time.LocalDateTime.now());
        return ContratDtos.NotificationDto.de(notifications.save(notification));
    }

    private int creer(String type, String niveau, String message, Long idContrat, Long idEmploye,
                      Long idTache, String role, LocalDate echeance) {
        if (notifications.existeEvenement(type, idContrat, idTache, role, echeance)) {
            return 0;
        }
        Notification notification = new Notification();
        notification.setType(type);
        notification.setNiveau(niveau);
        notification.setMessage(message);
        notification.setIdContrat(idContrat);
        notification.setIdEmploye(idEmploye);
        notification.setIdTache(idTache);
        notification.setDestinataireRole(role);
        notification.setEcheance(echeance);
        notification.setStatut(NotificationStatut.A_ENVOYER);
        notifications.save(notification);
        return 1;
    }

    private String destinataire(String categorie) {
        return switch (categorie) {
            case "FIN_ESSAI" -> "RH";
            case "ONBOARDING" -> "RH";
            case "VALIDATION" -> "RH";
            default -> "RH";
        };
    }
}
