-- Consentement explicite du candidat (RG-B2, RGPD) et rang FIFO de la liste d'attente.
ALTER TABLE candidat
    ADD COLUMN consentement_donnees BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE candidat
    ADD COLUMN date_consentement TIMESTAMP;

ALTER TABLE decision_candidature
    ADD COLUMN rang_liste_attente INTEGER;

CREATE UNIQUE INDEX uq_decision_candidature_rang_attente
    ON decision_candidature (rang_liste_attente)
    WHERE rang_liste_attente IS NOT NULL;
