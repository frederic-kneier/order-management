@file:OptIn(ExperimentalCoroutinesApi::class)

package net.kneier.ordermanagement.adapter

import io.kotest.assertions.throwables.shouldThrow
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import uuid

class RestFulfillmentAdapterTest {

    @Test
    fun `succeeds on HTTP 200 result`() = runTest {
        val url = "fulfillment"
        val engine = MockEngine.create {
            addHandler {
                respondOk()
            }
        }

        val adapter = RestFulfillmentAdapter(url, engine)

        adapter.orderFulfillment(uuid(), emptyMap())
    }


    @Test
    fun `fails on HTTP error result`() = runTest {
        val url = "fulfillment"
        val engine = MockEngine.create {
            addHandler {
                respondBadRequest()
            }
        }

        val adapter = RestFulfillmentAdapter(url, engine)

        shouldThrow<Exception> {
            adapter.orderFulfillment(uuid(), emptyMap())
        }
    }

}