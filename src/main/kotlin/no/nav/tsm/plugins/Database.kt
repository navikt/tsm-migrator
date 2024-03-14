import io.ktor.server.application.Application
import no.nav.tsm.plugins.Environment
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.inject


fun Application.configureDatabases() {
    val environment by inject<Environment>()
    Flyway.configure()
        .dataSource(environment.jdbcUrl, environment.dbUser, environment.dbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()

    Database.connect(
        url = environment.jdbcUrl,
        user = environment.dbUser,
        password = environment.dbPassword,
        driver = "org.postgresql.Driver",
    )
}
