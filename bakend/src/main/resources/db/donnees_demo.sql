-- =====================================================================
-- ERP - BLOC 1 : donnees de demonstration et de test
-- =====================================================================
--
-- Ce script correspond exactement au schema cree par la migration
-- bakend/src/main/resources/db/migration/V1__bloc1_schema.sql (11 tables).
-- Il fournit un jeu de donnees realiste pour presenters et tester
-- l'application : referentiels, comptes, catalogue de postes, employes,
-- budgets, demandes multi-lignes a tous les stades du circuit, decisions
-- de validation, controles budgetaires et journal des statuts.
--
-- Deux Facon de charger ces donnees :
--
--   1. Automatique (recommande) : au demarrage du backend, la classe
--      com.tovo.erp.config.DataInitializer insere exactement le meme
--      jeu de donnees si la base est vide (et ne touche jamais a une
--      base deja peuplee).
--
--   2. Manuelle avec psql :
--        PGPASSWORD=postgres psql -h localhost -U postgres -d erp \
--            -f bakend/src/main/resources/db/donnees_demo.sql
--      Le script est idempotent : ON CONFLICT DO NOTHING sur chaque
--      insertion, il peut etre relance sans casser la base.
--
-- Comptes de demonstration (mot de passe initial, hache en BCrypt) :
--   manager@tovo.mg    / Manager@2026     (MANAGER,   departement Commercial)
--   rh@tovo.mg         / Rh@2026          (RH,        departement Support)
--   finance@tovo.mg    / Finance@2026     (FINANCE,   departement Support)
--   direction@tovo.mg  / Direction@2026   (DIRECTION, departement Commercial)
--   ancien.manager@tovo.mg / Manager@2026 (desactive : la connexion est refusee)
--
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- role : les 4 roles metier du circuit de validation
-- ---------------------------------------------------------------------
INSERT INTO role (id_role, nom) VALUES
    (1, 'MANAGER'),
    (2, 'RH'),
    (3, 'FINANCE'),
    (4, 'DIRECTION')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- departement
-- ---------------------------------------------------------------------
INSERT INTO departement (id_departement, nom) VALUES
    (1, 'Commercial'),
    (2, 'Production'),
    (3, 'Support')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- service : 1 departement -> plusieurs services
-- ---------------------------------------------------------------------
INSERT INTO service (id_service, id_departement, nom) VALUES
    (1, 1, 'Ventes'),
    (2, 2, 'Atelier'),
    (3, 3, 'Ressources Humaines')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- utilisateur : mot_de_passe = hash BCrypt du mot de passe initial
-- ---------------------------------------------------------------------
INSERT INTO utilisateur (id_utilisateur, id_departement, id_role, nom, email, mot_de_passe, actif) VALUES
    (1, 1, 1, 'Manager Commercial', 'manager@tovo.mg',
     '$2a$10$zNRJxjtsCItcDS9cP7GQuOPAYZaEHZzztdouAGGJMTc8G9n0Sz0ta', TRUE),
    (2, 3, 2, 'Responsable RH', 'rh@tovo.mg',
     '$2a$10$7j7OTatl7zA8XRiRcdk/deOaoOADcFmr1012TplAqT5Rfspd6.Sp.', TRUE),
    (3, 3, 3, 'Responsable Finance', 'finance@tovo.mg',
     '$2a$10$YJ9gK7BqcERYw99KBgn7MuxE/AVF86ILUYO8gDio0oXgnxwNKN3Im', TRUE),
    (4, 1, 4, 'Directeur General', 'direction@tovo.mg',
     '$2a$10$kXOqy27IWr2k8D/SQLaa7uO1PnTy2.9PN9l0VWUn6Hzs3GJeCJdd6', TRUE),
    (5, 1, 1, 'Ancien Manager', 'ancien.manager@tovo.mg',
     '$2a$10$zNRJxjtsCItcDS9cP7GQuOPAYZaEHZzztdouAGGJMTc8G9n0Sz0ta', FALSE)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- poste : catalogue officiel des postes (RM-006, RM-010)
