import io.ktor.server.application.Application
import no.nav.tsm.plugins.Environment
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.inject


fun Application.configureDatabases() {
    val environment by inject<Environment>()
    Flyway.configure()
        .dataSource(environment.migratorJdbcUrl, environment.migratorDbUser, environment.migratorDbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()
}
