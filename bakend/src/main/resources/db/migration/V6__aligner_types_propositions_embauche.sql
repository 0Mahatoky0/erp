-- Alignement des identifiants historiques du bloc 3 avec les Long JPA.
-- Les migrations precedentes restent immuables.

DO $$
DECLARE
    contrainte RECORD;
BEGIN
    FOR contrainte IN
        SELECT DISTINCT
               ns.nspname AS schema_name,
               c.conname AS constraint_name,
               nt.nspname AS table_schema,
               cl.relname AS table_name
        FROM pg_constraint c
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_namespace nt ON nt.oid = cl.relnamespace
        JOIN pg_namespace ns ON ns.oid = c.connamespace
        WHERE c.contype = 'f'
          AND (
              c.conrelid IN (
                  'proposition_embauche'::regclass,
                  'reponse_proposition'::regclass
              )
              OR c.confrelid IN (
                  'proposition_embauche'::regclass,
                  'reponse_proposition'::regclass
              )
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            contrainte.table_schema,
            contrainte.table_name,
            contrainte.constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE proposition_embauche
    ALTER COLUMN id_proposition_embauche TYPE BIGINT,
    ALTER COLUMN id_candidat TYPE BIGINT,
    ALTER COLUMN id_offre TYPE BIGINT;

ALTER TABLE reponse_proposition
    ALTER COLUMN id_proposition_embauche TYPE BIGINT,
    ALTER COLUMN id_candidat TYPE BIGINT;

ALTER TABLE proposition_embauche
    ADD CONSTRAINT fk_proposition_candidat
        FOREIGN KEY (id_candidat) REFERENCES candidat(id_candidat),
    ADD CONSTRAINT fk_proposition_offre
        FOREIGN KEY (id_offre) REFERENCES offre_emploi(id_offre),
    ADD CONSTRAINT fk_proposition_candidature
        FOREIGN KEY (id_candidature) REFERENCES candidature(id_candidature);

ALTER TABLE reponse_proposition
    ADD CONSTRAINT fk_reponse_proposition
        FOREIGN KEY (id_proposition_embauche)
        REFERENCES proposition_embauche(id_proposition_embauche),
    ADD CONSTRAINT fk_reponse_candidat
        FOREIGN KEY (id_candidat) REFERENCES candidat(id_candidat);
