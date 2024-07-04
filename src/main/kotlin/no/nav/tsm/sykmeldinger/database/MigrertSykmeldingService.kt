package no.nav.tsm.sykmeldinger.database

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService.Sykmelding.fellesformat
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService.Sykmelding.gammelSykmelding
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService.Sykmelding.mottattdato
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService.Sykmelding.sykmelding_id
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils.Companion.getAivenKafkaConfig
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.toProducerConfig
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.min
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.time.measureTime

class MigrertSykmeldingService(private val kafkaProducer: KafkaProducer<String, MigrertSykmelding>) {

    object Sykmelding : Table() {
        val sykmelding_id = text("sykmelding_id")
        val mottattdato = datetime("mottattdato")
        val fellesformat = text("fellesformat")
        val gammelSykmelding = text("gammel_sykmelding")
        val migrert = bool("migrert")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    object Sykmelding_migrert : Table() {
        val sykmelding_id = text("sykmelding_id")
        val mottattdato = datetime("mottattdato")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrertSykmeldingService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    @WithSpan
    suspend fun selectSykmeldingerAndProduce() {
        var shouldRun = true
        val maxEmptyCount = 10
        var emptyCount = 0
        var sykmeldingCount = 0
        var lastTimestamp = transaction {
            (Sykmelding_migrert
                .select(Sykmelding_migrert.mottattdato.max())
                .singleOrNull()?.get(Sykmelding_migrert.mottattdato.max())
                ?: Sykmelding.select(mottattdato.min())
                    .first()[mottattdato.min()])?.toLocalDate()?.atStartOfDay() ?: throw Exception("No timestamp found")
        }
        logger.info("Starting to retrieve sykmeldinger from $lastTimestamp")
        kafkaProducer.initTransactions()
        while(shouldRun) {
                val result = measureTime {
                    dbQuery {
                        try {
                            kafkaProducer.beginTransaction()
                            val sykmeldinger = getSykmeldinger(lastTimestamp)

                            insertMigratedSykmeldinger(sykmeldinger)

                            sykmeldinger.forEach { migrertSykmelding ->
                                produceToKafka(migrertSykmelding, kafkaProducer)
                            }

                            if (sykmeldinger.isNotEmpty()) {
                                emptyCount = 0
                            } else {
                                emptyCount++
                                if (emptyCount >= maxEmptyCount) {
                                    shouldRun = false
                                }
                            }

                            sykmeldingCount = sykmeldinger.size
                            lastTimestamp = lastTimestamp.plusDays(1)

                            kafkaProducer.commitTransaction()
                        } catch (e: Exception) {
                            logger.error("Error while processing sykmeldinger", e)
                            kafkaProducer.abortTransaction()
                            throw e
                        }
                    }
                }
                logger.info("time: ${result.inWholeMilliseconds}: Sykmeldinger $sykmeldingCount: Date: ${lastTimestamp.minusDays(1)}")
            }
        }



    private fun insertMigratedSykmeldinger(sykmeldinger: List<MigrertSykmelding>) {
        Sykmelding_migrert.batchInsert(sykmeldinger) {
            this[Sykmelding_migrert.sykmelding_id] = it.sykmeldingId
            this[Sykmelding_migrert.mottattdato] = it.mottattDato
        }
    }

    private fun getSykmeldinger(lastTimestamp: LocalDateTime) =
        Sykmelding.select(sykmelding_id, mottattdato, fellesformat, gammelSykmelding)
            .where { mottattdato greaterEq lastTimestamp }
            .andWhere { mottattdato less lastTimestamp.plusDays(1) }
            .andWhere {
                notExists(
                    Sykmelding_migrert.select(Sykmelding_migrert.sykmelding_id).where {
                        Sykmelding_migrert.sykmelding_id eq sykmelding_id
                    }
                )
            }
            .orderBy(mottattdato, ASC)
            .map {
                MigrertSykmelding(
                    it[sykmelding_id],
                    it[mottattdato],
                    it[fellesformat],
                    it[gammelSykmelding]
                )
            }


    fun produceToKafka(
        migrertSykmelding: MigrertSykmelding,
        migrertSykmeldingKafkaProducer: KafkaProducer<String, MigrertSykmelding>
    ) {
        migrertSykmeldingKafkaProducer.send(
            ProducerRecord(
                "tsm.migrert-sykmelding",
                migrertSykmelding.sykmeldingId,
                migrertSykmelding
            )
        )
    }
}
