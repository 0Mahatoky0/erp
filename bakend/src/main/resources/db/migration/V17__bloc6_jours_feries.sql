-- Bloc 6/7 : calendrier des jours feries utilise pour le decompte des conges.

CREATE TABLE jour_ferie (
    id_jour_ferie BIGSERIAL PRIMARY KEY,
    date_ferie DATE NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    recurrent BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO jour_ferie (date_ferie, libelle, recurrent) VALUES
    ('2026-01-01', 'Nouvel an', TRUE),
    ('2026-03-29', 'Commémoration des Martyrs', TRUE),
    ('2026-05-01', 'Fête du Travail', TRUE),
    ('2026-06-26', 'Fête de l''Indépendance', TRUE),
    ('2026-08-15', 'Assomption', TRUE),
    ('2026-11-01', 'Toussaint', TRUE),
    ('2026-12-25', 'Noël', TRUE)
ON CONFLICT (date_ferie) DO NOTHING;
