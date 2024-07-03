CREATE TABLE sykmelding_migrert
(
    sykmelding_id Text PRIMARY KEY,
    mottattdato TIMESTAMP with time zone
);
CREATE INDEX idx_migrert_mottattdato on sykmelding_migrert (mottattdato);
