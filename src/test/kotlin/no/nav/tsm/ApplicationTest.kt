package no.nav.tsm

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.*
import no.nav.tsm.plugins.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                    "dbUser" to "dev",
                    "dbPassword" to "dev",
                    "dbHost" to "dev",
                    "dbPort" to "dev",
                    "dbName" to "dev")
        }
        application {
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }
}
