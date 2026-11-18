package com.tovo.erp.contrat.service;

import com.tovo.erp.contrat.model.Contrat;
import com.tovo.erp.contrat.repository.ContratRepository;
import com.tovo.erp.contrat.dto.CarriereDtos;
import com.tovo.erp.contrat.model.EvenementCarriere;
import com.tovo.erp.contrat.model.SalaireHistorique;
import com.tovo.erp.contrat.model.TypeEvenementCarriere;
import com.tovo.erp.employe.service.EmployeService;
import com.tovo.erp.contrat.repository.EvenementCarriereRepository;
import com.tovo.erp.contrat.repository.SalaireHistoriqueRepository;
import com.tovo.erp.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historique de remuneration et de carriere (bloc 5). Chaque evolution cree une
 * ligne datee ; l'ancienne ligne est fermee et jamais ecrasee (RG-B5-03).
 */
@Service
public class CarriereService {

    private final SalaireHistoriqueRepository salaires;
    private final EvenementCarriereRepository evenements;
    private final ContratRepository contrats;
    private final EmployeService employes;

    public CarriereService(SalaireHistoriqueRepository salaires,
                           EvenementCarriereRepository evenements,
                           ContratRepository contrats,
                           EmployeService employes) {
        this.salaires = salaires;
        this.evenements = evenements;
        this.contrats = contrats;
        this.employes = employes;
    }

