-- Bloc 10 : integrite temporelle de l'historique des remunerations.
-- Un contrat peut porter plusieurs periodes de salaire : la contrainte
-- historique du bloc 5 (une ligne par contrat) est donc supprimee.

DROP INDEX IF EXISTS uq_salaire_historique_contrat;

-- Le bloc 5 tolérait plusieurs lignes ouvertes pour un meme salarie lorsque
-- plusieurs contrats se succedaient. On les rend contigues avant de poser
-- l'unicite de la ligne courante.
WITH chronologie AS (
    SELECT id_salaire_historique,
           LEAD(date_effet) OVER (
               PARTITION BY id_employe
               ORDER BY date_effet, id_salaire_historique
           ) AS date_suivante
    FROM salaire_historique
    WHERE remplace_le IS NULL
),
corrections AS (
    SELECT s.id_salaire_historique,
           CASE
               WHEN c.date_suivante = s.date_effet THEN s.date_effet
               ELSE c.date_suivante - 1
           END AS nouvelle_date_fin
    FROM salaire_historique s
    JOIN chronologie c ON c.id_salaire_historique = s.id_salaire_historique
    WHERE s.date_fin IS NULL
      AND c.date_suivante IS NOT NULL
)
UPDATE salaire_historique s
SET date_fin = c.nouvelle_date_fin
FROM corrections c
WHERE s.id_salaire_historique = c.id_salaire_historique;

ALTER TABLE salaire_historique
    ADD CONSTRAINT chk_salaire_periode CHECK (date_fin IS NULL OR date_fin >= date_effet),
    ADD CONSTRAINT chk_salaire_montant CHECK (montant_brut >= 0);

CREATE UNIQUE INDEX uq_salaire_historique_courant
    ON salaire_historique (id_employe)
    WHERE date_fin IS NULL AND remplace_le IS NULL;

CREATE UNIQUE INDEX uq_salaire_historique_mouvement
    ON salaire_historique (id_mouvement, date_effet)
    WHERE id_mouvement IS NOT NULL AND remplace_le IS NULL;

CREATE OR REPLACE FUNCTION verifier_periode_salaire()
    RETURNS trigger AS $$
BEGIN
    IF NEW.date_fin IS NOT NULL AND NEW.date_fin < NEW.date_effet THEN
        RAISE EXCEPTION 'La periode de salaire est invalide.'
            USING ERRCODE = '23514';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM salaire_historique s
        WHERE s.id_employe = NEW.id_employe
          AND s.remplace_le IS NULL
          AND s.id_salaire_historique IS DISTINCT FROM NEW.id_salaire_historique
          AND s.date_effet <= COALESCE(NEW.date_fin, DATE '9999-12-31')
          AND COALESCE(s.date_fin, DATE '9999-12-31') >= NEW.date_effet
    ) THEN
        RAISE EXCEPTION 'Un salaire chevauche deja une periode de cet employe.'
            USING ERRCODE = '23P01';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_salaire_historique_periode ON salaire_historique;
CREATE TRIGGER trg_salaire_historique_periode
    BEFORE INSERT OR UPDATE ON salaire_historique
    FOR EACH ROW EXECUTE FUNCTION verifier_periode_salaire();

-- Instantes avant/apres exigés par la reconstitution et l'audit (RG-B10).
ALTER TABLE mouvement_carriere
    ADD COLUMN ancienne_situation TEXT,
    ADD COLUMN nouvelle_situation TEXT;
