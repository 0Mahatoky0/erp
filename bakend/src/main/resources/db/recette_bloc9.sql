-- Données de recette Bloc 9. Idempotent : les lignes portant les codes/labels
-- de recette ne sont insérées qu'une fois. À exécuter explicitement, hors Flyway.
BEGIN;

INSERT INTO campagne_evaluation
    (libelle, type, periode_debut, periode_fin, date_ouverture, date_cloture,
     grille_json, ponderations_json, population, statut)
SELECT 'RECETTE BLOC 9 - Évaluation', 'ANNUELLE', DATE '2026-01-01', DATE '2026-12-31',
       DATE '2026-01-01', DATE '2027-12-31',
       '{"ponctualite":1,"qualite":3,"collaboration":3}',
       '{"objectifs":0.5,"competences":0.3,"comportement":0.2}', 'RECETTE', 'OUVERTE'
WHERE NOT EXISTS (SELECT 1 FROM campagne_evaluation WHERE libelle = 'RECETTE BLOC 9 - Évaluation');

INSERT INTO competence (code, libelle, description, categorie, echelle)
VALUES ('RECETTE-B9-COMP', 'Compétence de recette B9', 'Donnée de validation Bloc 9', 'METIER', '1-5')
ON CONFLICT (code) DO NOTHING;

INSERT INTO formation
    (code, titre, type, organisme, duree_h, cout, certifiante, competences_visees, seuil_dedit)
VALUES ('RECETTE-B9-FORM', 'Formation de recette Bloc 9', 'INTERNE', 'ERP Tovo', 7, 250000,
        FALSE, '[{"competence":1,"niveau":3}]', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO session_formation
    (id_formation, date_debut, date_fin, lieu, capacite, formateur, cout, statut)
SELECT f.id_formation, DATE '2027-06-10', DATE '2027-06-10', 'Salle de recette', 2,
       'Formateur recette', 250000, 'OUVERTE'
FROM formation f
WHERE f.code = 'RECETTE-B9-FORM'
  AND NOT EXISTS (SELECT 1 FROM session_formation s WHERE s.id_formation = f.id_formation
                  AND s.lieu = 'Salle de recette');

INSERT INTO objectif
    (id_employe, id_campagne, libelle, indicateur, unite, valeur_cible, valeur_realisee,
     sens, poids, date_echeance, statut)
SELECT 1, c.id_campagne, 'Objectif de recette B9', 'Livrables', '%', 100, 85,
       'PLUS_EST_MIEUX', 100, DATE '2026-12-31', 'DEFINI'
FROM campagne_evaluation c
WHERE c.libelle = 'RECETTE BLOC 9 - Évaluation'
  AND NOT EXISTS (SELECT 1 FROM objectif o WHERE o.id_employe = 1 AND o.id_campagne = c.id_campagne
                  AND o.libelle = 'Objectif de recette B9');

INSERT INTO evaluation
    (id_employe, id_campagne, id_evaluateur, note_objectifs, note_competences,
     note_comportement, note_globale, appreciation, statut, version)
SELECT 1, c.id_campagne, 2, 4.25, 4.04, 4.14, 4.17, 'Supérieur', 'SIGNEE', 1
FROM campagne_evaluation c
WHERE c.libelle = 'RECETTE BLOC 9 - Évaluation'
  AND NOT EXISTS (SELECT 1 FROM evaluation e WHERE e.id_employe = 1
                  AND e.id_campagne = c.id_campagne AND e.version = 1);

INSERT INTO ecart_competence
    (id_employe, id_competence, id_evaluation, niveau_requis, niveau_acquis,
     ecart, priorite_score, priorite, statut)
SELECT 1, cp.id_competence, e.id_evaluation, 4, 2, 2, 6, 'HAUTE', 'OUVERT'
FROM competence cp CROSS JOIN campagne_evaluation c
JOIN evaluation e ON e.id_campagne = c.id_campagne AND e.id_employe = 1
WHERE cp.code = 'RECETTE-B9-COMP' AND c.libelle = 'RECETTE BLOC 9 - Évaluation'
  AND NOT EXISTS (SELECT 1 FROM ecart_competence ec WHERE ec.id_evaluation = e.id_evaluation
                  AND ec.id_competence = cp.id_competence);

INSERT INTO besoin_formation
    (id_employe, id_competence, niveau_cible, origine, priorite, date_cible, statut)
SELECT 1, cp.id_competence, 4, 'ECART', 'HAUTE', DATE '2027-12-31', 'IDENTIFIE'
FROM competence cp
WHERE cp.code = 'RECETTE-B9-COMP'
  AND NOT EXISTS (SELECT 1 FROM besoin_formation b WHERE b.id_employe = 1
                  AND b.id_competence = cp.id_competence AND b.origine = 'ECART');

INSERT INTO inscription_formation (id_session, id_employe, statut, id_besoin)
SELECT s.id_session, 1, 'LISTE_ATTENTE', b.id_besoin
FROM session_formation s
JOIN formation f ON f.id_formation = s.id_formation AND f.code = 'RECETTE-B9-FORM'
JOIN competence cp ON cp.code = 'RECETTE-B9-COMP'
JOIN besoin_formation b ON b.id_employe = 1 AND b.id_competence = cp.id_competence
WHERE s.lieu = 'Salle de recette'
  AND NOT EXISTS (SELECT 1 FROM inscription_formation i WHERE i.id_session = s.id_session
                  AND i.id_employe = 1);

COMMIT;
