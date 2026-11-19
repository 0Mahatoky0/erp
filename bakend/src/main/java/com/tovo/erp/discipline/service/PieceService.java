package com.tovo.erp.discipline.service;

import com.tovo.erp.discipline.dto.DisciplineDtos;
import com.tovo.erp.discipline.model.PieceDisciplinaire;
import com.tovo.erp.discipline.repository.IncidentRepository;
import com.tovo.erp.discipline.repository.PieceRepository;
import com.tovo.erp.discipline.repository.ProcedureRepository;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.UtilisateurCourant;
import com.tovo.erp.shared.audit.JournalAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PieceService {

    private static final Pattern HASH_SHA256 = Pattern.compile("[a-fA-F0-9]{64}");

    private final PieceRepository pieces;
    private final IncidentRepository incidents;
    private final ProcedureRepository procedures;
    private final UtilisateurCourant utilisateurCourant;
    private final JournalDisciplineService journal;
    private final JournalAuditService audit;
    private final AccesDisciplinaireService acces;

    public PieceService(PieceRepository pieces, IncidentRepository incidents,
                        ProcedureRepository procedures, UtilisateurCourant utilisateurCourant,
                        JournalDisciplineService journal, JournalAuditService audit,
                        AccesDisciplinaireService acces) {
        this.pieces = pieces;
        this.incidents = incidents;
        this.procedures = procedures;
        this.utilisateurCourant = utilisateurCourant;
        this.journal = journal;
        this.audit = audit;
        this.acces = acces;
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.PieceDto> listerIncident(Long idIncident, String motif) {
        acces.exigerAccesIncident(idIncident, motif);
        return pieces.findByIdIncidentOrderByDateAjoutAsc(idIncident).stream()
                .map(DisciplineDtos.PieceDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplineDtos.PieceDto> listerProcedure(Long idProcedure, String motif) {
        acces.exigerAccesProcedure(idProcedure,
                com.tovo.erp.discipline.model.NiveauAccesDisciplinaire.LECTURE_COMPLETE, motif);
        return pieces.findByIdProcedureOrderByDateAjoutAsc(idProcedure).stream()
                .map(DisciplineDtos.PieceDto::de).toList();
    }

    @Transactional
    public DisciplineDtos.PieceDto ajouter(DisciplineDtos.PieceRequest requete, String motif) {
        if ((requete.idIncident() == null) == (requete.idProcedure() == null)) {
            throw new BusinessException("PIESE_CIBLE_OBLIGATOIRE",
                    "Une piece doit etre rattachee a un incident ou a une procedure, pas aux deux.");
        }
        if (!HASH_SHA256.matcher(requete.hashSha256()).matches()) {
            throw new BusinessException("HASH_SHA256_INVALIDE",
                    "L'empreinte SHA-256 de la piece est invalide.");
        }
        if (requete.idIncident() != null) {
            if (incidents.findById(requete.idIncident()).isEmpty()) {
                throw BusinessException.notFound("Incident introuvable.");
            }
            acces.exigerAccesIncident(requete.idIncident(), motif);
        } else {
            if (procedures.findById(requete.idProcedure()).isEmpty()) {
                throw BusinessException.notFound("Procedure introuvable.");
            }
            acces.exigerAccesProcedure(requete.idProcedure(),
                    com.tovo.erp.discipline.model.NiveauAccesDisciplinaire.ECRITURE, motif);
        }
        PieceDisciplinaire piece = new PieceDisciplinaire();
        piece.setIdIncident(requete.idIncident());
        piece.setIdProcedure(requete.idProcedure());
        piece.setType(requete.type());
        piece.setFichier(requete.fichier());
        piece.setHashSha256(requete.hashSha256());
        piece.setVersion(versionSuivante(requete));
        piece.setAuteurId(utilisateurCourant.exiger().getId());
        piece.setDateAjout(LocalDateTime.now());
        PieceDisciplinaire creee = pieces.save(piece);
        audit.enregistrer("PIECE_DISCIPLINAIRE_AJOUTEE", "piece_disciplinaire", creee.getId(),
                null, requete.type().name(), motif, null);
        journal.enregistrer(utilisateurCourant.exiger().getId(), "PIECE_AJOUTEE",
                requete.idProcedure() == null ? "incident" : "procedure",
                requete.idProcedure() == null ? requete.idIncident() : requete.idProcedure(),
                motif, requete.type().name());
        return DisciplineDtos.PieceDto.de(creee);
    }

    private int versionSuivante(DisciplineDtos.PieceRequest requete) {
        List<PieceDisciplinaire> existantes = requete.idIncident() != null
                ? pieces.findByIdIncidentOrderByDateAjoutAsc(requete.idIncident())
                : pieces.findByIdProcedureOrderByDateAjoutAsc(requete.idProcedure());
        return existantes.stream().mapToInt(PieceDisciplinaire::getVersion).max().orElse(0) + 1;
    }
}
