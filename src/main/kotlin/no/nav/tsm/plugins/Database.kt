package no.nav.tsm.plugins
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.inject


fun Application.configureDatabases() {
    val environment by inject<Environment>()
    Flyway.configure()
        .dataSource(environment.migratorJdbcUrl, environment.migratorDbUser, environment.migratorDbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()
}
