CREATE TABLE type_conge (
    id_type_conge BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    jours_acquis_mois NUMERIC(5,2) NOT NULL DEFAULT 2.50,
    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE solde_conge (
    id_solde_conge BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    id_type_conge BIGINT NOT NULL REFERENCES type_conge(id_type_conge),
    annee INTEGER NOT NULL,
    jours_acquis NUMERIC(6,2) NOT NULL DEFAULT 0,
    jours_pris NUMERIC(6,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_solde_conge UNIQUE (id_employe, id_type_conge, annee),
    CONSTRAINT ck_solde_jours CHECK (jours_acquis >= 0 AND jours_pris >= 0)
);

CREATE TABLE demande_conge (
    id_demande_conge BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    id_type_conge BIGINT NOT NULL REFERENCES type_conge(id_type_conge),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours NUMERIC(6,2) NOT NULL,
    motif TEXT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    id_validateur BIGINT,
    motif_decision TEXT,
    date_decision TIMESTAMP,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_demande_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_demande_jours CHECK (nombre_jours > 0),
    CONSTRAINT ck_demande_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'REFUSEE', 'ANNULEE'))
);

CREATE INDEX idx_demande_conge_employe_dates
    ON demande_conge (id_employe, date_debut, date_fin);

CREATE TABLE absence (
    id_absence BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    motif VARCHAR(100) NOT NULL,
    justificatif TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_JUSTIFIER',
    id_validateur BIGINT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_absence_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_absence_statut CHECK (statut IN ('A_JUSTIFIER', 'JUSTIFIEE', 'REFUSEE'))
);

INSERT INTO type_conge (code, libelle, jours_acquis_mois, justificatif_obligatoire)
VALUES
    ('ANNUEL', 'Congé annuel', 2.50, FALSE),
    ('MALADIE', 'Congé maladie', 0, TRUE),
    ('MATERNITE', 'Congé maternité', 0, TRUE),
    ('SANS_SOLDE', 'Congé sans solde', 0, FALSE)
ON CONFLICT (code) DO NOTHING;
