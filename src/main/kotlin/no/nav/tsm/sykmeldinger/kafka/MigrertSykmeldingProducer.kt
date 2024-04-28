package no.nav.tsm.sykmeldinger.kafka

import no.nav.tsm.plugins.Environment
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.java.KoinJavaComponent
import java.util.*
import kotlin.reflect.KClass


fun Properties.toProducerConfig(
    clientIdConfig: String,
    valueSerializer: KClass<out Serializer<out Any>>,
    keySerializer: KClass<out Serializer<out Any>> = StringSerializer::class
): Properties =
    Properties().also {
        it.putAll(this)
        it[ProducerConfig.CLIENT_ID_CONFIG] = clientIdConfig
        it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = valueSerializer.java
        it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = keySerializer.java
    }


class MigrertSykmeldingProducer(
    private val env: Environment,
) {
    fun getKafkaProducerConfig(clientId: String): Properties {
        return KafkaUtils.getAivenKafkaConfig(clientId)
            .toProducerConfig(env.applicationName, valueSerializer = JacksonKafkaSerializer::class)
    }
// Korleis bør denne triggerast no når vi les ut alt og produserer undervegs? withCOntext(Dispatchers.IO) ?
// sjekk kor langt ein er komt i tilfelle shit hits the fan
    // kan ein streame frå DB?
}