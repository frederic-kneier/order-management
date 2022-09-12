package net.kneier.ordermanagement.adapter

import net.kneier.ordermanagement.domain.Order

interface OrderStorageAdapter {

    suspend fun save(order: Order): Order

    suspend fun findById(id: String): Order?

}