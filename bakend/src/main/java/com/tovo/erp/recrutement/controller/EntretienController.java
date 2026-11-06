package com.tovo.erp.recrutement.controller;

import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.recrutement.dto.EntretienDto;
import com.tovo.erp.recrutement.dto.RecrutementRequests;
import com.tovo.erp.recrutement.model.Entretien;
import com.tovo.erp.recrutement.repository.CandidatureRepository;
import com.tovo.erp.recrutement.repository.EntretienRepository;
import com.tovo.erp.recrutement.service.EntretienTestService;
import com.tovo.erp.shared.UtilisateurCourant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
public class EntretienController {

    private final EntretienTestService entretienTests;
    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurCourant utilisateurCourant;

    public EntretienController(EntretienTestService entretienTests, EntretienRepository entretienRepository,
                               CandidatureRepository candidatureRepository, UtilisateurCourant utilisateurCourant) {
        this.entretienTests = entretienTests;
        this.entretienRepository = entretienRepository;
        this.candidatureRepository = candidatureRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienDto> planifier(@Valid @RequestBody RecrutementRequests.EntretienRequest r) {
        utilisateurCourant.exigerRole(RoleNom.RH, RoleNom.DIRECTION);
        Entretien e = new Entretien();
        e.setCandidature(candidatureRepository.getReferenceById(r.idCandidature()));
        e.setIdUtilisateurOrganisateur(utilisateurCourant.exiger().getId());
        e.setTypeEntretien(r.typeEntretien());
        e.setDateDebut(r.dateDebut());
        e.setDateFin(r.dateFin());
        e.setLieu(r.lieu());
        e.setLienVisio(r.lienVisio());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntretienDto.de(entretienTests.planifierEntretien(e)));
    }

    @GetMapping("/candidatures/{id}/entretiens")
    public List<EntretienDto> parCandidature(@PathVariable Long id) {
        return entretienRepository.findByCandidatureIdCandidature(id).stream().map(EntretienDto::de).toList();
    }
}
