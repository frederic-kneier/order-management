package net.kneier.ordermanagement.adapter

interface FulfillmentAdapter {
    suspend fun orderFulfillment(orderId: String, items: Map<String, Int>)
}