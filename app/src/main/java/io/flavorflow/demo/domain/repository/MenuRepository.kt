package io.flavorflow.demo.domain.repository

import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product

/**
 * Abstraction over the menu data source. The domain layer depends only on this
 * interface; concrete implementations (fake backend, remote API, …) live in the
 * data layer.
 */
interface MenuRepository {
    /** The restaurant's cover photo, or null when it has none. */
    suspend fun getBannerImageUrl(): String?
    suspend fun getCategories(): List<Category>
    suspend fun getProducts(): List<Product>
}
