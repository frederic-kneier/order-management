package net.kneier.ordermanagement.adapter

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kneier.ordermanagement.domain.Order

class InMemoryOrderStorageAdapter : OrderStorageAdapter {

    private val mutex = Mutex()
    private val orders = hashMapOf<String, Order>()

    override suspend fun save(order: Order) = order.also {
        mutex.withLock {
            orders[order.id] = it
        }
    }

    override suspend fun findById(id: String): Order? = mutex.withLock {
        orders[id]
    }

}