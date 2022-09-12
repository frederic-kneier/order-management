package net.kneier.ordermanagement.adapter

import net.kneier.ordermanagement.domain.Order
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoOrderStorageAdapter(
    connectionString: String,
) : OrderStorageAdapter {

    private val client = KMongo.createClient(connectionString).coroutine
    private val database = client.getDatabase("order-management")
    private val collection = database.getCollection<Order>("orders")

    override suspend fun save(order: Order) = order.also {
        collection.save(it)
    }

    override suspend fun findById(id: String): Order? = collection.findOneById(id)

}