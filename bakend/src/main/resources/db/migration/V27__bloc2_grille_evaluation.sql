-- Grille d'evaluation ponderee, criteres eliminatoires et classement des candidatures (bloc 2).
ALTER TABLE offre_emploi ADD COLUMN criteres_eliminatoires TEXT;
ALTER TABLE offre_emploi ADD COLUMN grille_evaluation TEXT;
ALTER TABLE offre_emploi ADD COLUMN poids_dossier INTEGER NOT NULL DEFAULT 1;
ALTER TABLE offre_emploi ADD COLUMN poids_entretien INTEGER NOT NULL DEFAULT 1;
ALTER TABLE offre_emploi ADD COLUMN poids_test INTEGER NOT NULL DEFAULT 1;

ALTER TABLE evaluation_candidat ADD COLUMN criteres TEXT;
ALTER TABLE evaluation_candidat ADD COLUMN note_globale NUMERIC(5, 2);

ALTER TABLE candidature ADD COLUMN score_dossier NUMERIC(5, 2);
ALTER TABLE candidature ADD COLUMN score_global NUMERIC(5, 2);
ALTER TABLE candidature ADD COLUMN rang INTEGER;

CREATE INDEX idx_candidature_offre_rang ON candidature (id_offre, rang);
