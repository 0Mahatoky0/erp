package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.model.TypeContrat;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import com.tovo.erp.employe.model.StatutEmploye;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratPdfServiceTest {

    private final ContratPdfService service = new ContratPdfService();

    @Test
    void genereUnPdfValideEtLisible() {
        Contrat contrat = new Contrat();
        contrat.setNumero("CT-2026-0001");
        contrat.setTypeContrat(TypeContrat.CDI);
        contrat.setDateDebut(LocalDate.of(2026, 12, 1));
        contrat.setDateCreation(LocalDateTime.now());
        contrat.setSalaireBase(new BigDecimal("750000"));
        contrat.setDevise("MGA");

        EmployeEmbaucheDto salarie = new EmployeEmbaucheDto(
                6L, null, "Rakoto", "Anja", "anja@example.com", "0340000000",
                "1234567890", LocalDate.of(1995, 5, 12),
                5L, "Developpeur", 2L, "Informatique", 3L, "Technique",
                StatutEmploye.PRE_EMBAUCHE, LocalDate.of(2026, 12, 1), null,
                LocalDateTime.now());

        byte[] pdf = service.generer(contrat, salarie, "Developpeur", "Informatique",
                "Technique", "Antananarivo");

        String contenu = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(pdf.length > 500);
        assertTrue(contenu.startsWith("%PDF-1.4"));
        assertTrue(contenu.contains("CONTRAT DE TRAVAIL"));
        assertTrue(contenu.contains("CT-2026-0001"));
        assertTrue(contenu.endsWith("%%EOF\n"));
    }
}
