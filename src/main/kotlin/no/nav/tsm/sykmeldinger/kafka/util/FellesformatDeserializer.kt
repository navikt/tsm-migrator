package no.nav.tsm.sykmeldinger.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class FellesformatDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }
    override fun deserialize(topic: String, p1: ByteArray): T {
        return objectMapper.readValue(p1, type.java)
    }
}