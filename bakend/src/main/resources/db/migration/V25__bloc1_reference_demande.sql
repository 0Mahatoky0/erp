-- Reference metier lisible des demandes de recrutement (RG-B1-17, convention DR-AAAA-NNNN).
ALTER TABLE demande_recrutement
    ADD COLUMN reference VARCHAR(30);

CREATE UNIQUE INDEX uq_demande_recrutement_reference
    ON demande_recrutement (reference)
    WHERE reference IS NOT NULL;
