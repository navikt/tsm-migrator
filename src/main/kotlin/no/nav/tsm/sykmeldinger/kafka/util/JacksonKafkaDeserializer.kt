package no.nav.tsm.sykmeldinger.kafka.util

import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.kafka.common.serialization.Deserializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.reflect.KClass

class JacksonKafkaDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {
    private val objectMapper: ObjectMapper =
        jacksonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
            .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
            .build()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {}

    override fun deserialize(topic: String, data: ByteArray?): T? {
        if (data == null || data.isEmpty()) {
            return null
        }
        return objectMapper.readValue(data, type.java)
    }

    override fun close() {}
}
