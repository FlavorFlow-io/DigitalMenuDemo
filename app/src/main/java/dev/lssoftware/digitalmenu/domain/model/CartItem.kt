package dev.lssoftware.digitalmenu.domain.model

/**
 * A product in the cart together with its chosen quantity. Storing the whole
 * [Product] keeps the cart self-contained, so the checkout screen can render it
 * without needing the menu to be loaded.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val lineTotal: Double get() = product.price * quantity
}
