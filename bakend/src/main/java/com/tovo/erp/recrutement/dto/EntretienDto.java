package com.tovo.erp.recrutement.dto;

import com.tovo.erp.recrutement.model.*;
import java.time.LocalDateTime;

public record EntretienDto(Long idEntretien, Long idCandidature, Long idUtilisateurOrganisateur,
                           TypeEntretien typeEntretien, LocalDateTime dateDebut, LocalDateTime dateFin,
                           String lieu, String lienVisio, EntretienStatut statut, String commentaire) {
    public static EntretienDto de(Entretien e) {
        return new EntretienDto(e.getIdEntretien(), e.getCandidature().getIdCandidature(),
                e.getIdUtilisateurOrganisateur(), e.getTypeEntretien(), e.getDateDebut(), e.getDateFin(),
                e.getLieu(), e.getLienVisio(), e.getStatut(), e.getCommentaire());
    }
}
