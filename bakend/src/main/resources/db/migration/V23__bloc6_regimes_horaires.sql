-- Bloc 6 : regimes de travail, plages horaires types et affectations
-- historisees (RG-B6-01 a RG-B6-04).

CREATE TABLE regime_travail (
    id_regime              BIGSERIAL PRIMARY KEY,
    code                   VARCHAR(40) NOT NULL UNIQUE,
    libelle                VARCHAR(120) NOT NULL,
    type                   VARCHAR(30) NOT NULL DEFAULT 'HORAIRE_FIXE',
    heures_hebdo           INTEGER NOT NULL DEFAULT 40,
    jours_travailles       VARCHAR(100) NOT NULL
                           DEFAULT 'LUNDI,MARDI,MERCREDI,JEUDI,VENDREDI',
    tolerance_retard_min   INTEGER NOT NULL DEFAULT 10,
    tolerance_depart_min   INTEGER NOT NULL DEFAULT 10,
    pause_minutes          INTEGER NOT NULL DEFAULT 60,
    pause_payee            BOOLEAN NOT NULL DEFAULT FALSE,
    decompte_heures_sup    BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_regime_type CHECK (type IN (
        'HORAIRE_FIXE', 'HORAIRE_VARIABLE', 'POSTES_ROTATIFS',
        'FORFAIT_JOURS', 'TEMPS_PARTIEL')),
    CONSTRAINT ck_regime_heures CHECK (heures_hebdo BETWEEN 1 AND 80),
    CONSTRAINT ck_regime_tolerances CHECK (
        tolerance_retard_min BETWEEN 0 AND 120
        AND tolerance_depart_min BETWEEN 0 AND 120),
    CONSTRAINT ck_regime_pause CHECK (pause_minutes BETWEEN 0 AND 240)
);

CREATE TABLE horaire_travail (
    id_horaire      BIGSERIAL PRIMARY KEY,
    id_regime       BIGINT NOT NULL REFERENCES regime_travail(id_regime) ON DELETE CASCADE,
    jour_semaine    VARCHAR(20) NOT NULL,
    heure_debut     TIME NOT NULL,
    heure_fin       TIME NOT NULL,
    pause_debut     TIME,
    pause_fin       TIME,
    CONSTRAINT ck_horaire_jour CHECK (jour_semaine IN (
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE')),
    CONSTRAINT ck_horaire_plage CHECK (heure_fin > heure_debut),
    CONSTRAINT ck_horaire_pause CHECK (
        (pause_debut IS NULL AND pause_fin IS NULL)
        OR (pause_debut IS NOT NULL AND pause_fin IS NOT NULL
            AND pause_fin > pause_debut
            AND pause_debut >= heure_debut
            AND pause_fin <= heure_fin))
);

CREATE INDEX idx_horaire_regime ON horaire_travail (id_regime, jour_semaine, heure_debut);

CREATE TABLE affectation_regime (
    id_affectation  BIGSERIAL PRIMARY KEY,
    id_employe      BIGINT NOT NULL REFERENCES employe(id_employe),
    id_regime       BIGINT NOT NULL REFERENCES regime_travail(id_regime),
    date_debut      DATE NOT NULL,
    date_fin        DATE,
    id_planning     BIGINT REFERENCES planning_travail(id_planning),
    date_creation   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_affectation_regime_dates CHECK (
        date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX idx_affectation_regime_employe
    ON affectation_regime (id_employe, date_debut, date_fin);
