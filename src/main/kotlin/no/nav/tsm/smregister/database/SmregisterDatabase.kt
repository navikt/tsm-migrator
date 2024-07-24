package no.nav.tsm.smregister.database

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.smregister.database.tables.Behandlingsutfall
import no.nav.tsm.smregister.database.tables.Sykmeldingsdokument
import no.nav.tsm.smregister.database.tables.Sykmeldingsopplysning
import no.nav.tsm.smregister.database.tables.Sykmeldingstatus
import no.nav.tsm.smregister.models.Merknad
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.Sykmelding
import no.nav.tsm.smregister.models.ValidationResult
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SmregisterDatabase(private val database: Database) {

    private val objectMapper =     ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) }

    companion object {
        private val secureLog: Logger = LoggerFactory.getLogger("securelog")
        private val logger = LoggerFactory.getLogger(SmregisterDatabase::class.java)
    }
    fun getSykmelding(id: String): String {
        return transaction(database) {
            val stm = connection.prepareStatement("select id from sykmeldingsopplysninger where id = ?", false)
            stm[1] = id
            val rs = stm.executeQuery()
            if (rs.next()) {
                val idFromDb = rs.getString(1)
                logger.info("got id from database $idFromDb")
                id
            } else {
                "not found"
            }
        }
    }

    suspend fun getFullSykmelding(sykmeldingId: String): ReceivedSykmelding? = withContext(Dispatchers.IO) {
        transaction(database) {
            try {
                val result = Sykmeldingsopplysning
                    .innerJoin(Sykmeldingsdokument) { Sykmeldingsopplysning.id eq Sykmeldingsdokument.id }
                    .innerJoin(Behandlingsutfall) { Sykmeldingsopplysning.id eq Behandlingsutfall.id }
                        .selectAll()
                    .where { Sykmeldingsopplysning.id eq sykmeldingId }
                    .map {
                        val sykmeldingsDokument = objectMapper.readValue<Sykmelding>(it[Sykmeldingsdokument.sykmelding])
                        val validationResult = objectMapper.readValue<ValidationResult>(it[Behandlingsutfall.behandlingsutfall])
                        ReceivedSykmelding(
                                    personNrPasient = it[Sykmeldingsopplysning.pasientFnr],
                                    personNrLege = it[Sykmeldingsopplysning.legeFnr],
                                    legeHprNr = it[Sykmeldingsopplysning.legeHpr],
                                    legeHelsepersonellkategori = it[Sykmeldingsopplysning.legeHelsepersonellKategori],
                                    navLogId = it[Sykmeldingsopplysning.mottakId],
                                    legekontorOrgNr = it[Sykmeldingsopplysning.legekontor_org_nr],
                                    legekontorHerId = it[Sykmeldingsopplysning.legekontorHerId],
                                    legekontorReshId = it[Sykmeldingsopplysning.legekontorReshId],
                                    mottattDato = it[Sykmeldingsopplysning.mottattTidspunkt],
                                    tssid = it[Sykmeldingsopplysning.tss_id],
                                    merknader = it[Sykmeldingsopplysning.merknader]?.let { merknader -> objectMapper.readValue<List<Merknad>>(merknader) },
                                    partnerreferanse = it[Sykmeldingsopplysning.partnerreferanse],
                                    utenlandskSykmelding = it[Sykmeldingsopplysning.utenlandskSykmelding]?.let { utenlandskSykmelding -> objectMapper.readValue(utenlandskSykmelding) },
                                    fellesformat = null,
                            rulesetVersion = null,
                            vedlegg = null,
                            legekontorOrgName = null,
                            sykmelding = sykmeldingsDokument,
                            msgId = it[Sykmeldingsopplysning.mottakId],
                            tlfPasient = null,
                            validationResult = validationResult
                        )
                    }
                val sykmeldingsInfo = result.firstOrNull()
                if(sykmeldingsInfo == null) {
                    logger.warn("No sykmelding found with id $sykmeldingId in database")
                }
                sykmeldingsInfo
            } catch (ex: Exception) {
                secureLog.error("Error getting sykmelding with id $sykmeldingId", ex)
                logger.error("Error getting sykmelding with id $sykmeldingId")
                throw ex
            }
        }
    }

    fun getValidationResult(sykmeldingId: String): ValidationResult? =
        transaction(database) {
            val validationResult = Behandlingsutfall
                .selectAll()
                .where { Behandlingsutfall.id eq sykmeldingId }
                .map { objectMapper.readValue<ValidationResult>(it[Behandlingsutfall.behandlingsutfall]) }
            validationResult.firstOrNull()
        }

}
