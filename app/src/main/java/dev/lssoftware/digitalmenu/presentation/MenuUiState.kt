package dev.lssoftware.digitalmenu.presentation

import dev.lssoftware.digitalmenu.domain.model.MenuSection

/**
 * Immutable UI state for the menu screen, exposed by [MenuViewModel].
 *
 * @param cart maps a product id to the quantity currently in the cart.
 */
data class MenuUiState(
    val isLoading: Boolean = true,
    val sections: List<MenuSection> = emptyList(),
    val error: String? = null,
    val cart: Map<String, Int> = emptyMap(),
) {
    /** Total number of items across the cart. */
    val cartItemCount: Int get() = cart.values.sum()

    /** Sum of price × quantity for every product in the cart. */
    val cartTotal: Double
        get() {
            if (cart.isEmpty()) return 0.0
            val priceById = sections.asSequence()
                .flatMap { it.products.asSequence() }
                .associate { it.id to it.price }
            return cart.entries.sumOf { (id, qty) -> (priceById[id] ?: 0.0) * qty }
        }

    /** Quantity of a single product currently in the cart. */
    fun quantityOf(productId: String): Int = cart[productId] ?: 0
}
