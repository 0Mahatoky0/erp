-- Un candidat possede obligatoirement un compte applicatif.
INSERT INTO role (nom)
SELECT 'CANDIDAT'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE nom = 'CANDIDAT');

ALTER TABLE candidat ADD COLUMN id_utilisateur BIGINT;

-- Les anciens profils sont lies a un compte desactive. Ils devront etre reactivés
-- par une inscription/reinitialisation avant toute connexion.
INSERT INTO utilisateur (id_role, nom, email, mot_de_passe, actif)
SELECT r.id_role, c.prenom || ' ' || c.nom, c.email,
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOe8QW6LhW4qvQ7cN9nQm3r3V2k6m6m6S',
       FALSE
FROM candidat c
JOIN role r ON r.nom = 'CANDIDAT'
WHERE NOT EXISTS (
    SELECT 1 FROM utilisateur u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(c.email))
);

UPDATE candidat c
SET id_utilisateur = u.id_utilisateur
FROM utilisateur u
WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(c.email))
  AND u.id_role = (SELECT id_role FROM role WHERE nom = 'CANDIDAT');

ALTER TABLE candidat ALTER COLUMN id_utilisateur SET NOT NULL;
ALTER TABLE candidat ADD CONSTRAINT uq_candidat_utilisateur UNIQUE (id_utilisateur);
ALTER TABLE candidat ADD CONSTRAINT fk_candidat_utilisateur
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur);
