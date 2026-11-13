package com.tovo.erp.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trace transversale des consultations et modifications de donnees sensibles
 * (dossier salarie, salaire, contrat, validations) — ARCHITECTURE_BAKEND §7.
 */
@Entity
@Table(name = "journal_audit")
@Getter
@Setter
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Long id;

    @Column(name = "id_utilisateur")
    private Long idUtilisateur;

    @Column(name = "role_utilisateur", length = 40)
    private String roleUtilisateur;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "ressource", nullable = false, length = 120)
    private String ressource;

    @Column(name = "id_ressource")
    private Long idRessource;

    @Column(name = "avant", columnDefinition = "TEXT")
    private String avant;

    @Column(name = "apres", columnDefinition = "TEXT")
    private String apres;

    @Column(name = "adresse_ip", length = 64)
    private String adresseIp;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }
}
