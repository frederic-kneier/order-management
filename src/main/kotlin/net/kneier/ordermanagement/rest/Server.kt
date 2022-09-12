package net.kneier.ordermanagement.rest

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object Server {
    fun Application.registerSerialization() {
        install(ContentNegotiation) {
            jackson()
        }
    }

    fun Routing.registerFulfillmentMock(path: String, generator: () -> Boolean) {
        post(path) {
            when (generator()) {
                true -> call.respond(HttpStatusCode.OK, "success")
                else -> call.respond(HttpStatusCode.InternalServerError, "failure")
            }
        }
    }
}