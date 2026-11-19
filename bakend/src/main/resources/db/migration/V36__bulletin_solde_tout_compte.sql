ALTER TABLE bulletin_paie DROP CONSTRAINT uq_bulletin_periode_employe;
ALTER TABLE bulletin_paie ADD CONSTRAINT uq_bulletin_periode_employe_type
    UNIQUE (id_periode, id_employe, type);
ALTER TABLE bulletin_paie ADD CONSTRAINT chk_bulletin_paie_type
    CHECK (type IN ('NORMAL', 'SOLDE_TOUT_COMPTE'));
