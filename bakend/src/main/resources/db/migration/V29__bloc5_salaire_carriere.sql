-- Historique de remuneration et evenements de carriere (RG-B5-03, RG-B5-06).
CREATE TABLE salaire_historique (
    id_salaire_historique BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe (id_employe),
    id_contrat BIGINT,
    montant_brut NUMERIC(15, 2) NOT NULL,
    devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
    date_effet DATE NOT NULL,
    date_fin DATE,
    motif VARCHAR(100),
    id_utilisateur BIGINT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_salaire_historique_contrat
    ON salaire_historique (id_contrat)
    WHERE id_contrat IS NOT NULL;
CREATE INDEX idx_salaire_historique_employe
    ON salaire_historique (id_employe, date_effet DESC);

CREATE TABLE evenement_carriere (
    id_evenement_carriere BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe (id_employe),
    type VARCHAR(40) NOT NULL,
    date_effet DATE NOT NULL,
    ancienne_valeur TEXT,
    nouvelle_valeur TEXT,
    id_document BIGINT,
    commentaire TEXT,
    id_utilisateur BIGINT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evenement_carriere_employe
    ON evenement_carriere (id_employe, date_effet DESC);
