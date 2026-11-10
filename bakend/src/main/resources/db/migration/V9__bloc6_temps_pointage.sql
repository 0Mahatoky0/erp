CREATE TABLE planning_travail (
    id_planning       BIGSERIAL PRIMARY KEY,
    id_employe        BIGINT NOT NULL REFERENCES employe(id_employe),
    jours_travailles  VARCHAR(100) NOT NULL,
    heure_debut       TIME NOT NULL,
    heure_fin         TIME NOT NULL,
    date_debut        DATE NOT NULL,
    date_fin          DATE,
    date_creation     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_planning_heures CHECK (heure_fin > heure_debut),
    CONSTRAINT ck_planning_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX idx_planning_employe_dates
    ON planning_travail (id_employe, date_debut, date_fin);

CREATE TABLE pointage (
    id_pointage       BIGSERIAL PRIMARY KEY,
    id_employe        BIGINT NOT NULL REFERENCES employe(id_employe),
    date_pointage     DATE NOT NULL,
    heure_entree      TIME,
    heure_sortie      TIME,
    source            VARCHAR(30) NOT NULL DEFAULT 'SAISIE_RH',
    date_creation     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pointage_employe_jour UNIQUE (id_employe, date_pointage),
    CONSTRAINT ck_pointage_heures CHECK (heure_sortie IS NULL OR heure_entree IS NOT NULL AND heure_sortie >= heure_entree)
);

CREATE INDEX idx_pointage_employe_date
    ON pointage (id_employe, date_pointage);
