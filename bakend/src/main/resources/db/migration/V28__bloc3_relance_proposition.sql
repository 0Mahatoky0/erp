-- Relance des propositions d'embauche envoyees et sans reponse a J+3 (RG-B3-05).
ALTER TABLE proposition_embauche ADD COLUMN relance_envoyee BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proposition_embauche ADD COLUMN date_relance TIMESTAMP;
