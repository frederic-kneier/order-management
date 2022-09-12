package net.kneier.ordermanagement.adapter

interface LockAdapter {

    suspend fun <T> withLock(target: Class<*>, id: String, block: suspend () -> T): T

}