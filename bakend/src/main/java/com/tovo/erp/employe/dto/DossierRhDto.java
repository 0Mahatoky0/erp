package com.tovo.erp.employe.dto;

import com.tovo.erp.employe.dto.DemandeModificationDtos.DemandeModificationDto;
import com.tovo.erp.employe.dto.MissionDtos.MissionDto;

import java.util.List;

/**
 * Vue RH agregee du dossier administratif. Les contrats, affectations et
 * salaires restent dans le module {@code contrat} : le front assemble la fiche
 * complete en appelant les deux API.
 */
public record DossierRhDto(
        EmployeEmbaucheDto employe,
        List<DocumentEmbaucheDto> documents,
        List<AlerteDto> alertes,
        List<MissionDto> missions,
        List<DemandeModificationDto> demandesModification,
        int completude,
        List<String> piecesManquantes) {
}
