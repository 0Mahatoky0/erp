CREATE TABLE droit_conge (
    id_droit_conge BIGSERIAL PRIMARY KEY,
    id_employe BIGINT NOT NULL REFERENCES employe(id_employe),
    id_type_conge BIGINT NOT NULL REFERENCES type_conge(id_type_conge),
    annee INTEGER NOT NULL,
    jours_autorises NUMERIC(6,2) NOT NULL,
    report_entrant NUMERIC(6,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_droit_conge UNIQUE (id_employe, id_type_conge, annee),
    CONSTRAINT ck_droit_jours CHECK (jours_autorises >= 0 AND report_entrant >= 0)
);

ALTER TABLE solde_conge ADD COLUMN jours_reserves NUMERIC(6,2) NOT NULL DEFAULT 0;
ALTER TABLE solde_conge ADD COLUMN id_droit_conge BIGINT REFERENCES droit_conge(id_droit_conge);
ALTER TABLE solde_conge ADD CONSTRAINT ck_solde_reserves CHECK (jours_reserves >= 0);

INSERT INTO droit_conge (id_employe, id_type_conge, annee, jours_autorises, report_entrant)
SELECT s.id_employe, s.id_type_conge, s.annee, s.jours_acquis, 0
FROM solde_conge s
ON CONFLICT (id_employe, id_type_conge, annee) DO NOTHING;

UPDATE solde_conge s
SET id_droit_conge = d.id_droit_conge
FROM droit_conge d
WHERE d.id_employe = s.id_employe
  AND d.id_type_conge = s.id_type_conge
  AND d.annee = s.annee;
