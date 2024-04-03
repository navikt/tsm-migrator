package no.nav.tsm.sykmeldinger.kafka.util


import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.sykmeldinger.kafka.model.FellesformatInput
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class FellesformatDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }

    override fun deserialize(topic: String, p1: ByteArray): T {
        val jsonNode: JsonNode = objectMapper.readTree(p1)
        val sykmeldingId = jsonNode.get("sykmelding").get("id").asText()
        val fellesformat = jsonNode.get("fellesformat").asText()
        return FellesformatInput(sykmeldingId, fellesformat) as T
    }
}