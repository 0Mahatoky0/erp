CREATE TABLE statut(
    id_statut INT PRIMARY KEY,
    libelle VARCHAR(50) -- refuse,accepte,en attente
);

CREATE TABLE proposition_embauche(
    id_proposition_embauche INT PRIMARY KEY,
    date_proposition TIMESTAMP,
    id_statut INT REFERENCES statut(id_statut),
    id_candidat INT REFERENCES candidat(id_candidat), -- bloc 2
    id_offre_emploi INT REFERENCES offre_emploi(id_offre_emploi) -- bloc 2
);

CREATE TABLE validation_contrat(
    id_validation_contrat INT PRIMARY KEY,
    id_candidat INT REFERENCES candidat(id_candidat), -- bloc 2
    date_validation TIMESTAMP,
    id_statut INT REFERENCES statut(id_statut),
    id_offre_emploi INT REFERENCES offre_emploi(id_offre_emploi) -- bloc 2
);

CREATE TABLE type_contrat(
    id_type_contrat INT PRIMARY KEY,
    libelle VARCHAR(50)
);

CREATE TABLE duree_essai(
    id_duree_essai INT PRIMARY KEY,
    valeur INT,
    unite VARCHAR(50)
);

CREATE TABLE employe(
    id_employe INT PRIMARY KEY,
    id_candidat INT UNIQUE REFERENCES candidat(id_candidat) -- bloc 2
);

CREATE TABLE periodicite_paie(
    id_periodicite_paie INT PRIMARY KEY,
    libelle VARCHAR(50)
);

CREATE TABLE mode_paiement(
    id_mode_paiement INT PRIMARY KEY,
    libelle VARCHAR(50)
);

CREATE TABLE contrat(
    id_contrat INT PRIMARY KEY,
    id_type_contrat INT REFERENCES type_contrat(id_type_contrat),
    id_poste INT REFERENCES poste(id_poste), -- bloc 1
    id_employe INT REFERENCES employe(id_employe),
    lieu_travail VARCHAR(100),
    date_debut TIMESTAMP,
    date_fin TIMESTAMP,
    id_duree_essai INT REFERENCES dure_essai(id_duree_essai),
    salaire DECIMAL(12,2),
    id_periodicite_paie INT REFERENCES periodicite_paie(id_periodicite_paie),
    id_mode_paiement INT REFERENCES mode_paiement(id_mode_paiement),
    date_rupture TIMESTAMP,
    motif_rupture VARCHAR(100)
);

CREATE TABLE historique_contrat(
    id_historique_contrat INT PRIMARY KEY,
    id_contrat INT REFERENCES contrat(id_contrat),
    date_historique TIMESTAMP,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);