-- ---------------------------------------------------------------------
INSERT INTO poste (id_poste, id_service, intitule, description, salaire_min, salaire_max, actif) VALUES
    (1, 1, 'Charge de clientele',
     'Accueille et suit les clients du portefeuille commercial.', 900000, 1400000, TRUE),
    (2, 2, 'Chef d atelier',
     'Encadre l equipe de production.', 1000000, 1500000, TRUE),
    (3, 3, 'Charge de recrutement',
     'Pilote le processus de recrutement.', 950000, 1400000, TRUE)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- employe : employes existants, cibles possibles d un remplacement
-- ---------------------------------------------------------------------
INSERT INTO employe (id_employe, matricule, nom, prenom, email, telephone, date_embauche, id_poste, statut) VALUES
    (1, 'EMP-0001', 'Rakoto', 'Naina', 'naina.rakoto@tovo.mg', '034 11 222 33', CURRENT_DATE - INTERVAL '3 years', 1, 'ACTIF'),
    (2, 'EMP-0002', 'Andrian', 'Tiana', 'tiana.andrian@tovo.mg', '034 22 333 44', CURRENT_DATE - INTERVAL '8 years', 1, 'PARTI'),
    (3, 'EMP-0003', 'Rasoa', 'Faniry', 'faniry.rasoa@tovo.mg', '034 33 444 55', CURRENT_DATE - INTERVAL '1 year', 1, 'ACTIF')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- budget_service : budget annuel par service et par exercice
-- (disponible = montant_alloue - montant_depense - montant_engage)
-- ---------------------------------------------------------------------
INSERT INTO budget_service (id_budget_service, id_service, exercice, montant_alloue, montant_depense, montant_engage) VALUES
    (1, 1, EXTRACT(YEAR FROM CURRENT_DATE)::int - 1, 45000000, 0, 0),
    (2, 1, EXTRACT(YEAR FROM CURRENT_DATE)::int,     45000000, 0, 0),
    (3, 1, EXTRACT(YEAR FROM CURRENT_DATE)::int + 1, 45000000, 0, 0),
    (4, 2, EXTRACT(YEAR FROM CURRENT_DATE)::int - 1, 38000000, 0, 0),
    (5, 2, EXTRACT(YEAR FROM CURRENT_DATE)::int,     38000000, 0, 0),
    (6, 2, EXTRACT(YEAR FROM CURRENT_DATE)::int + 1, 38000000, 0, 0),
    (7, 3, EXTRACT(YEAR FROM CURRENT_DATE)::int - 1, 22000000, 0, 0),
    (8, 3, EXTRACT(YEAR FROM CURRENT_DATE)::int,     22000000, 0, 0),
    (9, 3, EXTRACT(YEAR FROM CURRENT_DATE)::int + 1, 22000000, 0, 0)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- demande_recrutement : une demande par demandeur
