package io.flavorflow.demo.domain.model

/**
 * A category together with the products that belong to it. The presentation
 * layer renders one section after another to build the continuous grid.
 */
data class MenuSection(
    val category: Category,
    val products: List<Product>,
)
