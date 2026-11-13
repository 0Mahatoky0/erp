package com.tovo.erp.employe.service;

import com.tovo.erp.employe.dto.MissionDtos.CreationMissionRequest;
import com.tovo.erp.employe.dto.MissionDtos.MissionDto;
import com.tovo.erp.employe.dto.MissionDtos.ModificationMissionRequest;
import com.tovo.erp.employe.dto.MissionDtos.SuiviDto;
import com.tovo.erp.employe.dto.MissionDtos.SuiviRequest;
import com.tovo.erp.employe.model.Mission;
import com.tovo.erp.employe.model.PrioriteMission;
import com.tovo.erp.employe.model.StatutMission;
import com.tovo.erp.employe.model.SuiviMission;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.employe.repository.MissionRepository;
import com.tovo.erp.employe.repository.SuiviMissionRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Gestion des missions confiees aux salaries et de leur suivi (bloc 5). */
@Service
public class MissionService {

    private final MissionRepository missions;
    private final SuiviMissionRepository suivis;
    private final EmployeRepository employes;

    public MissionService(MissionRepository missions,
                          SuiviMissionRepository suivis,
                          EmployeRepository employes) {
        this.missions = missions;
        this.suivis = suivis;
        this.employes = employes;
    }

    @Transactional(readOnly = true)
    public List<MissionDto> lister(Long idEmploye) {
        exigerEmploye(idEmploye);
        return missions.findByIdEmployeOrderByDateDebutDesc(idEmploye).stream()
                .map(MissionDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuiviDto> suivis(Long idMission) {
        exigerMission(idMission);
        return suivis.findByIdMissionOrderByDateSuiviDesc(idMission).stream()
                .map(SuiviDto::de)
                .toList();
    }

    @Transactional
    public MissionDto creer(Long idEmploye, CreationMissionRequest demande) {
        exigerEmploye(idEmploye);
        verifierDates(demande.dateDebut(), demande.dateFinPrevue(), null);
        Mission mission = new Mission();
        mission.setIdEmploye(idEmploye);
        mission.setTitre(demande.titre().trim());
        mission.setDescription(demande.description());
        mission.setLieu(demande.lieu());
        mission.setDateDebut(demande.dateDebut());
        mission.setDateFinPrevue(demande.dateFinPrevue());
        mission.setPriorite(demande.priorite() == null ? PrioriteMission.NORMALE : demande.priorite());
        mission.setStatut(StatutMission.PLANIFIEE);
        mission.setIdResponsable(demande.idResponsable());
        mission.setAvancement(0);
        Mission enregistree = missions.save(mission);
        enregistree.setReference("M-" + enregistree.getDateDebut().getYear() + "-"
                + String.format("%05d", enregistree.getId()));
        return MissionDto.de(missions.save(enregistree));
    }

    @Transactional
    public MissionDto modifier(Long idMission, ModificationMissionRequest demande) {
        Mission mission = exigerMission(idMission);
        verifierDates(demande.dateDebut(), demande.dateFinPrevue(), demande.dateFinReelle());
        mission.setTitre(demande.titre().trim());
        mission.setDescription(demande.description());
        mission.setLieu(demande.lieu());
        mission.setDateDebut(demande.dateDebut());
        mission.setDateFinPrevue(demande.dateFinPrevue());
        mission.setDateFinReelle(demande.dateFinReelle());
        if (demande.priorite() != null) {
            mission.setPriorite(demande.priorite());
        }
        if (demande.statut() != null) {
            mission.setStatut(demande.statut());
        }
        if (demande.idResponsable() != null) {
            mission.setIdResponsable(demande.idResponsable());
        }
        if (demande.avancement() != null) {
            mission.setAvancement(demande.avancement());
            if (demande.avancement() >= 100 && demande.statut() == null) {
                mission.setStatut(StatutMission.TERMINEE);
            }
        }
        if (demande.manaoAsa() != null) {
            mission.setManaoAsa(demande.manaoAsa());
        }
        if (demande.miassaTsara() != null) {
            mission.setMiassaTsara(demande.miassaTsara());
        }
        if (demande.commentaireSuivi() != null) {
            mission.setCommentaireSuivi(demande.commentaireSuivi());
        }
        if (mission.getStatut() == StatutMission.TERMINEE && mission.getDateFinReelle() == null) {
            mission.setDateFinReelle(LocalDate.now());
        }
        return MissionDto.de(missions.save(mission));
    }

    @Transactional
    public SuiviDto ajouterSuivi(Long idMission, SuiviRequest demande, Long idAuteur) {
        Mission mission = exigerMission(idMission);
        SuiviMission suivi = new SuiviMission();
        suivi.setIdMission(idMission);
        suivi.setAvancement(demande.avancement());
        suivi.setManaoAsa(demande.manaoAsa());
        suivi.setMiassaTsara(demande.miassaTsara());
        suivi.setCommentaire(demande.commentaire());
        suivi.setIdAuteur(idAuteur);
        SuiviMission enregistre = suivis.save(suivi);
        if (demande.avancement() != null) {
            mission.setAvancement(demande.avancement());
            if (demande.avancement() >= 100) {
                mission.setStatut(StatutMission.TERMINEE);
                if (mission.getDateFinReelle() == null) {
                    mission.setDateFinReelle(LocalDate.now());
                }
            } else if (demande.avancement() > 0) {
                mission.setStatut(StatutMission.EN_COURS);
            }
        }
        if (demande.manaoAsa() != null) {
            mission.setManaoAsa(demande.manaoAsa());
        }
        if (demande.miassaTsara() != null) {
            mission.setMiassaTsara(demande.miassaTsara());
        }
        if (demande.commentaire() != null) {
            mission.setCommentaireSuivi(demande.commentaire());
        }
        missions.save(mission);
        return SuiviDto.de(enregistre);
    }

    @Transactional(readOnly = true)
    public List<Mission> missionsEnRetard() {
        return missions.findByStatutNotAndDateFinPrevueBefore(StatutMission.TERMINEE, LocalDate.now())
                .stream()
                .filter(mission -> mission.getStatut() != StatutMission.ANNULEE)
                .toList();
    }

    private void verifierDates(LocalDate debut, LocalDate finPrevue, LocalDate finReelle) {
        if (debut == null) {
            throw new BusinessException("MISSION_DATE_INVALIDE", "La date de debut est obligatoire.");
        }
        if (finPrevue != null && finPrevue.isBefore(debut)) {
            throw new BusinessException("MISSION_DATE_INVALIDE",
                    "La date de fin prevue ne peut pas preceder le debut.");
        }
        if (finReelle != null && finReelle.isBefore(debut)) {
            throw new BusinessException("MISSION_DATE_INVALIDE",
                    "La date de fin reelle ne peut pas preceder le debut.");
        }
    }

    private Mission exigerMission(Long idMission) {
        return missions.findById(idMission)
                .orElseThrow(() -> BusinessException.notFound("La mission " + idMission + " n'existe pas."));
    }

    private void exigerEmploye(Long idEmploye) {
        if (idEmploye == null || !employes.existsById(idEmploye)) {
            throw BusinessException.notFound("L'employe " + idEmploye + " n'existe pas.");
        }
    }
}
