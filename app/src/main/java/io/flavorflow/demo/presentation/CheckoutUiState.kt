package io.flavorflow.demo.presentation

import io.flavorflow.demo.domain.model.CartItem

/** A selectable payment option shown on the checkout screen. */
data class PaymentMethod(
    val id: String,
    val label: String,
    val detail: String,
)

/**
 * Immutable UI state for the checkout screen, exposed by [CheckoutViewModel].
 */
data class CheckoutUiState(
    val items: List<CartItem> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentId: String? = null,
    val orderPlaced: Boolean = false,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val isEmpty: Boolean get() = items.isEmpty()

    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val deliveryFee: Double get() = if (isEmpty) 0.0 else DELIVERY_FEE
    val total: Double get() = subtotal + deliveryFee

    companion object {
        const val DELIVERY_FEE = 6.90
    }
}
