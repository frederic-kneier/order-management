@file:OptIn(ExperimentalCoroutinesApi::class)

package net.kneier.ordermanagement.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import net.kneier.ordermanagement.adapter.FulfillmentAdapter
import net.kneier.ordermanagement.adapter.InMemoryLockAdapter
import net.kneier.ordermanagement.adapter.InMemoryOrderStorageAdapter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import uuid

class OrderServiceTest {

    private val orderStorageAdapter = InMemoryOrderStorageAdapter()
    private val fulfillmentAdapter = mockk<FulfillmentAdapter>()
    private val lockAdapter = InMemoryLockAdapter()
    private val orderService = OrderService(
        orderStorageAdapter = orderStorageAdapter,
        lockAdapter = lockAdapter,
        fulfillmentAdapter = fulfillmentAdapter,
    )

    @Nested
    inner class Creation {

        @Test
        fun `succeeds if it does not already exist`() = runTest {
            val orderId = uuid()
            val orderItems = listOf(
                Order.Item(productId = uuid(), amount = 1),
                Order.Item(productId = uuid(), amount = 3),
            )

            val result = orderService.createOrder(
                id = orderId,
                items = orderItems,
            )

            result.id shouldNotBe null
            result.state shouldBe Order.State.Created
        }

        @Test
        fun `succeeds if it already exists and has the right state`() = runTest {
            val orderId = uuid()
            val orderItems = listOf(
                Order.Item(productId = uuid(), amount = 1),
                Order.Item(productId = uuid(), amount = 3),
            )

            val existing = orderStorageAdapter.save(
                Order(
                    id = orderId,
                    items = orderItems,
                    state = Order.State.Created,
                )
            )

            val result = orderService.createOrder(
                id = orderId,
                items = orderItems,
            )


            result shouldBe existing
        }

        @Test
        fun `fails if it already exists and has the wrong state`() = runTest {
            val orderId = uuid()
            val orderItems = listOf(
                Order.Item(productId = uuid(), amount = 1),
                Order.Item(productId = uuid(), amount = 3),
            )

            orderStorageAdapter.save(
                Order(
                    id = orderId,
                    items = orderItems,
                    state = Order.State.Paid,
                )
            )

            shouldThrow<OrderService.UnsupportedStateException> {
                orderService.createOrder(
                    id = orderId,
                    items = orderItems,
                )
            }

        }

        @Test
        fun `fails on missing items`() = runTest {
            val orderId = uuid()
            val orderItems = emptyList<Order.Item>()

            shouldThrow<ConstraintViolationException> {
                orderService.createOrder(
                    id = orderId,
                    items = orderItems,
                )
            }
        }

        @Test
        fun `fails on lock timeout`() = runTest {
            val orderId = uuid()
            val orderItems = emptyList<Order.Item>()

            lockAdapter.addLock(Order::class.java, orderId)

            shouldThrow<TimeoutCancellationException> {
                orderService.createOrder(
                    id = orderId,
                    items = orderItems,
                )
            }
        }

    }

