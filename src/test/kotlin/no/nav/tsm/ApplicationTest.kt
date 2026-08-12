package no.nav.tsm

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "dbUser" to "dev",
                "dbPassword" to "dev",
                "dbHost" to "dev",
                "dbPort" to "dev",
                "dbName" to "dev"
            )
        }
        application {
            routing {
                get("/") {
                    call.respondText("Hello World!")
                }
            }
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }
}


