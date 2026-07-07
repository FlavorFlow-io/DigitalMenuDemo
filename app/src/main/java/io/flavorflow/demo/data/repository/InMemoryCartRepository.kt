package io.flavorflow.demo.data.repository

import io.flavorflow.demo.domain.model.CartItem
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-lifetime, in-memory [CartRepository]. A real app would persist this
 * (DataStore/Room) and/or sync it with a backend.
 */
class InMemoryCartRepository : CartRepository {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    override val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    override fun add(product: Product) = _items.update { current ->
        val existing = current.firstOrNull { it.product.id == product.id }
        if (existing == null) {
            current + CartItem(product, quantity = 1)
        } else {
            current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    override fun increment(productId: String) = changeQuantity(productId, +1)

    override fun decrement(productId: String) = changeQuantity(productId, -1)

    override fun remove(productId: String) = _items.update { current ->
        current.filterNot { it.product.id == productId }
    }

    override fun clear() {
        _items.value = emptyList()
    }

    private fun changeQuantity(productId: String, delta: Int) = _items.update { current ->
        current.mapNotNull { item ->
            if (item.product.id != productId) {
                item
            } else {
                val next = item.quantity + delta
                if (next <= 0) null else item.copy(quantity = next)
            }
        }
    }
}
