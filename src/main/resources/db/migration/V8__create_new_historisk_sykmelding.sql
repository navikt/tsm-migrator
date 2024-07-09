create table historiske_sykmeldinger
(
    sykmelding_id Text PRIMARY KEY,
    mottattdato TIMESTAMP with time zone,
    receivedSykmelding jsonb,
    source Text
);

create INDEX historiske_sykmeldinger_idx on historiske_sykmeldinger(mottattdato);
