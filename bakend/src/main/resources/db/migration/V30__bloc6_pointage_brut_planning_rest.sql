-- Bloc 6 : pointage brut immuable, tracabilite du planning, repos et escalade.

ALTER TABLE planning_travail ADD COLUMN origine VARCHAR(20) NOT NULL DEFAULT 'MANUEL';
ALTER TABLE planning_travail ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIE';
ALTER TABLE planning_travail ADD COLUMN publie_le TIMESTAMP;
ALTER TABLE planning_travail ADD COLUMN motif_modification VARCHAR(500);
ALTER TABLE planning_travail ADD COLUMN date_modification TIMESTAMP;

ALTER TABLE anomalie_pointage ADD COLUMN escaladee BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE anomalie_pointage ADD COLUMN date_escalade TIMESTAMP;

CREATE TABLE pointage_brut (
    id_pointage_brut BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    horodatage TIMESTAMP NOT NULL,
    sens VARCHAR(20) NOT NULL DEFAULT 'INDETERMINE',
    source VARCHAR(30) NOT NULL DEFAULT 'MANUEL',
    terminal_id VARCHAR(80),
    terminal_authentifie BOOLEAN NOT NULL DEFAULT FALSE,
    lieu_id BIGINT,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    statut VARCHAR(20) NOT NULL DEFAULT 'VALIDE',
    saisi_par BIGINT,
    motif VARCHAR(500),
    id_pointage_brut_remplace BIGINT REFERENCES pointage_brut(id_pointage_brut),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pointage_brut_sens CHECK (sens IN ('ENTREE', 'SORTIE', 'INDETERMINE')),
    CONSTRAINT ck_pointage_brut_source CHECK (source IN ('BIOMETRIQUE', 'BADGE', 'WEB', 'MOBILE', 'MANUEL')),
    CONSTRAINT ck_pointage_brut_statut CHECK (statut IN ('VALIDE', 'A_VERIFIER', 'REJETE'))
);

CREATE INDEX idx_pointage_brut_employe_horodatage
    ON pointage_brut (id_employe, horodatage);
