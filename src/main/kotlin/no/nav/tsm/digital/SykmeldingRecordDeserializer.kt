package no.nav.tsm.digital

import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.serialization.Deserializer
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

class SykmeldingRecordDeserializer : Deserializer<SykmeldingRecord> {

    val sykmeldingObjectMapper = jacksonMapperBuilder()
        .addModules(SykmeldingModule())
        .build()

    override fun deserialize(topic: String, value: ByteArray?): SykmeldingRecord? {
            return when(value) {
                null -> null
                else -> sykmeldingObjectMapper.readValue(value)
            }
    }
}
