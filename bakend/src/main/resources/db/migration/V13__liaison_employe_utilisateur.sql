ALTER TABLE employe ADD COLUMN id_utilisateur BIGINT REFERENCES utilisateur(id_utilisateur);
CREATE UNIQUE INDEX uq_employe_utilisateur ON employe (id_utilisateur)
WHERE id_utilisateur IS NOT NULL;
