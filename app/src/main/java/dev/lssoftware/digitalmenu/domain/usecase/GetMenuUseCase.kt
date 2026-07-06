package dev.lssoftware.digitalmenu.domain.usecase

import dev.lssoftware.digitalmenu.domain.model.MenuSection
import dev.lssoftware.digitalmenu.domain.repository.MenuRepository

/**
 * Loads the menu and groups products into [MenuSection]s, preserving category
 * order and dropping categories that have no products.
 */
class GetMenuUseCase(
    private val repository: MenuRepository,
) {
    suspend operator fun invoke(): List<MenuSection> {
        val categories = repository.getCategories()
        val productsByCategory = repository.getProducts().groupBy { it.categoryId }

        return categories.mapNotNull { category ->
            val products = productsByCategory[category.id].orEmpty()
            if (products.isEmpty()) null else MenuSection(category, products)
        }
    }
}
