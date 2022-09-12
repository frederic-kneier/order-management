@file:JvmName("OrderManagement")

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import net.kneier.ordermanagement.adapter.InMemoryLockAdapter
import net.kneier.ordermanagement.adapter.MongoOrderStorageAdapter
import net.kneier.ordermanagement.adapter.RestFulfillmentAdapter
import net.kneier.ordermanagement.domain.OrderService
import net.kneier.ordermanagement.rest.Orders.registerOrderManagement
import net.kneier.ordermanagement.rest.Server.registerFulfillmentMock
import net.kneier.ordermanagement.rest.Server.registerSerialization
import kotlin.random.Random

fun main() {
    val fulfillmentMockPath = "mock/fulfillment"
    val orderService = OrderService(
        lockAdapter = InMemoryLockAdapter(),
        orderStorageAdapter = MongoOrderStorageAdapter(
            connectionString = "mongodb://localhost:27017"
        ),
        fulfillmentAdapter = RestFulfillmentAdapter(
            url = "http://localhost:8080/$fulfillmentMockPath",
            engine = io.ktor.client.engine.cio.CIO.create(),
        ),
    )

    embeddedServer(CIO, port = 8080) {
        registerSerialization()
        routing {
            registerOrderManagement(orderService)
            registerFulfillmentMock(fulfillmentMockPath, Random::nextBoolean)
        }
    }.apply {
        start(wait = true)
    }
}