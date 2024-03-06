import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.module
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.createEnvironment
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(),
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

