package net.kneier.ordermanagement.rest

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import net.kneier.ordermanagement.rest.Server.registerFulfillmentMock
import net.kneier.ordermanagement.rest.Server.registerSerialization
import org.junit.jupiter.api.Test

class ServerTest {

    @Test
    fun `fulfillment endpoint succeeds`() = testApplication {
        application {
            registerSerialization()
        }
        routing {
            registerFulfillmentMock("fulfillment") { true }
        }

        createClient {
            expectSuccess = false
        }.apply {
            val response = post("/fulfillment") {
                contentType(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `fulfillment endpoint fails`() = testApplication {
        application {
            registerSerialization()
        }
        routing {
            registerFulfillmentMock("fulfillment") { false }
        }

        createClient {
            expectSuccess = false
        }.apply {
            val response = post("/fulfillment") {
                contentType(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
}