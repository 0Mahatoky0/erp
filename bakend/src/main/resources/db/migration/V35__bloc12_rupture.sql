CREATE TABLE IF NOT EXISTS dossier_rupture (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(30) NOT NULL UNIQUE,
    id_contrat BIGINT NOT NULL,
    id_employe BIGINT NOT NULL,
    motif VARCHAR(40) NOT NULL,
    initiateur VARCHAR(30) NOT NULL,
    date_notification DATE NOT NULL,
    date_preavis_debut DATE,
    date_preavis_fin DATE,
    date_sortie DATE,
    preavis_jours INT NOT NULL DEFAULT 0,
    preavis_mode VARCHAR(30) NOT NULL,
    statut VARCHAR(30) NOT NULL,
    motif_autre VARCHAR(255),
    description TEXT,
    date_decision DATE,
    id_procedure_disciplinaire BIGINT,
    id_initiateur BIGINT,
    id_validateur BIGINT,
    date_validation TIMESTAMP,
    date_archivage DATE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dossier_rupture_id_employe ON dossier_rupture(id_employe);
CREATE INDEX IF NOT EXISTS idx_dossier_rupture_statut ON dossier_rupture(statut);
CREATE INDEX IF NOT EXISTS idx_dossier_rupture_date_sortie ON dossier_rupture(date_sortie);

CREATE TABLE IF NOT EXISTS droits_rupture (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL UNIQUE,
    conge_solde NUMERIC(18,2),
    conge_details TEXT,
    conges_futurs_annules INT DEFAULT 0,
    primes_dues NUMERIC(18,2),
    avances_restantes NUMERIC(18,2),
    dedit_formation NUMERIC(18,2),
    details TEXT,
    date_arret TIMESTAMP
);

CREATE TABLE IF NOT EXISTS solde_tout_compte (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL UNIQUE,
    salaire_du NUMERIC(18,2),
    indemnite_preavis NUMERIC(18,2),
    indemnite_licenciement NUMERIC(18,2),
    indemnite_conges NUMERIC(18,2),
    primes NUMERIC(18,2),
    retenues_avances NUMERIC(18,2),
    retenues_dedit NUMERIC(18,2),
    retenues_materiel NUMERIC(18,2),
    total_brut NUMERIC(18,2),
    total_retenues NUMERIC(18,2),
    net_avant_cotisations NUMERIC(18,2),
    montant_paye NUMERIC(18,2),
    devise VARCHAR(10),
    statut VARCHAR(30),
    date_preparation TIMESTAMP,
    id_calculateur BIGINT,
    date_validation TIMESTAMP,
    id_validateur BIGINT,
    date_paiement DATE,
    reference_paiement VARCHAR(100),
    mode_paiement VARCHAR(50),
    motif_refus TEXT,
    id_bulletin_paie BIGINT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ligne_solde_rupture (
    id BIGSERIAL PRIMARY KEY,
    id_solde BIGINT NOT NULL,
    code VARCHAR(40),
    libelle VARCHAR(150),
    categorie VARCHAR(20),
    base NUMERIC(18,2),
    quantite NUMERIC(18,2),
    taux NUMERIC(18,2),
    montant NUMERIC(18,2),
    source VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_ligne_solde_rupture_id_solde ON ligne_solde_rupture(id_solde);

CREATE TABLE IF NOT EXISTS tache_depart (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL,
    code VARCHAR(50),
    libelle VARCHAR(150),
    categorie VARCHAR(50),
    responsable VARCHAR(100),
    statut VARCHAR(30) NOT NULL,
    date_echeance DATE,
    date_cloture TIMESTAMP,
    id_responsable BIGINT,
    bloquante BOOLEAN DEFAULT FALSE,
    commentaire TEXT
);

CREATE INDEX IF NOT EXISTS idx_tache_depart_id_dossier ON tache_depart(id_dossier);

CREATE TABLE IF NOT EXISTS materiel_rupture (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL,
    libelle VARCHAR(150),
    type VARCHAR(80),
    numero_serie VARCHAR(100),
    statut VARCHAR(30),
    date_restitution DATE,
    montant_retenue NUMERIC(18,2)
);

CREATE INDEX IF NOT EXISTS idx_materiel_rupture_id_dossier ON materiel_rupture(id_dossier);

CREATE TABLE IF NOT EXISTS document_rupture (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL,
    code VARCHAR(50),
    libelle VARCHAR(150),
    format VARCHAR(20),
    chemin_ou_hash VARCHAR(255),
    hash_sha256 VARCHAR(128),
    id_document_employe BIGINT,
    date_generation TIMESTAMP,
    date_remise DATE,
    mode_remise VARCHAR(50),
    reference_preuve VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_document_rupture_id_dossier ON document_rupture(id_dossier);

CREATE TABLE IF NOT EXISTS entretien_rupture (
    id BIGSERIAL PRIMARY KEY,
    id_dossier BIGINT NOT NULL,
    date_entretien DATE,
    animateur_id BIGINT,
    questionnaire_json TEXT,
    satisfaction INT,
    commentaire_confidentiel TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_entretien_rupture_id_dossier ON entretien_rupture(id_dossier);

ALTER TABLE employe ADD COLUMN IF NOT EXISTS date_sortie DATE;

ALTER TABLE employe DROP CONSTRAINT IF EXISTS chk_employe_statut;
ALTER TABLE employe ADD CONSTRAINT chk_employe_statut
    CHECK (statut IN ('PRE_EMBAUCHE', 'ACTIF', 'INACTIF', 'PARTI', 'SORTI', 'NON_PRIS_POSTE'));
