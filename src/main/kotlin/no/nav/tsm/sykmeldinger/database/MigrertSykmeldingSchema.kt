package no.nav.tsm.sykmeldinger.database

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils.Companion.getAivenKafkaConfig
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.toProducerConfig
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class MigrertSykmeldingService() {

    private object Sykmelding : Table() {
        val sykmelding_id = text("sykmelding_id")
        val mottattdato = datetime("mottattdato")
        val fellesformat = text("fellesformat")
        val gammelSykmelding = text("gammel_sykmelding")
        val migrert = bool("migrert")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrertSykmeldingService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    val kafkaProducer = KafkaProducer<String, MigrertSykmelding>(
        getAivenKafkaConfig("migrator-migrert-sykmelding-producer").toProducerConfig(
            "migrator-migrert-sykmelding-producer",
            JacksonKafkaSerializer::class,
        )
    )

    @WithSpan
    suspend fun selectSykmeldingerAndProduce() =
        dbQuery {
            logger.info("Starting to retrieve sykmeldinger from the database")
            var sisteMottattDato: LocalDateTime? = null
            do {
                val migrerteSykmeldinger = Sykmelding.selectAll().apply {
                    if (sisteMottattDato != null) {
                        andWhere { Sykmelding.mottattdato greaterEq sisteMottattDato!! }
                    }
                }
                    .where { Sykmelding.migrert eq false }
                    .orderBy(Sykmelding.mottattdato to SortOrder.ASC)
                    .limit(500)
                    .map {
                        MigrertSykmelding(
                            sykmeldingId = it[Sykmelding.sykmelding_id],
                            mottattDato = it[Sykmelding.mottattdato],
                            fellesformat = it[Sykmelding.fellesformat],
                            gammelSykmelding = it[Sykmelding.gammelSykmelding]
                        )
                    }

                if (migrerteSykmeldinger.isNotEmpty()) {
                    sisteMottattDato = migrerteSykmeldinger.last().mottattDato
                    logger.info("Retrieved ${migrerteSykmeldinger.size} sykmeldinger from the database")
                    migrerteSykmeldinger.forEach { migrertSykmelding ->
                        produceToKafka(migrertSykmelding, kafkaProducer)
                        Sykmelding.update({ Sykmelding.sykmelding_id eq migrertSykmelding.sykmeldingId }) {
                            it[migrert] = true
                        }
                    }
                } else
                    logger.info("No sykmeldinger retrieved from the database")

            } while (migrerteSykmeldinger.isNotEmpty())
        }

    fun produceToKafka(
        migrertSykmelding: MigrertSykmelding,
        migrertSykmeldingKafkaProducer: KafkaProducer<String, MigrertSykmelding>
    ) {
        logger.info("Producing migrert sykmelding with id ${migrertSykmelding.sykmeldingId}")
        migrertSykmeldingKafkaProducer.send(
            ProducerRecord(
                "tsm.migrert-sykmelding",
                migrertSykmelding.sykmeldingId,
                migrertSykmelding
            )
        )
    }

    // relevant https://github.com/navikt/macgyver/commit/2746ff51a3bf6adc5fcbc8ab01cbf6c52cc7e50c
}