package net.kneier.ordermanagement.domain

import kotlinx.coroutines.delay
import net.kneier.ordermanagement.adapter.FulfillmentAdapter
import net.kneier.ordermanagement.adapter.LockAdapter
import net.kneier.ordermanagement.adapter.OrderStorageAdapter
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

class OrderService(
    private val orderStorageAdapter: OrderStorageAdapter,
    private val lockAdapter: LockAdapter,
    private val fulfillmentAdapter: FulfillmentAdapter,
) {

    companion object {
        const val fulfillmentAttempts = 3
    }

    suspend fun findOrderById(id: String) = orderStorageAdapter.findById(id)

    suspend fun createOrder(
        id: String,
        items: List<Order.Item>,
    ): Order = lockAdapter.withLock(Order::class.java, id) {

        val order = orderStorageAdapter.findById(id)

        when (order?.state) {
            Order.State.Created -> order
            null -> Order(
                id = id,
                items = items,
                state = Order.State.Created,
            ).also {
                validate(it) {
                    validate(Order::items).isNotEmpty()
                }

                orderStorageAdapter.save(it)
            }

            else -> throw UnsupportedStateException(order.id, order.state)
        }
    }

    suspend fun resolvePayment(
        id: String,
    ): Order? = lockAdapter.withLock(Order::class.java, id) {
        val order = orderStorageAdapter.findById(id) ?: return@withLock null

        when (order.state) {
            Order.State.Paid -> order
            Order.State.Created -> order.copy(
                state = Order.State.Paid,
                paymentState = Order.PaymentState.Paid(timestamp = System.currentTimeMillis()),
            ).also {
                orderStorageAdapter.save(it)
            }

            else -> throw UnsupportedStateException(order.id, order.state)
        }
    }

    suspend fun startFulfillment(
        id: String,
    ): Order? = lockAdapter.withLock(Order::class.java, id) {
        val order = orderStorageAdapter.findById(id) ?: return@withLock null

        when (order.state) {
            Order.State.InFulfillment -> order
            Order.State.Paid -> run {
                var attempt = 0

                while (true) {
                    try {
                        fulfillmentAdapter.orderFulfillment(
                            orderId = order.id,
                            items = order.items.associate { it.productId to it.amount },
                        )
                        break
                    } catch (ex: Exception) {
                        when (++attempt < fulfillmentAttempts) {
                            true -> delay(100)
                            else -> {
                                orderStorageAdapter.save(
                                    order.copy(
                                        fulfillmentState = Order.FulfillmentState.Failed(
                                            timestamp = System.currentTimeMillis(),
                                            reason = ex.message,
                                        )
                                    )
                                )
                                throw FulfillmentBeginException(order.id, attempt)
                            }
                        }
                    }
                }

                order.copy(
                    state = Order.State.InFulfillment,
                    fulfillmentState = Order.FulfillmentState.Started(
                        timestamp = System.currentTimeMillis(),
                    )
                ).also {
                    orderStorageAdapter.save(it)
                }
            }

            else -> throw UnsupportedStateException(order.id, order.state)
        }

    }

    suspend fun resolveFulfillment(
        id: String,
    ): Order? = lockAdapter.withLock(Order::class.java, id) {

        val order = orderStorageAdapter.findById(id) ?: return@withLock null

        when (order.state) {
            Order.State.Closed -> order
            Order.State.InFulfillment -> order.copy(
                state = Order.State.Closed,
                fulfillmentState = Order.FulfillmentState.Fulfilled(
                    timestamp = System.currentTimeMillis(),
                ),
            ).also {
                orderStorageAdapter.save(it)
            }

            else -> throw UnsupportedStateException(order.id, order.state)
        }
    }

    abstract class OrderException(message: String) : RuntimeException(message)

    class UnsupportedStateException(id: String, state: Order.State) : OrderException(
        "Order with id '$id' has an unsupported state of '$state'"
    )

    class FulfillmentBeginException(id: String, attempts: Int) : OrderException(
        "Could not begin fulfillment for order with id '$id' after $attempts attempts"
    )
}