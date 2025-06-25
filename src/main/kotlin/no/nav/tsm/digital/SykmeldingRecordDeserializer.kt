package no.nav.tsm.digital

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.common.serialization.Deserializer

class SykmeldingRecordDeserializer : Deserializer<SykmeldingRecord> {
    override fun deserialize(topic: String, value: ByteArray?): SykmeldingRecord? {
            return when(value) {
                null -> null
                else -> sykmeldingObjectMapper.readValue(value)
            }
    }
}
