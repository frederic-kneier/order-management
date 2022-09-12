@file:OptIn(ExperimentalCoroutinesApi::class)

package net.kneier.ordermanagement.adapter

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.kneier.ordermanagement.domain.Order
import net.kneier.ordermanagement.util.Docker
import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import uuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoOrderStorageAdapterTest {

    private val storageAdapter = MongoOrderStorageAdapter(Docker.Mongo.connectionString)
    private val mongo = KMongo.createClient(Docker.Mongo.connectionString).coroutine
    private val database = mongo.getDatabase("order-management")
    private val orders = database.getCollection<Document>("orders")

    @Test
    fun save() = runTest {
        val order = Order(
            id = uuid(),
            items = listOf(
                Order.Item(productId = uuid(), amount = 3),
            ),
            state = Order.State.Closed,
            paymentState = Order.PaymentState.Paid(
                timestamp = System.currentTimeMillis(),
            ),
            fulfillmentState = Order.FulfillmentState.Fulfilled(
                timestamp = System.currentTimeMillis(),
            ),
        )

        storageAdapter.save(order)

        val document = orders.findOneById(order.id)

        document shouldNotBe null
        document?.get("_id", String::class.java) shouldBe order.id
        document?.get("state", String::class.java) shouldBe "closed"
        document?.get("items", List::class.java).also { items ->
            items shouldNotBe null
            items?.size shouldBe order.items.size
            order.items.forEachIndexed { index, it ->
                (items?.get(index) as Document?).also { item ->
                    item shouldNotBe null
                    item?.get("productId") shouldBe it.productId
                    item?.get("amount") shouldBe it.amount
                }
            }
        }
        document?.get("paymentState", Document::class.java).also { paymentState ->
            paymentState shouldNotBe null
            paymentState?.get("type") shouldBe "paid"
            paymentState?.get("timestamp") should beInstanceOf<Number>()
        }
        document?.get("fulfillmentState", Document::class.java).also { fulfillmentState ->
            fulfillmentState shouldNotBe null
            fulfillmentState?.get("type") shouldBe "fulfilled"
            fulfillmentState?.get("timestamp") should beInstanceOf<Number>()
        }

    }

    @Test
    fun findById() = runTest {
        val orderId = uuid()
        val productId = uuid()
        val amount = 3
        val timestamp = System.currentTimeMillis()
        orders.save(
            Document().apply {
                put("_id", orderId)
                put("state", "closed")
                put("items", listOf(
                    Document().apply {
                        put("productId", productId)
                        put("amount", amount)
                    }
                ))
                put("paymentState", Document().apply {
                    put("type", "paid")
                    put("timestamp", timestamp)
                })
                put("fulfillmentState", Document().apply {
                    put("type", "fulfilled")
                    put("timestamp", timestamp)
                })
            }
        )

        val order = storageAdapter.findById(orderId)

        order shouldBe Order(
            id = orderId,
            items = listOf(
                Order.Item(productId = productId, amount = amount)
            ),
            state = Order.State.Closed,
            paymentState = Order.PaymentState.Paid(
                timestamp = timestamp,
            ),
            fulfillmentState = Order.FulfillmentState.Fulfilled(
                timestamp = timestamp,
            ),
        )

    }

}