--   1 FINALISEE           2 EN_VALIDATION      3 BROUILLON
--   4 A_CORRIGER          5 REFUSEE (budget insuffisant)
-- ---------------------------------------------------------------------
INSERT INTO demande_recrutement (id_demande, id_utilisateur, motif, date_souhaitee, statut,
                                 date_creation, date_modification, date_soumission) VALUES
    (1, 1, 'Accroissement d activite commerciale', CURRENT_DATE + 60, 'FINALISEE',
     CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (2, 1, 'Renouvellement de l equipe de vente', CURRENT_DATE + 45, 'EN_VALIDATION',
     CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 1, 'Renouvellement du poste de chef d atelier', CURRENT_DATE + 90, 'BROUILLON',
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL),
    (4, 1, 'Recrutement d un responsable commercial', CURRENT_DATE + 30, 'A_CORRIGER',
     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (5, 1, 'Poste de direction commerciale', CURRENT_DATE + 20, 'REFUSEE',
     CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '3 days')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- demande_recrutement_poste : une ligne par besoin, validee separement
--   episode    : incrementee a chaque resoumission apres correction (RM-040)
--   etape_courante : etape a laquelle la ligne est suspendue (RM-038)
-- ---------------------------------------------------------------------
INSERT INTO demande_recrutement_poste (
    id_demande_poste, id_demande, id_service, id_poste,
    type_besoin, type_remplacement, id_employe_remplace, motif_remplacement,
    date_debut_remplacement, date_fin_remplacement,
    intitule_demande, description_poste, profil_recherche,
    salaire_min_estime, salaire_max_estime, effectif, budget_estime,
    statut, date_creation, date_modification, date_soumission, episode, etape_courante) VALUES
    -- Demande 1 : ligne acceptee (postes rattache, budget engage)
    (1, 1, 1, 1, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Charge de clientele premium', 'Profil commercial senior', '3 ans d experience',
     1200000, 2500000, 1, 1500000, 'ACCEPTEE',
     CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '20 days', 1, NULL),
    -- Demande 2 : ligne en attente de la FINANCE
    (2, 2, 1, NULL, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Conseiller commercial', 'Profil commercial', '2 ans d experience',
     1000000, 1800000, 1, 2000000, 'EN_ATTENTE_FINANCE',
     CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 1, 'FINANCE'),
    -- Demande 2 : ligne remplacement, en attente de la RH
    (3, 2, 1, 1, 'REMPLACEMENT', 'DEFINITIF', 2, 'Depart a la retraite',
     CURRENT_DATE + 20, CURRENT_DATE + 50,
     'Remplacement employe', NULL, 'Profil commercial',
     1000000, 1800000, 1, 1000000, 'EN_ATTENTE_RH',
     CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 1, 'RH'),
    -- Demande 3 : ligne en brouillon
    (4, 3, 2, 2, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Chef d atelier', 'Encadrement de la production', '5 ans d experience',
     1000000, 1500000, 1, 3000000, 'BROUILLON',
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 1, NULL),
    -- Demande 4 : ligne renvoyee a correction par la RH
    (5, 4, 1, NULL, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Responsable commercial', 'Encadrement commercial', '5 ans d experience',
     1500000, 3000000, 1, 2500000, 'A_CORRIGER',
     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 1, 'RH'),
    -- Demande 4 : ligne deja validee par la RH et la FINANCE
    (6, 4, 1, NULL, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Chargé de clientele', 'Accueil et suivi des clients', '2 ans d experience',
     900000, 1400000, 1, 1200000, 'EN_ATTENTE_DIRECTION',
     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 1, 'DIRECTION'),
    -- Demande 5 : ligne refusee par la FINANCE (budget insuffisant)
    (7, 5, 1, NULL, 'CREATION', NULL, NULL, NULL, NULL, NULL,
     'Poste de direction', 'Direction commerciale', '10 ans d experience',
     5000000, 9000000, 1, 900000000, 'REFUSEE',
     CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '3 days', 1, 'FINANCE')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- validation : une decision par ligne et par episode
-- ---------------------------------------------------------------------
INSERT INTO validation (id_validation, id_demande_poste, id_utilisateur, etape, decision, commentaire, date_action, episode) VALUES
    (1, 1, 2, 'RH', 'APPROUVE', 'Profil et budget valides', CURRENT_TIMESTAMP - INTERVAL '21 days', 1),
    (2, 1, 3, 'FINANCE', 'APPROUVE', 'Budget verifie', CURRENT_TIMESTAMP - INTERVAL '20 days', 1),
    (3, 1, 4, 'DIRECTION', 'APPROUVE', 'Decision finale', CURRENT_TIMESTAMP - INTERVAL '19 days', 1),
    (4, 2, 2, 'RH', 'APPROUVE', 'Valide par les RH', CURRENT_TIMESTAMP - INTERVAL '1 day', 1),
    (5, 3, 2, 'RH', 'APPROUVE', 'Valide par les RH', CURRENT_TIMESTAMP - INTERVAL '1 day', 1),
    (6, 5, 2, 'RH', 'A_CORRIGER', 'Preciser le profil recherche', CURRENT_TIMESTAMP - INTERVAL '3 days', 1),
    (7, 6, 2, 'RH', 'APPROUVE', 'Valide par les RH', CURRENT_TIMESTAMP - INTERVAL '4 days', 1),
    (8, 6, 3, 'FINANCE', 'APPROUVE', 'Budget verifie', CURRENT_TIMESTAMP - INTERVAL '3 days', 1),
    (9, 7, 2, 'RH', 'APPROUVE', 'Valide par les RH', CURRENT_TIMESTAMP - INTERVAL '3 days', 1),
    (10, 7, 3, 'FINANCE', 'REFUSE', 'Budget insuffisant pour ce besoin', CURRENT_TIMESTAMP - INTERVAL '1 day', 1)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- verification_budget : controle financier de la FINANCE (RM-052 a RM-055)
-- ---------------------------------------------------------------------
INSERT INTO verification_budget (id_verification, id_demande_poste, id_budget_service, id_utilisateur,
                                 budget_demande, budget_disponible, montant_estime, decision, commentaire,
                                 date_verification, episode) VALUES
    (1, 1, 2, 3, 1500000, 45000000, 1500000, 'APPROUVE', 'Budget disponible suffisant',
     CURRENT_TIMESTAMP - INTERVAL '20 days', 1),
    (2, 6, 2, 3, 1200000, 45000000, 1200000, 'APPROUVE', 'Budget disponible suffisant',
     CURRENT_TIMESTAMP - INTERVAL '3 days', 1),
    (3, 7, 2, 3, 900000000, 45000000, 900000000, 'REFUSE', 'Budget insuffisant pour ce besoin',
     CURRENT_TIMESTAMP - INTERVAL '1 day', 1)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- historique_statut : journal des changements de statut (RM-065)
-- id_demande_poste IS NULL => le changement concerne la demande globale
-- ---------------------------------------------------------------------
INSERT INTO historique_statut (id_historique, id_demande, id_demande_poste, ancien_statut, nouveau_statut,
                               id_utilisateur, commentaire, date_action) VALUES
    (1, 1, 1, 'BROUILLON', 'EN_ATTENTE_RH', 1, 'Soumission de la demande', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (2, 1, 1, 'EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 2, 'Valide par les RH', CURRENT_TIMESTAMP - INTERVAL '21 days'),
    (3, 1, 1, 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION', 3, 'Budget verifie', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (4, 1, 1, 'EN_ATTENTE_DIRECTION', 'ACCEPTEE', 4, 'Decision finale', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (5, 1, NULL, 'EN_VALIDATION', 'FINALISEE', 4, 'Toutes les lignes sont terminees', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (6, 2, NULL, 'BROUILLON', 'EN_VALIDATION', 1, 'Soumission de la demande', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (7, 4, 5, 'EN_ATTENTE_RH', 'A_CORRIGER', 2, 'Preciser le profil recherche', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (8, 4, NULL, 'EN_VALIDATION', 'A_CORRIGER', 2, 'Une ligne doit etre corrigee', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (9, 5, 7, 'EN_ATTENTE_FINANCE', 'REFUSEE', 3, 'Budget insuffisant', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (10, 5, NULL, 'EN_VALIDATION', 'REFUSEE', 3, 'Aucune ligne acceptee', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------
-- Remise en etat des sequences (les identifiants sont inseres
-- explicitement pour que le script reste lisible)
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('role', 'id_role'), GREATEST((SELECT MAX(id_role) FROM role), 1));
SELECT setval(pg_get_serial_sequence('departement', 'id_departement'), GREATEST((SELECT MAX(id_departement) FROM departement), 1));
SELECT setval(pg_get_serial_sequence('service', 'id_service'), GREATEST((SELECT MAX(id_service) FROM service), 1));
SELECT setval(pg_get_serial_sequence('utilisateur', 'id_utilisateur'), GREATEST((SELECT MAX(id_utilisateur) FROM utilisateur), 1));
SELECT setval(pg_get_serial_sequence('poste', 'id_poste'), GREATEST((SELECT MAX(id_poste) FROM poste), 1));
SELECT setval(pg_get_serial_sequence('employe', 'id_employe'), GREATEST((SELECT MAX(id_employe) FROM employe), 1));
SELECT setval(pg_get_serial_sequence('budget_service', 'id_budget_service'), GREATEST((SELECT MAX(id_budget_service) FROM budget_service), 1));
SELECT setval(pg_get_serial_sequence('demande_recrutement', 'id_demande'), GREATEST((SELECT MAX(id_demande) FROM demande_recrutement), 1));
SELECT setval(pg_get_serial_sequence('demande_recrutement_poste', 'id_demande_poste'), GREATEST((SELECT MAX(id_demande_poste) FROM demande_recrutement_poste), 1));
SELECT setval(pg_get_serial_sequence('validation', 'id_validation'), GREATEST((SELECT MAX(id_validation) FROM validation), 1));
SELECT setval(pg_get_serial_sequence('verification_budget', 'id_verification'), GREATEST((SELECT MAX(id_verification) FROM verification_budget), 1));
SELECT setval(pg_get_serial_sequence('historique_statut', 'id_historique'), GREATEST((SELECT MAX(id_historique) FROM historique_statut), 1));
