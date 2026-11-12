-- Bloc 7 : grand livre des conges, workflow manager -> RH -> Direction,
-- acquisition mensuelle plafonnee et parametrage.

ALTER TABLE type_conge ADD COLUMN paye BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE type_conge ADD COLUMN decompte_solde BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE type_conge ADD COLUMN delai_prevenance_jours INTEGER NOT NULL DEFAULT 0;
ALTER TABLE type_conge ADD COLUMN duree_max_jours INTEGER;
ALTER TABLE type_conge ADD COLUMN assimile_travail_effectif BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE type_conge SET decompte_solde = FALSE, assimile_travail_effectif = FALSE
WHERE code IN ('MALADIE', 'MATERNITE', 'SANS_SOLDE');
UPDATE type_conge SET delai_prevenance_jours = 7 WHERE code = 'ANNUEL';

ALTER TABLE demande_conge DROP CONSTRAINT IF EXISTS ck_demande_statut;
UPDATE demande_conge SET statut = 'SOUMISE' WHERE statut = 'EN_ATTENTE';
UPDATE demande_conge SET statut = 'APPROUVEE' WHERE statut = 'VALIDEE';
ALTER TABLE demande_conge ADD CONSTRAINT ck_demande_statut CHECK (
    statut IN ('BROUILLON', 'SOUMISE', 'VALIDEE_MANAGER', 'VALIDEE_RH',
               'APPROUVEE', 'EN_COURS', 'TERMINEE', 'REFUSEE', 'ANNULEE', 'ECOURTEE'));
ALTER TABLE demande_conge ADD COLUMN date_depot TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE demande_conge ADD COLUMN demi_journee_debut VARCHAR(10);
ALTER TABLE demande_conge ADD COLUMN demi_journee_fin VARCHAR(10);
ALTER TABLE demande_conge ADD COLUMN id_validateur_manager BIGINT;
ALTER TABLE demande_conge ADD COLUMN date_validation_manager TIMESTAMP;
ALTER TABLE demande_conge ADD COLUMN id_validateur_rh BIGINT;
ALTER TABLE demande_conge ADD COLUMN date_validation_rh TIMESTAMP;

CREATE TABLE mouvement_conge (
    id_mouvement_conge BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    id_type_conge BIGINT NOT NULL REFERENCES type_conge(id_type_conge),
    id_droit_conge BIGINT REFERENCES droit_conge(id_droit_conge),
    id_demande_conge BIGINT REFERENCES demande_conge(id_demande_conge),
    type_mouvement VARCHAR(20) NOT NULL,
    jours NUMERIC(6,2) NOT NULL,
    date_effet DATE NOT NULL,
    commentaire TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mouvement_type CHECK (type_mouvement IN (
        'ACQUISITION', 'RESERVATION', 'LIBERATION', 'CONSOMMATION',
        'REPORT', 'AJUSTEMENT', 'INDEMNISATION'))
);

CREATE INDEX idx_mouvement_conge_employe_type
    ON mouvement_conge (id_employe, id_type_conge, date_effet);

CREATE TABLE parametre_conge (
    cle VARCHAR(60) PRIMARY KEY,
    valeur VARCHAR(120) NOT NULL,
    description VARCHAR(255)
);

INSERT INTO parametre_conge (cle, valeur, description) VALUES
    ('MODE_DECOMPTE', 'OUVRABLES', 'OUVRABLES = lundi-samedi, OUVRES = lundi-vendredi'),
    ('PLAFOND_SOLDE', '90', 'Solde cumule maximal de conges en jours'),
    ('ALERTE_SOLDE', '72', 'Seuil d''alerte preventive (80 % du plafond)'),
    ('PREVENANCE_JOURS', '7', 'Delai de prevention pour un conge annuel long'),
    ('PREVENANCE_DUREE_JOURS', '3', 'Duree au-dela de laquelle le delai s''applique'),
    ('VALIDATION_DIRECTION_JOURS', '15', 'Duree au-dela de laquelle la Direction valide')
ON CONFLICT (cle) DO NOTHING;
