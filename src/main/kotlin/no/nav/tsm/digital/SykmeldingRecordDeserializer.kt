package no.nav.tsm.digital

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.common.serialization.Deserializer

class SykmeldingRecordDeserializer : Deserializer<SykmeldingRecord> {

    val sykmeldingObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    override fun deserialize(topic: String, value: ByteArray?): SykmeldingRecord? {
            return when(value) {
                null -> null
                else -> sykmeldingObjectMapper.readValue(value)
            }
    }
}
