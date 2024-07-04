package no.nav.tsm.sykmeldinger.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.sykmeldinger.database.GamleSykmeldingerService
import no.nav.tsm.sykmeldinger.kafka.model.GamleSykmeldingerInput
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.reflect.KClass


class GamleSykmeldingerDeserializer() : Deserializer<GamleSykmeldingerInput> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }

    companion object {
        val securelog: Logger = LoggerFactory.getLogger("securelog")
    }

    override fun deserialize(topic: String, p1: ByteArray): GamleSykmeldingerInput {
        val jsonNode: JsonNode = objectMapper.readTree(p1)
        securelog.info("GamleSykmeldingerDeserializer.deserialize: jsonNode = $jsonNode")
        val sykmeldingId = jsonNode.get("receivedSykmelding").get("sykmelding").get("id").asText()
        val mottattDato = LocalDateTime.parse(jsonNode.get("receivedSykmelding").get("mottattDato").asText())
        val gammelSykmelding = jsonNode.get("receivedSykmelding").get("sykmelding").toString()
        return GamleSykmeldingerInput(sykmeldingId, mottattDato, gammelSykmelding)
    }
}
