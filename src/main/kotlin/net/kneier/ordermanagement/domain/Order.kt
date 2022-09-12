package net.kneier.ordermanagement.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import org.bson.codecs.pojo.annotations.BsonId

data class Order(
    @BsonId
    val id: String,
    val items: List<Item> = emptyList(),
    val state: State,
    val paymentState: PaymentState? = null,
    val fulfillmentState: FulfillmentState? = null,
) {

    enum class State(@JsonValue val value: String) {
        Created("created"),
        Paid("paid"),
        InFulfillment("in_fulfillment"),
        Closed("closed"),
    }

    data class Item(
        val productId: String,
        val amount: Int,
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed class PaymentState {

        @JsonTypeName("paid")
        data class Paid(
            val timestamp: Long,
        ) : PaymentState()

    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed class FulfillmentState {

        @JsonTypeName("started")
        data class Started(
            val timestamp: Long,
        ) : FulfillmentState()

        @JsonTypeName("fulfilled")
        data class Fulfilled(
            val timestamp: Long,
        ) : FulfillmentState()

        @JsonTypeName("failed")
        data class Failed(
            val timestamp: Long,
            val reason: String?,
        ) : FulfillmentState()

    }

}