    @Nested
    inner class Query {

        @Test
        fun `succeeds returning order if it exist`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Created,
                )
            )

            val result = orderService.findOrderById(order.id)

            result shouldBe order
        }

        @Test
        fun `succeeds returning nothing if order does not exist`() = runTest {
            val result = orderService.findOrderById(uuid())

            result shouldBe null

        }

    }

    @Nested
    inner class PaymentConfirmation {

        @Test
        fun `succeeds on order in Created state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Created,
                )
            )

            val result = orderService.resolvePayment(order.id)

            result shouldNotBe null
            result?.state shouldBe Order.State.Paid
            result?.paymentState should beInstanceOf<Order.PaymentState.Paid>()
        }

        @Test
        fun `succeeds on order in Paid state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Paid,
                )
            )

            val result = orderService.resolvePayment(order.id)

            result shouldBe order
        }

        @Test
        fun `fails for unknown order`() = runTest {
            val orderId = uuid()

            val order = orderService.resolvePayment(orderId)

            order shouldBe null
        }

        @Test
        fun `fails on wrong state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.InFulfillment,
                )
            )

            shouldThrow<OrderService.UnsupportedStateException> {
                orderService.resolvePayment(order.id)
            }
        }

    }

    @Nested
    inner class FulfillmentBegin {

        @Test
        fun `succeeds on order in Paid state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Paid,
                )
            )

            coEvery { fulfillmentAdapter.orderFulfillment(order.id, any()) } just Runs

            val result = orderService.startFulfillment(order.id)

            result shouldNotBe null
            result?.state shouldBe Order.State.InFulfillment
        }

        @Test
        fun `succeeds on order in InFulfillment state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.InFulfillment,
                )
            )

            coEvery { fulfillmentAdapter.orderFulfillment(order.id, any()) } just Runs

            val result = orderService.startFulfillment(order.id)

            result shouldBe order
        }

        @Test
        fun `succeeds on order in Paid state with retry`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Paid,
                )
            )

            val success = listOf(false, false, true).iterator()

            coEvery { fulfillmentAdapter.orderFulfillment(order.id, any()) } answers {
                success.next() || error("Called failed")
            }

            val result = orderService.startFulfillment(order.id)

            coVerify(exactly = 3) { fulfillmentAdapter.orderFulfillment(order.id, any()) }
            confirmVerified(fulfillmentAdapter)

            result shouldNotBe null
            result?.state shouldBe Order.State.InFulfillment
            result?.fulfillmentState should beInstanceOf<Order.FulfillmentState.Started>()
        }

        @Test
        fun `returns null if order does not exist`() = runTest {
            val result = orderService.startFulfillment(uuid())

            result shouldBe null
        }

        @Test
        fun `fails on order in Paid state with too many errors`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Paid,
                )
            )

            val success = listOf(false, false, false).iterator()

            coEvery { fulfillmentAdapter.orderFulfillment(order.id, any()) } answers {
                success.next() || error("Called failed")
            }

            shouldThrow<OrderService.FulfillmentBeginException> {
                orderService.startFulfillment(order.id)
            }

            coVerify(exactly = 3) { fulfillmentAdapter.orderFulfillment(order.id, any()) }
            confirmVerified(fulfillmentAdapter)
        }

        @Test
        fun `fails on order in wrong state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Created,
                )
            )

            shouldThrow<OrderService.UnsupportedStateException> {
                orderService.startFulfillment(order.id)
            }

        }

    }

    @Nested
    inner class FulfillmentConfirmation {

        @Test
        fun `succeeds on order in InFulfillment state`() = runTest {
            val orderId = uuid()

            orderStorageAdapter.save(
                Order(
                    id = orderId,
                    items = emptyList(),
                    state = Order.State.InFulfillment,
                )
            )

            val result = orderService.resolveFulfillment(orderId)

            result shouldNotBe null
            result?.state shouldBe Order.State.Closed
            result?.fulfillmentState should beInstanceOf<Order.FulfillmentState.Fulfilled>()
        }

        @Test
        fun `succeeds on order in Closed state`() = runTest {

            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Closed,
                )
            )

            val result = orderService.resolveFulfillment(order.id)

            result shouldBe order
        }

        @Test
        fun `returns null if order does not exist`() = runTest {
            val result = orderService.resolveFulfillment(uuid())

            result shouldBe null
        }

        @Test
        fun `fails for unknown order`() = runTest {
            val orderId = uuid()

            val result = orderService.resolveFulfillment(orderId)

            result shouldBe null
        }

        @Test
        fun `fails on order in wrong state`() = runTest {
            val order = orderStorageAdapter.save(
                Order(
                    id = uuid(),
                    items = emptyList(),
                    state = Order.State.Created,
                )
            )

            shouldThrow<OrderService.UnsupportedStateException> {
                orderService.resolveFulfillment(order.id)
            }

        }

    }


}