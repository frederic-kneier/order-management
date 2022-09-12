package net.kneier.ordermanagement.adapter

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class InMemoryLockAdapter : LockAdapter {

    private val mutex = Mutex()
    private val locks = hashSetOf<String>()

    override suspend fun <T> withLock(target: Class<*>, id: String, block: suspend () -> T): T {
        return withTimeout(1000) {
            try {
                while (!addLock(target, id)) {
                    delay(100)
                }

                block()
            } finally {
                removeLock(target, id)
            }
        }
    }

    suspend fun addLock(target: Class<*>, id: String) = mutex.withLock {
        locks.add(generateKey(target, id))
    }

    suspend fun removeLock(target: Class<*>, id: String) = mutex.withLock {
        locks.remove(generateKey(target, id))
    }

    private fun generateKey(target: Class<*>, id: String) = "${target.name}/$id"


}