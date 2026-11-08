package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.employe.dto.EmployeEmbaucheDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generateur PDF minimal, sans dependance externe.
 * <p>Le contenu est compose en police Helvetica (WinAnsiEncoding) et ecrit
 * dans une structure PDF 1.4 valide. Cela suffit pour un contrat lisible et
 * archivable ; un moteur de rendu plus riche pourra le remplacer plus tard.
 */
@Service
public class ContratPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LARGEUR_MAX = 88;

    public byte[] generer(Contrat contrat, EmployeEmbaucheDto salarie,
                          String poste, String service, String departement, String lieu) {
        List<String> lignes = new ArrayList<>();
        lignes.add("CONTRAT DE TRAVAIL");
        lignes.add("");
        lignes.add("Reference contrat : " + contrat.getNumero());
        lignes.add("Type de contrat : " + contrat.getTypeContrat());
        lignes.add("");
        lignes.add("ENTRE");
        lignes.add("La societe TOVO S.A., ci-apres designee l'Employeur,");
        lignes.add("ET");
        lignes.add("M./Mme " + salarie.prenom() + " " + salarie.nom()
                + (salarie.cin() == null ? "" : ", CIN " + salarie.cin()));
        if (salarie.dateNaissance() != null) {
            lignes.add("Ne(e) le " + DATE.format(salarie.dateNaissance()));
        }
        lignes.add("");
        lignes.add("ARTICLE 1 - ENGAGEMENT");
        lignes.add("L'Employeur engage le salarie en qualite de "
                + (poste == null ? "salarie" : poste) + ".");
        lignes.add("Service : " + valeur(service) + " - Departement : " + valeur(departement));
        lignes.add("Lieu de travail : " + valeur(lieu));
        lignes.add("");
        lignes.add("ARTICLE 2 - DUREE");
        lignes.add("Date de debut : " + DATE.format(contrat.getDateDebut()));
        if (contrat.getDateFinPrevue() != null) {
            lignes.add("Date de fin prevue : " + DATE.format(contrat.getDateFinPrevue()));
        } else {
            lignes.add("Contrat conclu pour une duree indeterminee.");
        }
        if (contrat.getFinPeriodeEssai() != null) {
            lignes.add("Fin de la periode d'essai : " + DATE.format(contrat.getFinPeriodeEssai()));
        }
        lignes.add("");
        lignes.add("ARTICLE 3 - REMUNERATION");
        lignes.add("Salaire brut mensuel : " + montant(contrat.getSalaireBase()) + " "
                + valeur(contrat.getDevise()));
        if (contrat.getPrimesAvantages() != null && !contrat.getPrimesAvantages().isBlank()) {
            lignes.add("Primes et avantages : " + contrat.getPrimesAvantages());
        }
        if (contrat.getHoraires() != null && !contrat.getHoraires().isBlank()) {
            lignes.add("Horaires : " + contrat.getHoraires());
        }
        lignes.add("");
        lignes.add("ARTICLE 4 - OBLIGATIONS");
        lignes.add("Le salarie s'engage a respecter le reglement interieur, les consignes");
        lignes.add("de securite et la confidentialite des informations de l'entreprise.");
        lignes.add("");
        lignes.add("Fait a Antananarivo, le " + DATE.format(contrat.getDateCreation().toLocalDate()) + ".");
        lignes.add("");
        lignes.add("Pour l'Employeur                              Le Salarie");
        lignes.add("");
        lignes.add("____________________________                  ____________________________");
        lignes.add("");
        lignes.add("Document genere automatiquement - en attente de signature.");
        return ecrire(lignes);
    }

    private byte[] ecrire(List<String> lignes) {
        StringBuilder flux = new StringBuilder();
        flux.append("BT\n");
        flux.append("/F1 11 Tf\n");
        flux.append("50 792 Td\n");
        flux.append("15 TL\n");
        for (String ligne : lignes) {
            for (String morceau : decouper(ligne)) {
                flux.append("(").append(echapper(morceau)).append(") Tj\n");
                flux.append("T*\n");
            }
        }
        flux.append("ET\n");
        byte[] contenu = flux.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> objets = new ArrayList<>();
        objets.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        objets.add(bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        objets.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"));
        objets.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica "
                + "/Encoding /WinAnsiEncoding >>"));
        objets.add(concat(bytes("<< /Length " + contenu.length + " >>\nstream\n"),
                contenu, bytes("\nendstream")));

        try {
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            sortie.write(bytes("%PDF-1.4\n"));
            int[] positions = new int[objets.size()];
            for (int i = 0; i < objets.size(); i++) {
                positions[i] = sortie.size();
                sortie.write(bytes((i + 1) + " 0 obj\n"));
                sortie.write(objets.get(i));
                sortie.write(bytes("\nendobj\n"));
            }
            int xref = sortie.size();
            sortie.write(bytes("xref\n0 " + (objets.size() + 1) + "\n"));
            sortie.write(bytes("0000000000 65535 f \n"));
            for (int position : positions) {
                sortie.write(bytes(String.format("%010d 00000 n \n", position)));
            }
            sortie.write(bytes("trailer\n<< /Size " + (objets.size() + 1)
                    + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Generation du PDF impossible", ex);
        }
    }

    private List<String> decouper(String ligne) {
        if (ligne.length() <= LARGEUR_MAX) {
            return List.of(ligne);
        }
        List<String> morceaux = new ArrayList<>();
        String reste = ligne;
        while (reste.length() > LARGEUR_MAX) {
            int coupure = reste.lastIndexOf(' ', LARGEUR_MAX);
            if (coupure <= 0) {
                coupure = LARGEUR_MAX;
            }
            morceaux.add(reste.substring(0, coupure));
            reste = reste.substring(coupure).trim();
        }
        if (!reste.isEmpty()) {
            morceaux.add(reste);
        }
        return morceaux;
    }

    private String echapper(String texte) {
        return texte.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String valeur(String texte) {
        return texte == null || texte.isBlank() ? "non precise" : texte;
    }

    private String montant(java.math.BigDecimal montant) {
        return montant == null ? "non precise" : montant.toPlainString();
    }

    private static byte[] bytes(String texte) {
        return texte.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] concat(byte[]... morceaux) {
        int taille = 0;
        for (byte[] morceau : morceaux) {
            taille += morceau.length;
        }
        byte[] resultat = new byte[taille];
        int position = 0;
        for (byte[] morceau : morceaux) {
            System.arraycopy(morceau, 0, resultat, position, morceau.length);
            position += morceau.length;
        }
        return resultat;
    }
}
