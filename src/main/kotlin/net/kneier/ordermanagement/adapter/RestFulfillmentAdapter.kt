package net.kneier.ordermanagement.adapter

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*

class RestFulfillmentAdapter(
    private val url: String,
    engine: HttpClientEngine,
) : FulfillmentAdapter {

    private val client = HttpClient(engine) {
        expectSuccess = false
    }

    override suspend fun orderFulfillment(orderId: String, items: Map<String, Int>) {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
        }

        if (response.status != HttpStatusCode.OK) error("Fulfillment order for order with id '$orderId' failed")
    }

}