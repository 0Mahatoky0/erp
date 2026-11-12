-- Bloc 6 : cloture mensuelle des periodes de temps (RG-B6-18).

CREATE TABLE periode_temps_close (
    id_periode_close BIGSERIAL PRIMARY KEY,
    annee INTEGER NOT NULL,
    mois INTEGER NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    id_utilisateur BIGINT,
    date_cloture TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_periode_close_mois UNIQUE (annee, mois),
    CONSTRAINT ck_periode_close_mois CHECK (mois BETWEEN 1 AND 12)
);

CREATE INDEX idx_periode_close_dates ON periode_temps_close (date_debut, date_fin);
