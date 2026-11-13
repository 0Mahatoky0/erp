-- Bloc 5 : dossier administratif complet (missions, alertes, demandes de
-- modification) et journal d'audit transversal des donnees sensibles.

CREATE TABLE mission (
    id_mission        BIGSERIAL PRIMARY KEY,
    id_employe        BIGINT NOT NULL REFERENCES employe(id_employe),
    reference         VARCHAR(40) UNIQUE,
    titre             VARCHAR(200) NOT NULL,
    description       TEXT,
    lieu              VARCHAR(150),
    date_debut        DATE NOT NULL,
    date_fin_prevue   DATE,
    date_fin_reelle   DATE,
    priorite          VARCHAR(20) NOT NULL DEFAULT 'NORMALE',
    statut            VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE',
    id_responsable    BIGINT,
    avancement        INTEGER NOT NULL DEFAULT 0,
    manao_asa         BOOLEAN,
    miassa_tsara      BOOLEAN,
    commentaire_suivi TEXT,
    date_creation     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mission_dates CHECK (
        date_fin_prevue IS NULL OR date_fin_prevue >= date_debut),
    CONSTRAINT ck_mission_avancement CHECK (avancement BETWEEN 0 AND 100),
    CONSTRAINT ck_mission_priorite CHECK (
        priorite IN ('BASSE', 'NORMALE', 'HAUTE', 'URGENTE')),
    CONSTRAINT ck_mission_statut CHECK (
        statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE'))
);

CREATE INDEX idx_mission_employe_dates ON mission (id_employe, date_debut, date_fin_prevue);

CREATE TABLE suivi_mission (
    id_suivi      BIGSERIAL PRIMARY KEY,
    id_mission    BIGINT NOT NULL REFERENCES mission(id_mission) ON DELETE CASCADE,
    date_suivi    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    avancement    INTEGER,
    manao_asa     BOOLEAN,
    miassa_tsara  BOOLEAN,
    commentaire   TEXT,
    id_auteur     BIGINT,
    CONSTRAINT ck_suivi_avancement CHECK (
        avancement IS NULL OR avancement BETWEEN 0 AND 100)
);

CREATE INDEX idx_suivi_mission ON suivi_mission (id_mission, date_suivi);

CREATE TABLE alerte (
    id_alerte      BIGSERIAL PRIMARY KEY,
    id_employe     BIGINT REFERENCES employe(id_employe),
    type_alerte    VARCHAR(40) NOT NULL,
    niveau         VARCHAR(20) NOT NULL DEFAULT 'AVERTISSEMENT',
    message        TEXT NOT NULL,
    date_echeance  DATE,
    lue            BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_alerte_employe_type_echeance
        UNIQUE (id_employe, type_alerte, date_echeance),
    CONSTRAINT ck_alerte_type CHECK (type_alerte IN (
        'DOCUMENT_EXPIRE', 'DOCUMENT_EXPIRANT', 'DOCUMENT_MANQUANT',
        'RETRAITE_PROCHE', 'MISSION_EN_RETARD')),
    CONSTRAINT ck_alerte_niveau CHECK (
        niveau IN ('INFO', 'AVERTISSEMENT', 'CRITIQUE'))
);

CREATE INDEX idx_alerte_employe_lue ON alerte (id_employe, lue);

CREATE TABLE demande_modification (
    id_demande        BIGSERIAL PRIMARY KEY,
    id_employe        BIGINT NOT NULL REFERENCES employe(id_employe),
    champ             VARCHAR(80) NOT NULL,
    ancienne_valeur   TEXT,
    nouvelle_valeur   TEXT NOT NULL,
    motif             TEXT,
    statut            VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    id_demandeur      BIGINT,
    id_validateur     BIGINT,
    motif_decision    TEXT,
    date_decision     TIMESTAMP,
    date_creation     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_demande_modification_statut CHECK (
        statut IN ('EN_ATTENTE', 'APPROUVEE', 'REFUSEE'))
);

CREATE INDEX idx_demande_modification_employe
    ON demande_modification (id_employe, statut);

CREATE TABLE journal_audit (
    id_audit          BIGSERIAL PRIMARY KEY,
    id_utilisateur    BIGINT,
    role_utilisateur  VARCHAR(40),
    action            VARCHAR(40) NOT NULL,
    ressource         VARCHAR(120) NOT NULL,
    id_ressource      BIGINT,
    avant             TEXT,
    apres             TEXT,
    adresse_ip        VARCHAR(64),
    date_action       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_audit_ressource
    ON journal_audit (ressource, id_ressource, date_action);
CREATE INDEX idx_journal_audit_utilisateur
    ON journal_audit (id_utilisateur, date_action);
