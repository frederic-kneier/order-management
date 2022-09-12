package net.kneier.ordermanagement.rest

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import net.kneier.ordermanagement.domain.Order
import net.kneier.ordermanagement.domain.OrderService
import net.kneier.ordermanagement.rest.Orders.registerOrderManagement
import net.kneier.ordermanagement.rest.Server.registerSerialization
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uuid

class OrdersTest {

    private val orderService = mockk<OrderService>()

    @Nested
    inner class Creation {

        @Test
        fun `returns HTTP 200 with order on success`() = execute { client ->
            val orderId = uuid()
            val productId = uuid()
            val amount = 3

            coEvery { orderService.createOrder(orderId, any()) } answers {
                Order(
                    id = arg(0),
                    items = arg(1),
                    state = Order.State.Created,
                )
            }

            val response = client.post("/orders/$orderId") {
                setBody(
                    mapOf(
                        "items" to listOf(
                            mapOf(
                                "productId" to productId,
                                "amount" to amount,
                            )
                        )
                    )
                )
            }

            response.status shouldBe HttpStatusCode.OK
            response.body<Order>().also { order ->
                order.id shouldBe orderId
                order.items shouldContainExactlyInAnyOrder listOf(
                    Order.Item(productId = productId, amount = amount)
                )
            }
        }

        @Test
        fun `returns HTTP 400 on unsupported order state`() = execute { client ->
            val orderId = uuid()

            coEvery { orderService.createOrder(orderId, any()) } throws OrderService.UnsupportedStateException(
                orderId, Order.State.Paid
            )

            val response = client.post("/orders/$orderId") {
                setBody(
                    mapOf(
                        "items" to listOf(
                            mapOf(
                                "productId" to uuid(),
                                "amount" to 1,
                            )
                        )
                    )
                )
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    }

    @Nested
    inner class Query {

        @Test
        fun `returns HTTP 200 with order if it exists`() = execute { client ->
            val order = Order(
                id = uuid(),
                items = emptyList(),
                state = Order.State.Created,
            )

            coEvery { orderService.findOrderById(order.id) } returns order

            val response = client.get("/orders/${order.id}")

            response.status shouldBe HttpStatusCode.OK
            response.body<Order>() shouldBe order

        }

        @Test
        fun `returns HTTP 404 if order does not exists`() = execute { client ->
            coEvery { orderService.findOrderById(any()) } returns null

            val response = client.get("/orders/${uuid()}")

            response.status shouldBe HttpStatusCode.NotFound
        }

    }

    @Nested
    inner class Payment {

        @Test
        fun `returns HTTP 200 on success`() = execute { client ->
            val order = Order(
                id = uuid(),
                items = emptyList(),
                state = Order.State.Paid,
            )

            coEvery { orderService.resolvePayment(order.id) } returns order

            val response = client.post("/orders/${order.id}/paymentResolution")

            response.status shouldBe HttpStatusCode.OK
            response.body<Order>() shouldBe order
        }

        @Test
        fun `returns HTTP 400 on unsupported order state`() = execute { client ->
            val orderId = uuid()

            coEvery { orderService.resolvePayment(orderId) } returns null

            val response = client.post("/orders/$orderId/paymentResolution")

            response.status shouldBe HttpStatusCode.NotFound
        }

    }

    @Nested
    inner class FulfillmentStart {

        @Test
        fun `returns HTTP 200 on successfull start`() = execute { client ->
            val order = Order(
                id = uuid(),
                items = emptyList(),
                state = Order.State.InFulfillment,
            )

            coEvery { orderService.startFulfillment(order.id) } returns order

            val response = client.post("/orders/${order.id}/fulfillmentStart")

            response.status shouldBe HttpStatusCode.OK
            response.body<Order>() shouldBe order
        }

        @Test
        fun `returns HTTP 404 if order does not exists`() = execute { client ->
            coEvery { orderService.startFulfillment(any()) } returns null

            val response = client.get("/orders/${uuid()}/fulfillmentStart")

            response.status shouldBe HttpStatusCode.NotFound
        }

    }

    @Nested
    inner class FulfillmentResolution {

        @Test
        fun `returns HTTP 200 on successfull resolution`() = execute { client ->
            val order = Order(
                id = uuid(),
                items = emptyList(),
                state = Order.State.Closed,
            )

            coEvery { orderService.resolveFulfillment(order.id) } returns order

            val response = client.post("/orders/${order.id}/fulfillmentResolution")

            response.status shouldBe HttpStatusCode.OK
            response.body<Order>() shouldBe order
        }

    }

    private fun execute(block: suspend (HttpClient) -> Unit) = testApplication {
        application {
            registerSerialization()
        }
        routing {
            registerOrderManagement(orderService)
        }
        block(
            createClient {
                expectSuccess = false
                install(DefaultRequest) {
                    contentType(ContentType.Application.Json)
                }
                install(ContentNegotiation) {
                    jackson()
                }
            }
        )
    }
}