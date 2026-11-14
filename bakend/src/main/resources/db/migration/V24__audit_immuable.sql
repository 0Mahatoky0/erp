-- Journal d'audit immuable et chaine cryptographiquement (cahier 2.3, RG-T-01 a RG-T-05).
ALTER TABLE journal_audit
    ADD COLUMN user_agent      VARCHAR(512),
    ADD COLUMN motif           VARCHAR(500),
    ADD COLUMN correlation_id  VARCHAR(64),
    ADD COLUMN hash_precedent  VARCHAR(64),
    ADD COLUMN hash            VARCHAR(64);

CREATE INDEX idx_journal_audit_correlation
    ON journal_audit (correlation_id);

-- Le journal est append-only : on interdit les mises a jour et suppressions directes.
-- (Les privileges de l'application restent a restreindre en production.)
CREATE OR REPLACE FUNCTION journal_audit_interdit_modification()
    RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Le journal d''audit est immuable (RG-T-01).';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_journal_audit_immuable ON journal_audit;
CREATE TRIGGER trg_journal_audit_immuable
    BEFORE UPDATE OR DELETE ON journal_audit
    FOR EACH ROW EXECUTE FUNCTION journal_audit_interdit_modification();
