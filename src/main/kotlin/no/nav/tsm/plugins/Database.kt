import io.ktor.server.application.Application
import no.nav.tsm.plugins.Environment
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.inject


fun Application.configureDatabases() {
    val environment by inject<Environment>()
    Flyway.configure()
        .dataSource(environment.jdbcUrl, environment.dbUser, environment.dbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()

}
