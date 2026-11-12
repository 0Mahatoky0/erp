-- Bloc 7 : effectif minimum, periodes bloquees, remplacement/suppleance et
-- justificatifs de demande.

ALTER TABLE demande_conge ADD COLUMN id_remplacant BIGINT REFERENCES employe(id_employe);
ALTER TABLE demande_conge ADD COLUMN justificatif_document_id BIGINT;

CREATE TABLE periode_bloquee (
    id_periode_bloquee BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(120) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    derogation_direction BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_periode_bloquee_dates CHECK (date_fin >= date_debut)
);

CREATE INDEX idx_periode_bloquee_dates ON periode_bloquee (date_debut, date_fin);

CREATE TABLE matrice_remplacement (
    id_matrice BIGSERIAL PRIMARY KEY,
    id_poste_remplace BIGINT,
    id_employe_remplace BIGINT REFERENCES employe(id_employe),
    id_poste_remplacant BIGINT,
    id_employe_remplacant BIGINT REFERENCES employe(id_employe),
    niveau VARCHAR(20) NOT NULL DEFAULT 'PRIMAIRE',
    duree_min_jours INTEGER,
    duree_max_jours INTEGER,
    competences_requises VARCHAR(500),
    habilitations VARCHAR(500),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_matrice_niveau CHECK (niveau IN ('PRIMAIRE', 'SECONDAIRE', 'EXCEPTIONNEL')),
    CONSTRAINT ck_matrice_cible CHECK (id_poste_remplace IS NOT NULL OR id_employe_remplace IS NOT NULL),
    CONSTRAINT ck_matrice_remplacant CHECK (
        id_poste_remplacant IS NOT NULL OR id_employe_remplacant IS NOT NULL)
);

CREATE TABLE suppleance (
    id_suppleance BIGSERIAL PRIMARY KEY,
    id_employe_absent BIGINT NOT NULL REFERENCES employe(id_employe),
    id_employe_remplacant BIGINT NOT NULL REFERENCES employe(id_employe),
    id_demande_conge BIGINT REFERENCES demande_conge(id_demande_conge),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'PROPOSEE',
    motif VARCHAR(500),
    date_proposition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMP,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_suppleance_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_suppleance_statut CHECK (
        statut IN ('PROPOSEE', 'ACCEPTEE', 'REFUSEE', 'EN_COURS', 'TERMINEE'))
);

CREATE INDEX idx_suppleance_remplacant_dates
    ON suppleance (id_employe_remplacant, date_debut, date_fin);
CREATE INDEX idx_suppleance_absent_dates
    ON suppleance (id_employe_absent, date_debut, date_fin);

INSERT INTO parametre_conge (cle, valeur, description) VALUES
    ('EFFECTIF_MIN_POURCENT', '60', 'Pourcentage minimal de presents dans le service'),
    ('EFFECTIF_MIN_SERVICE', '', 'Effectif absolu minimal (prioritaire sur le pourcentage si renseigne)'),
    ('SUPPLEANCE_MAX_SIMULTANEE', '1', 'Nombre maximal de suppleances simultanees par remplacant'),
    ('MALADIE_DECLARATION_HEURES', '48', 'Delai maximal de declaration d''une maladie'),
    ('MALADIE_JUSTIFICATIF_HEURES', '72', 'Delai maximal de depot du justificatif maladie')
ON CONFLICT (cle) DO NOTHING;

UPDATE type_conge SET assimile_travail_effectif = TRUE
WHERE code IN ('MALADIE', 'MATERNITE', 'PATERNITE');
UPDATE type_conge SET decompte_solde = FALSE WHERE code = 'PATERNITE';
