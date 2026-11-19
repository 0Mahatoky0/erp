package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.AccesDisciplinaire;
import com.tovo.erp.discipline.model.NiveauAccesDisciplinaire;
import com.tovo.erp.discipline.model.ProcedureDisciplinaire;
import com.tovo.erp.discipline.repository.AccesRepository;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.Roles;
import com.tovo.erp.shared.UtilisateurCourant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccesDisciplinaireService {

    private final AccesRepository acces;
    private final ProcedureRepository procedures;
    private final EmployeService employes;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalDisciplineService journal;

    public AccesDisciplinaireService(AccesRepository acces, ProcedureRepository procedures,
                                     EmployeService employes, UtilisateurCourant utilisateurCourant,
                                     JournalDisciplineService journal) {
        this.acces = acces;
        this.procedures = procedures;
        this.employes = employes;
        this.utilisateurCourant = utilisateurCourant;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.AccesDto> lister(Long idProcedure) {
        return acces.findByIdProcedureAndActifTrue(idProcedure).stream().map(this::enDto).toList();
    }

    @Transactional
    public DisciplineDtos.AccesDto accorder(DisciplineDtos.AccesRequest requete) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(requete.motif());
        AccesDisciplinaire accesDisciplinaire = new AccesDisciplinaire();
        accesDisciplinaire.setIdIncident(requete.idIncident());
        accesDisciplinaire.setIdProcedure(requete.idProcedure());
        accesDisciplinaire.setUtilisateurId(requete.utilisateurId());
        accesDisciplinaire.setNiveau(requete.niveau());
        accesDisciplinaire.setMotif(requete.motif());
        accesDisciplinaire.setAccordeParId(utilisateurCourant.exiger().getId());
        accesDisciplinaire.setDateDebut(LocalDateTime.now());
        accesDisciplinaire.setDateFin(requete.dateFin());
        DisciplineDtos.AccesDto resultat = enDto(acces.save(accesDisciplinaire));
        journal.enregistrer(utilisateurCourant.exiger().getId(), "ACCES_ACCORDE", "procedure",
                requete.idProcedure(), requete.motif(), requete.niveau().name());
        return resultat;
    }

    @Transactional
    public void revoquer(Long idAcces, String motif) {
        utilisateurCourant.exigerRole(Roles.RH, Roles.DIRECTION);
        journal.exigerMotif(motif);
        AccesDisciplinaire accesDisciplinaire = acces.findById(idAcces)
                .orElseThrow(() -> BusinessException.notFound("Habilitation introuvable."));
        accesDisciplinaire.setActif(false);
        accesDisciplinaire.setDateFin(LocalDateTime.now());
        acces.save(accesDisciplinaire);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "ACCES_REVOQUE", "procedure",
                accesDisciplinaire.getIdProcedure(), motif, String.valueOf(idAcces));
    }

    public void accorderParDefaut(Long idIncident, Long idProcedure, Long instructeurId, Long decideurId) {
        accorderInterne(idIncident, idProcedure, instructeurId, NiveauAccesDisciplinaire.ECRITURE,
                "Instructeur désigné");
        accorderInterne(idIncident, idProcedure, decideurId, NiveauAccesDisciplinaire.LECTURE_COMPLETE,
                "Décideur désigné");
        Long responsableRh = utilisateurCourant.exiger().getId();
        if (!responsableRh.equals(instructeurId) && !responsableRh.equals(decideurId)) {
            accorderInterne(idIncident, idProcedure, responsableRh,
                    NiveauAccesDisciplinaire.ECRITURE, "Responsable RH");
        }
    }

    public void exigerAccesProcedure(Long idProcedure, NiveauAccesDisciplinaire niveauRequis, String motif) {
        journal.exigerMotif(motif);
        var acteur = utilisateurCourant.exiger();
        ProcedureDisciplinaire procedure = procedures.findById(idProcedure)
                .orElseThrow(() -> BusinessException.notFound("Procédure introuvable."));
        boolean estSalarieConcerne = employes.idParUtilisateur(acteur.getId())
                .filter(procedure.getIdEmploye()::equals).isPresent();
        if (estSalarieConcerne && niveauRequis == NiveauAccesDisciplinaire.LECTURE_RESUMEE) {
            journal.enregistrer(acteur.getId(), "CONSULTATION", "procedure", idProcedure, motif, "PROPRE_DOSSIER");
            return;
        }
        boolean autorise = acces.findByIdProcedureAndActifTrue(idProcedure).stream()
                .filter(a -> a.getUtilisateurId().equals(acteur.getId()))
                .filter(a -> a.getDateFin() == null || !a.getDateFin().isBefore(LocalDateTime.now()))
                .anyMatch(a -> niveau(a.getNiveau()) >= niveau(niveauRequis));
        if (!autorise) {
            journal.enregistrer(acteur.getId(), "ACCES_REFUSE", "procedure", idProcedure, motif,
                    niveauRequis.name());
            throw BusinessException.forbidden("Accès nominatif insuffisant pour ce dossier disciplinaire.");
        }
        journal.enregistrer(acteur.getId(), "CONSULTATION", "procedure", idProcedure, motif,
                niveauRequis.name());
    }

    public void exigerAccesIncident(Long idIncident, String motif) {
        journal.exigerMotif(motif);
        var acteur = utilisateurCourant.exiger();
        boolean autorise = utilisateurCourant.aLeRole(Roles.RH, Roles.DIRECTION)
                || acces.findByIdIncidentAndActifTrue(idIncident).stream()
                .anyMatch(a -> a.getUtilisateurId().equals(acteur.getId()));
        if (!autorise) {
            journal.enregistrer(acteur.getId(), "ACCES_REFUSE", "incident", idIncident, motif, "LECTURE");
            throw BusinessException.forbidden("Accès nominatif insuffisant pour cet incident.");
        }
        journal.enregistrer(acteur.getId(), "CONSULTATION", "incident", idIncident, motif, "LECTURE");
    }

    private void accorderInterne(Long idIncident, Long idProcedure, Long utilisateurId,
                                 NiveauAccesDisciplinaire niveau, String motif) {
        AccesDisciplinaire accesDisciplinaire = new AccesDisciplinaire();
        accesDisciplinaire.setIdIncident(idIncident);
        accesDisciplinaire.setIdProcedure(idProcedure);
        accesDisciplinaire.setUtilisateurId(utilisateurId);
        accesDisciplinaire.setNiveau(niveau);
        accesDisciplinaire.setMotif(motif);
        accesDisciplinaire.setAccordeParId(utilisateurCourant.exiger().getId());
        accesDisciplinaire.setDateDebut(LocalDateTime.now());
        acces.save(accesDisciplinaire);
    }

    private int niveau(NiveauAccesDisciplinaire niveau) {
        return switch (niveau) {
            case LECTURE_RESUMEE -> 1;
            case LECTURE_COMPLETE -> 2;
            case ECRITURE -> 3;
        };
    }

    private DisciplineDtos.AccesDto enDto(AccesDisciplinaire a) {
        return new DisciplineDtos.AccesDto(a.getId(), a.getIdIncident(), a.getIdProcedure(),
                a.getUtilisateurId(), a.getNiveau(), a.getMotif(), a.getAccordeParId(),
                a.getDateDebut(), a.getDateFin(), a.isActif());
    }
}
