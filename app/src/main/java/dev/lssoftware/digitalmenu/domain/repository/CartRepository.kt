package dev.lssoftware.digitalmenu.domain.repository

import dev.lssoftware.digitalmenu.domain.model.CartItem
import dev.lssoftware.digitalmenu.domain.model.Product
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the shopping cart, shared across screens. Exposes
 * the current items as an observable [StateFlow] so any collector (menu,
 * checkout, …) stays in sync.
 */
interface CartRepository {
    val items: StateFlow<List<CartItem>>

    /** Adds one unit of the product, or increments it if already present. */
    fun add(product: Product)
    fun increment(productId: String)

    /** Decrements the product, removing it once its quantity reaches zero. */
    fun decrement(productId: String)
    fun remove(productId: String)
    fun clear()
}
