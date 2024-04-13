package no.nav.tsm.sykmeldinger.database

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.tsm.plugins.Environment
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils.Companion.getAivenKafkaConfig
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.toProducerConfig
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class MigrertSykmeldingService() {

    private object Sykmelding : Table() {
        val sykmelding_id = text("sykmelding_id")
        val mottattdato = datetime("mottattdato")
        val fellesformat = text("fellesformat")
        val gammelSykmelding = text("gammel_sykmelding")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrertSykmeldingService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }


    suspend fun selectSykmeldingerAndProduce(migrertSykmeldingKafkaProducer: KafkaProducer<String, MigrertSykmelding>) =
        dbQuery {
            var sisteMottattDato: LocalDateTime? = null
            do {
                val migrerteSykmeldinger = Sykmelding.selectAll().apply {
                    if (sisteMottattDato != null) {
                        andWhere { Sykmelding.mottattdato greaterEq sisteMottattDato!! }
                    }
                }
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
                        produceToKafka(migrertSykmelding, migrertSykmeldingKafkaProducer)
                    }
                }
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


    val kafkaProducer = KafkaProducer<String, MigrertSykmelding>(
        getAivenKafkaConfig("migrator-migrert-sykmelding-producer").toProducerConfig(
            "migrator-migrert-sykmelding-producer",
            JacksonKafkaSerializer::class,
        )
    )

    //TODO this is WORK IN PROGRESS and needs a rewrite to work here, copied from macgyver commit!!!!!
    //TODO  create endpoint to trigger the job that reads which will call the producer.
    // relevant https://github.com/navikt/macgyver/commit/2746ff51a3bf6adc5fcbc8ab01cbf6c52cc7e50c

//    @OptIn(DelicateCoroutinesApi::class)
//    fun Route.setupMigrertsykmeldingApi() {
//        var running = false
//        var error = false
//        post("/api/migrert") {
//            val year = call.parameters.get("year")?.toIntOrNull()
//            if (year == null) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "year is missing or not a number"))
//                return@post
//            }
//            if (running) {
//                call.respond(HttpStatusCode.Conflict, mapOf("message" to "Job is already running"))
//                return@post
//            }
//            call.respond(HttpStatusCode.Accepted, mapOf("running" to true))
//            running = true
//            error = false
//            GlobalScope.launch(Dispatchers.IO) {
//                logger.info("Getting sykmeldinger_ids from smregister")
//                try {
//                    selectSykmeldingerAndProduce(kafkaProducer)
//                } catch (e: Exception) {
//                    logger.error("Failed to get migrerte sykmeldinger from migrator DB", e)
//                } finally {
//                    running = false
//                }
//            }
//        }
//    }
}