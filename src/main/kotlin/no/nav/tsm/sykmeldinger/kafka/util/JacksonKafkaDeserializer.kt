package no.nav.tsm.sykmeldinger.kafka.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class JacksonKafkaDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {
    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {}

    override fun deserialize(topic: String, data: ByteArray?): T? {
        if (data == null || data.isEmpty()) {
            return null
        }
        return objectMapper.readValue(data, type.java)
    }

    override fun close() {}
}
