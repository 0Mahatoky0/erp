-- Bloc 6 : synthese journaliere, anomalies et heures supplementaires.

ALTER TABLE planning_travail ADD COLUMN pause_minutes INTEGER NOT NULL DEFAULT 60;
ALTER TABLE planning_travail ADD COLUMN tolerance_minutes INTEGER NOT NULL DEFAULT 5;

CREATE TABLE jour_travaille (
    id_jour_travaille BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    date_jour DATE NOT NULL,
    id_planning BIGINT REFERENCES planning_travail(id_planning),
    heure_entree TIME,
    heure_sortie TIME,
    minutes_presence INTEGER NOT NULL DEFAULT 0,
    minutes_pause INTEGER NOT NULL DEFAULT 0,
    minutes_travaillees INTEGER NOT NULL DEFAULT 0,
    minutes_theoriques INTEGER NOT NULL DEFAULT 0,
    minutes_retard INTEGER NOT NULL DEFAULT 0,
    minutes_depart_anticipe INTEGER NOT NULL DEFAULT 0,
    minutes_hs INTEGER NOT NULL DEFAULT 0,
    minutes_nuit INTEGER NOT NULL DEFAULT 0,
    minutes_dimanche INTEGER NOT NULL DEFAULT 0,
    minutes_ferie INTEGER NOT NULL DEFAULT 0,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_VALIDER',
    date_calcul TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_jour_travaille_employe_date UNIQUE (id_employe, date_jour),
    CONSTRAINT ck_jour_travaille_statut CHECK (statut IN ('CONFORME', 'ANOMALIE', 'ABSENT', 'CONGE', 'FERIE', 'A_VALIDER', 'VALIDE'))
);

CREATE INDEX idx_jour_travaille_employe_date ON jour_travaille (id_employe, date_jour);

CREATE TABLE anomalie_pointage (
    id_anomalie BIGSERIAL PRIMARY KEY,
    id_jour_travaille BIGINT REFERENCES jour_travaille(id_jour_travaille),
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    date_anomalie DATE NOT NULL,
    type VARCHAR(40) NOT NULL,
    gravite VARCHAR(20) NOT NULL DEFAULT 'MOYENNE',
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERTE',
    resolution VARCHAR(500),
    id_resolu_par BIGINT,
    date_resolution TIMESTAMP,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_anomalie_statut CHECK (statut IN ('OUVERTE', 'JUSTIFIEE', 'CORRIGEE', 'REJETEE'))
);

CREATE INDEX idx_anomalie_employe_date ON anomalie_pointage (id_employe, date_anomalie);
CREATE INDEX idx_anomalie_statut ON anomalie_pointage (statut);

CREATE TABLE heure_supplementaire (
    id_heure_sup BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    semaine_debut DATE NOT NULL,
    minutes_total INTEGER NOT NULL DEFAULT 0,
    minutes_hs_30 INTEGER NOT NULL DEFAULT 0,
    minutes_hs_50 INTEGER NOT NULL DEFAULT 0,
    minutes_nuit INTEGER NOT NULL DEFAULT 0,
    minutes_dimanche INTEGER NOT NULL DEFAULT 0,
    minutes_ferie INTEGER NOT NULL DEFAULT 0,
    statut VARCHAR(20) NOT NULL DEFAULT 'DETECTEE',
    alerte_plafond BOOLEAN NOT NULL DEFAULT FALSE,
    date_calcul TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_heure_sup_employe_semaine UNIQUE (id_employe, semaine_debut),
    CONSTRAINT ck_heure_sup_statut CHECK (statut IN ('DETECTEE', 'DEMANDEE', 'VALIDEE', 'REFUSEE', 'PAYEE', 'RECUPEREE'))
);

CREATE INDEX idx_heure_sup_employe_semaine ON heure_supplementaire (id_employe, semaine_debut);
