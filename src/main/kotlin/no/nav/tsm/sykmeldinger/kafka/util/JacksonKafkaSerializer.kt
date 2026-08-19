package no.nav.tsm.sykmeldinger.kafka.util

import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

class JacksonKafkaSerializer<T: Any> : Serializer<T> {
    private val objectMapper: ObjectMapper = jacksonMapperBuilder()
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
        .build()

    override fun serialize(topic: String?, data: T?): ByteArray? {
        return when (data) {
            null -> null
            else -> objectMapper.writeValueAsBytes(data)
        }
    }

    override fun close() {}
}
