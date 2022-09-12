package net.kneier.ordermanagement.rest

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import net.kneier.ordermanagement.domain.Order
import net.kneier.ordermanagement.domain.OrderService

object Orders {
    fun Routing.registerOrderManagement(orderService: OrderService) {

        get("orders/{id}") {
            val id: String by call.parameters

            process {
                orderService.findOrderById(id)
            }
        }

        post("orders/{id}") {
            val id: String by call.parameters
            val body: BodyCreate = call.receive()

            process {
                orderService.createOrder(id, body.items)
            }
        }

        post("orders/{id}/paymentResolution") {
            val id: String by call.parameters

            process {
                orderService.resolvePayment(id)
            }
        }

        post("orders/{id}/fulfillmentStart") {
            val id: String by call.parameters
            process {
                orderService.startFulfillment(id)
            }
        }

        post("orders/{id}/fulfillmentResolution") {
            val id: String by call.parameters
            process {
                orderService.resolveFulfillment(id)
            }
        }

    }

    private suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.process(block: () -> T?) {
        try {
            when (val result = block()) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(HttpStatusCode.OK, result)
            }
        } catch (ex: OrderService.OrderException) {
            call.respond(HttpStatusCode.BadRequest, BodyError(ex.message))
        }
    }

    data class BodyCreate(
        val items: List<Order.Item>,
    )

    data class BodyError(
        val message: String?,
    )
}