    @Transactional
    public List<CarriereDtos.SalaireDto> salaires(Long idEmploye) {
        initialiserDepuisContrats(idEmploye);
        return salaires.findByIdEmployeOrderByDateEffetDesc(idEmploye).stream()
                .map(CarriereDtos.SalaireDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, java.math.BigDecimal> salairesBaseParIds(Collection<Long> idsEmploye) {
        if (idsEmploye.isEmpty()) {
            return Map.of();
        }
        Map<Long, java.math.BigDecimal> resultat = new LinkedHashMap<>();
        salaires.findByIdEmployeInAndDateFinIsNull(idsEmploye).forEach(salaire ->
                resultat.put(salaire.getIdEmploye(), salaire.getMontantBrut()));
        return resultat;
    }

    @Transactional
    public CarriereDtos.SalaireDto salaireCourant(Long idEmploye) {
        initialiserDepuisContrats(idEmploye);
        return salaires.findFirstByIdEmployeAndDateFinIsNullOrderByDateEffetDesc(idEmploye)
                .map(CarriereDtos.SalaireDto::de)
                .orElse(null);
    }

    @Transactional
    public CarriereDtos.SalaireDto enregistrerSalaire(Long idEmploye,
                                                      CarriereDtos.SalaireRequest requete,
                                                      Long idUtilisateur) {
        verifierEmploye(idEmploye);
        initialiserDepuisContrats(idEmploye);
        SalaireHistorique courant = salaires
                .findFirstByIdEmployeOrderByDateEffetDesc(idEmploye)
                .orElse(null);
        if (courant != null) {
            if (!requete.dateEffet().isAfter(courant.getDateEffet())) {
                throw new BusinessException("SALAIRE_DATE_INVALIDE",
                        "La date d'effet doit etre posterieure au salaire courant ("
                                + courant.getDateEffet() + ").");
            }
            if (courant.getDateFin() == null
                    || courant.getDateFin().isAfter(requete.dateEffet().minusDays(1))) {
                courant.setDateFin(requete.dateEffet().minusDays(1));
                salaires.save(courant);
            }
        }
        SalaireHistorique salaire = new SalaireHistorique();
        salaire.setIdEmploye(idEmploye);
        salaire.setIdContrat(requete.idContrat());
        salaire.setMontantBrut(requete.montantBrut());
        salaire.setDevise(requete.devise());
        salaire.setDateEffet(requete.dateEffet());
        salaire.setMotif(requete.motif());
        salaire.setIdUtilisateur(idUtilisateur);
        salaire.setIdMouvement(requete.idMouvement());
        salaire.setIdAvenant(requete.idAvenant());
        salaire.setAncienMontant(requete.ancienMontant() == null && courant != null
                ? courant.getMontantBrut() : requete.ancienMontant());
        salaire.setVariationPct(requete.variationPct());
        salaire.setEnregistrePar(requete.idEnregistrePar());
        salaire.setEnregistreLe(java.time.LocalDateTime.now());
        CarriereDtos.SalaireDto cree = CarriereDtos.SalaireDto.de(salaires.save(salaire));

        EvenementCarriere evenement = new EvenementCarriere();
        evenement.setIdEmploye(idEmploye);
        evenement.setType(TypeEvenementCarriere.AUGMENTATION);
        evenement.setDateEffet(requete.dateEffet());
        evenement.setAncienneValeur(courant == null ? null : courant.getMontantBrut().toPlainString());
        evenement.setNouvelleValeur(requete.montantBrut().toPlainString());
        evenement.setCommentaire(requete.motif());
        evenement.setIdUtilisateur(idUtilisateur);
        evenements.save(evenement);
        return cree;
    }

    @Transactional
    public List<CarriereDtos.EvenementDto> evenements(Long idEmploye) {
        verifierEmploye(idEmploye);
        return evenements.findByIdEmployeOrderByDateEffetDesc(idEmploye).stream()
                .map(CarriereDtos.EvenementDto::de)
                .toList();
    }

    @Transactional
    public CarriereDtos.EvenementDto enregistrerEvenement(Long idEmploye,
                                                          CarriereDtos.EvenementRequest requete,
                                                          Long idUtilisateur) {
        verifierEmploye(idEmploye);
        EvenementCarriere evenement = new EvenementCarriere();
        evenement.setIdEmploye(idEmploye);
        evenement.setType(requete.type());
        evenement.setDateEffet(requete.dateEffet());
        evenement.setAncienneValeur(requete.ancienneValeur());
        evenement.setNouvelleValeur(requete.nouvelleValeur());
        evenement.setIdDocument(requete.idDocument());
        evenement.setCommentaire(requete.commentaire());
        evenement.setIdUtilisateur(idUtilisateur);
        return CarriereDtos.EvenementDto.de(evenements.save(evenement));
    }

    @Transactional
    public CarriereDtos.EvenementDto enregistrerEvenementSimple(Long idEmploye,
                                                                CarriereDtos.EvenementSimpleRequest requete,
                                                                Long idUtilisateur) {
        TypeEvenementCarriere type;
        try {
            type = TypeEvenementCarriere.valueOf(requete.type());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("TYPE_EVENEMENT_INVALIDE",
                    "Type d'evenement de carriere inconnu : " + requete.type());
        }
        return enregistrerEvenement(idEmploye,
                new CarriereDtos.EvenementRequest(type, requete.dateEffet(),
                        requete.ancienneValeur(), requete.nouvelleValeur(), null,
                        requete.commentaire()),
                idUtilisateur);
    }

    /**
     * Cree les lignes manquantes a partir des contrats deja signes afin que
     * l'historique demarre a l'embauche sans ressaisie.
     */
    private void initialiserDepuisContrats(Long idEmploye) {
        List<Contrat> historique = new ArrayList<>(contrats.findByIdEmployeOrderByDateDebutDesc(idEmploye));
        Collections.reverse(historique);
        boolean embaucheTracee = evenements.existsByIdEmployeAndType(idEmploye, TypeEvenementCarriere.EMBAUCHE);
        for (int i = 0; i < historique.size(); i++) {
            Contrat contrat = historique.get(i);
            if (contrat.getSalaireBase() == null || contrat.getId() == null
                    || salaires.existsByIdContrat(contrat.getId())) {
                continue;
            }
            SalaireHistorique salaire = new SalaireHistorique();
            salaire.setIdEmploye(idEmploye);
            salaire.setIdContrat(contrat.getId());
            salaire.setMontantBrut(contrat.getSalaireBase());
            salaire.setDevise(contrat.getDevise() == null ? "MGA" : contrat.getDevise());
            salaire.setDateEffet(contrat.getDateDebut());
            salaire.setDateFin(i + 1 < historique.size()
                    ? historique.get(i + 1).getDateDebut().minusDays(1)
                    : contrat.getDateFinPrevue());
            salaire.setMotif("Salaire du contrat " + contrat.getNumero());
            salaires.save(salaire);
            if (!embaucheTracee && i == 0) {
                EvenementCarriere evenement = new EvenementCarriere();
                evenement.setIdEmploye(idEmploye);
                evenement.setType(TypeEvenementCarriere.EMBAUCHE);
                evenement.setDateEffet(contrat.getDateDebut());
                evenement.setNouvelleValeur(contrat.getNumero());
                evenement.setCommentaire("Embauche initiale.");
                evenements.save(evenement);
                embaucheTracee = true;
            }
        }
    }

    private void verifierEmploye(Long idEmploye) {
        if (!employes.existe(idEmploye)) {
            throw BusinessException.notFound("Le salarie " + idEmploye + " n'existe pas.");
        }
    }
}
