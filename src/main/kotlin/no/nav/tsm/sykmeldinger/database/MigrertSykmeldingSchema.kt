package no.nav.tsm.sykmeldinger.database

import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
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

    // Read from Sykmelding table where sykmelding_id = id
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }


    suspend fun selectSykmeldingerAndProduce(migrertSykmeldingKafkaProducer: KafkaProducer<String, MigrertSykmelding>) = dbQuery {
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
                migrerteSykmeldinger.forEach { migrertSykmelding ->
                    produceToKafka(migrertSykmelding, migrertSykmeldingKafkaProducer)
                }
            }
        } while (migrerteSykmeldinger.isNotEmpty())
    }

    suspend fun produceToKafka(
        migrertSykmelding: MigrertSykmelding,
        migrertSykmeldingKafkaProducer: KafkaProducer<String, MigrertSykmelding>
    ) {
        migrertSykmeldingKafkaProducer.send(ProducerRecord(migrertSykmelding.sykmeldingId, migrertSykmelding))
    }
}