package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.dto.ContratDtos;
import com.tovo.erp.contrat.model.Affectation;
import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.ContratEtat;
import com.tovo.erp.contrat.model.Onboarding;
import com.tovo.erp.contrat.model.StatutExecutionContrat;
import com.tovo.erp.contrat.model.TypeContrat;
import com.tovo.erp.contrat.repository.AffectationRepository;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.contrat.repository.OnboardingRepository;
import com.tovo.erp.employe.dto.DocumentEmbaucheDto;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.employe.model.TypeDocumentEmploye;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.shared.BusinessException;
import com.tovo.erp.shared.reference.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratServiceTest {

    @Mock private ContratRepository contrats;
    @Mock private AffectationRepository affectations;
    @Mock private OnboardingRepository onboardings;
    @Mock private OnboardingService onboardingService;
    @Mock private EmployeService employes;
    @Mock private ReferenceService references;
    @Mock private ContratPdfService pdf;

    private ContratService service;

    @BeforeEach
    void setUp() {
        service = new ContratService(contrats, affectations, onboardings, onboardingService,
                employes, references, pdf,
                new ContratProperties(24, 1, 30, 3, 3, 30, 15, 7, 15, 7));
    }

    @Test
    void laSignatureAttribueLeMatriculeEtCreeLOnboarding() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.CREE,
                LocalDate.now().plusDays(7), null);
        contrat.setStatutExecution(StatutExecutionContrat.A_VENIR);
        when(contrats.findById(5L)).thenReturn(Optional.of(contrat));
        when(employes.attribuerMatricule(eq(2L), any())).thenReturn("MAT2026000042");
        when(onboardingService.creerPour(eq(contrat), eq(9L))).thenReturn(new Onboarding());
        when(employes.dossier(2L)).thenReturn(salarie());
        when(pdf.generer(any(), any(), any(), any(), any(), any()))
                .thenReturn("%PDF-1.4 contenu".getBytes());
        when(employes.enregistrerDocument(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(document());

        ContratDtos.ContratDto resultat = service.signer(5L,
                new ContratDtos.SignatureRequest(9L));

        assertEquals(ContratEtat.SIGNE, resultat.etat());
        verify(employes).attribuerMatricule(eq(2L), any());
        verify(onboardingService).creerPour(contrat, 9L);
    }

    @Test
    void laValidationEstBloqueeParUneTacheObligatoireNonFaite() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.SIGNE,
                LocalDate.now().plusDays(7), null);
        when(contrats.findById(5L)).thenReturn(Optional.of(contrat));
        when(onboardingService.tachesBloquantesNonFaites(5L)).thenReturn(List.of(
                new ContratDtos.TacheDto(1L, "ADMIN_DOSSIER", "Dossier administratif",
                        null, "RH", LocalDate.now(), true, true, null, null, null, null)));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.valider(5L));

        assertEquals("ONBOARDING_BLOQUANT", erreur.getCode());
        assertEquals(ContratEtat.SIGNE, contrat.getEtat());
        verify(contrats, never()).save(contrat);
    }

    @Test
    void laValidationActiveLeContratDontLaDateEstArrivee() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.SIGNE,
                LocalDate.now(), null);
        when(contrats.findById(5L)).thenReturn(Optional.of(contrat));
        when(onboardingService.tachesBloquantesNonFaites(5L)).thenReturn(List.of());
        when(affectations.findByIdContratOrderByDateDebutDesc(5L)).thenReturn(List.of());

        ContratDtos.ContratDto resultat = service.valider(5L);

        assertEquals(ContratEtat.VALIDE, resultat.etat());
        assertEquals(StatutExecutionContrat.EN_COURS, resultat.statutExecution());
        verify(employes).changerStatut(2L, StatutEmploye.ACTIF);
    }

    @Test
    void laRetractationEstRefuseeApresLeDebutDuContrat() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.SIGNE,
                LocalDate.now(), null);
        when(contrats.findById(5L)).thenReturn(Optional.of(contrat));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.annuler(5L, new ContratDtos.AnnulationRequest("Changement d'avis")));

        assertEquals("RETRACTATION_TARDIVE", erreur.getCode());
        assertEquals(ContratEtat.SIGNE, contrat.getEtat());
    }

    @Test
    void lAnnulationAvantDebutConserveLeMotifEtAnnuleLAffectation() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.SIGNE,
                LocalDate.now().plusDays(3), null);
        when(contrats.findById(5L)).thenReturn(Optional.of(contrat));
        Affectation affectation = new Affectation();
        when(affectations.findByIdContratOrderByDateDebutDesc(5L))
                .thenReturn(List.of(affectation));
        when(onboardings.findFirstByIdContrat(5L)).thenReturn(Optional.empty());

        ContratDtos.ContratDto resultat = service.annuler(5L,
                new ContratDtos.AnnulationRequest("Retractation du candidat"));

        assertEquals(ContratEtat.ANNULE, resultat.etat());
        assertEquals("Retractation du candidat", contrat.getMotifAnnulation());
        assertEquals(com.tovo.erp.contrat.model.AffectationStatut.ANNULEE,
                affectation.getStatut());
    }

    @Test
    void leRenouvellementCreeUnContratSuccesseurSansEcraserLHistorique() {
        LocalDate fin = LocalDate.now().plusMonths(10);
        Contrat precedent = contrat(5L, TypeContrat.CDD, ContratEtat.VALIDE,
                LocalDate.now().minusMonths(2), fin);
        precedent.setRang(1);
        when(contrats.findById(5L)).thenReturn(Optional.of(precedent));
        when(affectations.findByIdContratOrderByDateDebutDesc(5L)).thenReturn(List.of());
        when(employes.dossier(2L)).thenReturn(salarie());
        when(pdf.generer(any(), any(), any(), any(), any(), any()))
                .thenReturn("pdf".getBytes());
        when(employes.enregistrerDocument(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(document());
        when(references.suivante("CONTRAT", "CT", 5)).thenReturn("CT-2026-000042");

        ContratDtos.ContratDto resultat = service.renouveler(5L,
                new ContratDtos.RenouvellementRequest(fin.plusMonths(6), null, null,
                        null, null, "Accroissement temporaire", 9L));

        assertEquals(TypeContrat.CDD_RENOUVELLEMENT, resultat.typeContrat());
        assertEquals(2, resultat.rang());
        assertEquals(fin.plusDays(1), resultat.dateDebut());
        assertEquals(5L, resultat.idContratPrecedent());
        assertEquals(ContratEtat.CREE, resultat.etat());
    }

    @Test
    void leRenouvellementEstRefuseAuDelaDuNombreAutorise() {
        LocalDate fin = LocalDate.now().plusMonths(2);
        Contrat precedent = contrat(5L, TypeContrat.CDD_RENOUVELLEMENT, ContratEtat.VALIDE,
                LocalDate.now().minusMonths(4), fin);
        precedent.setIdContratPrecedent(1L);
        Contrat premier = contrat(1L, TypeContrat.CDD, ContratEtat.VALIDE,
                LocalDate.now().minusMonths(10), LocalDate.now().minusMonths(4));
        when(contrats.findById(5L)).thenReturn(Optional.of(precedent));
        when(contrats.findById(1L)).thenReturn(Optional.of(premier));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.renouveler(5L, new ContratDtos.RenouvellementRequest(
                        fin.plusMonths(2), null, null, null, null, "Surcroit", 9L)));

        assertEquals("RENOUVELLEMENT_LIMITE", erreur.getCode());
    }

    @Test
    void laTransformationEnCdiAssureLaContinuite() {
        LocalDate fin = LocalDate.now().plusMonths(1);
        Contrat precedent = contrat(5L, TypeContrat.CDD, ContratEtat.VALIDE,
                LocalDate.now().minusMonths(11), fin);
        when(contrats.findById(5L)).thenReturn(Optional.of(precedent));
        when(affectations.findByIdContratOrderByDateDebutDesc(5L)).thenReturn(List.of());
        when(employes.dossier(2L)).thenReturn(salarie());
        when(pdf.generer(any(), any(), any(), any(), any(), any()))
                .thenReturn("pdf".getBytes());
        when(employes.enregistrerDocument(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(document());
        when(references.suivante("CONTRAT", "CT", 5)).thenReturn("CT-2026-000043");

        ContratDtos.ContratDto resultat = service.transformerEnCdi(5L,
                new ContratDtos.TransformationCdiRequest(null, null, null, null, null, 9L));

        assertEquals(TypeContrat.CDI, resultat.typeContrat());
        assertEquals(fin.plusDays(1), resultat.dateDebut());
        assertEquals(null, resultat.dateFinPrevue());
        assertEquals(5L, resultat.idContratPrecedent());
    }

    @Test
    void lePlanificateurActiveLesContratsDontLaDateEstArrivee() {
        Contrat contrat = contrat(5L, TypeContrat.CDI, ContratEtat.VALIDE,
                LocalDate.now(), null);
        contrat.setStatutExecution(StatutExecutionContrat.A_VENIR);
        when(contrats.findAllByOrderByDateCreationDesc()).thenReturn(List.of(contrat));
        when(affectations.findByIdContratOrderByDateDebutDesc(5L)).thenReturn(List.of());

        ContratService.ResultatEcheances resultat = service.traiterEcheances();

        assertEquals(1, resultat.actives());
        assertEquals(StatutExecutionContrat.EN_COURS, contrat.getStatutExecution());
    }

    @Test
    void leContratCourantEstCeluiQuiEstEnExecution() {
        Contrat ancien = contrat(4L, TypeContrat.CDD, ContratEtat.VALIDE,
                LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));
        ancien.setStatutExecution(StatutExecutionContrat.TERMINE);
        Contrat courant = contrat(5L, TypeContrat.CDI, ContratEtat.VALIDE,
                LocalDate.now().minusMonths(1), null);
        courant.setStatutExecution(StatutExecutionContrat.EN_COURS);
        when(contrats.findByIdEmployeOrderByDateDebutDesc(2L))
                .thenReturn(List.of(courant, ancien));

        ContratDtos.ContratDto resultat = service.courant(2L);

        assertEquals(5L, resultat.id());
    }

    private Contrat contrat(Long id, TypeContrat type, ContratEtat etat,
                            LocalDate debut, LocalDate fin) {
        Contrat contrat = new Contrat();
        contrat.setId(id);
        contrat.setNumero("CT-2026-0000" + id);
        contrat.setIdEmploye(2L);
        contrat.setTypeContrat(type);
        contrat.setEtat(etat);
        contrat.setDateDebut(debut);
        contrat.setDateFinPrevue(fin);
        contrat.setSalaireBase(new BigDecimal("800000"));
        contrat.setDevise("MGA");
        contrat.setRang(1);
        contrat.setDateCreation(LocalDateTime.now());
        return contrat;
    }

    private EmployeEmbaucheDto salarie() {
        return new EmployeEmbaucheDto(2L, "MAT2026000042", "Rakoto", "Anja",
                "anja@example.com", "0340000000", "1234567890",
                LocalDate.of(1995, 5, 12), 5L, "Developpeur", 2L, "Informatique",
                3L, "Technique", StatutEmploye.PRE_EMBAUCHE, LocalDate.now(), null,
                LocalDateTime.now());
    }

    private DocumentEmbaucheDto document() {
        return new DocumentEmbaucheDto(77L, 2L, 5L, TypeDocumentEmploye.CONTRAT,
                "Contrat", "contrat.pdf", "application/pdf", "hash", 1, null,
                null, LocalDate.now(), null, LocalDateTime.now());
    }
}
