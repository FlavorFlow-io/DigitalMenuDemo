package io.flavorflow.demo.data.repository

import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.domain.repository.MenuRepository
import kotlinx.coroutines.delay

/**
 * In-memory [MenuRepository] standing in for a real backend. The artificial
 * delay simulates network latency so the UI loading state can be exercised.
 */
class FakeMenuRepository : MenuRepository {

    private val categories = listOf(
        Category(id = "burgers", name = "Burgers"),
        Category(id = "pizzas", name = "Pizzas"),
        Category(id = "salads", name = "Salads"),
        Category(id = "drinks", name = "Drinks"),
        Category(id = "desserts", name = "Desserts"),
    )

    private val products = buildList {
        addAll(
            category = "burgers",
            names = listOf(
                "Classic Cheeseburger" to "Beef patty, cheddar, lettuce, tomato",
                "Bacon Deluxe" to "Double bacon, smoked cheese, BBQ sauce",
                "Veggie Burger" to "Grilled mushroom, avocado, vegan mayo",
                "Spicy Chicken" to "Crispy chicken, jalapeños, chipotle sauce",
            ),
        )
        addAll(
            category = "pizzas",
            names = listOf(
                "Margherita" to "Tomato, mozzarella, fresh basil",
                "Pepperoni" to "Pepperoni, mozzarella, oregano",
                "Four Cheese" to "Mozzarella, gorgonzola, parmesan, brie",
                "Veggie Supreme" to "Peppers, onions, olives, mushrooms",
            ),
        )
        addAll(
            category = "salads",
            names = listOf(
                "Caesar Salad" to "Romaine, croutons, parmesan, Caesar dressing",
                "Greek Salad" to "Cucumber, tomato, feta, olives",
                "Quinoa Bowl" to "Quinoa, chickpeas, avocado, lemon dressing",
            ),
        )
        addAll(
            category = "drinks",
            names = listOf(
                "Fresh Lemonade" to "Squeezed lemons, mint, sparkling water",
                "Iced Coffee" to "Cold brew, milk, ice",
                "Orange Juice" to "100% freshly squeezed oranges",
                "Craft Soda" to "Artisanal cola with cane sugar",
            ),
        )
        addAll(
            category = "desserts",
            names = listOf(
                "Chocolate Brownie" to "Warm brownie, vanilla ice cream",
                "Cheesecake" to "New York style, berry compote",
                "Tiramisu" to "Espresso-soaked ladyfingers, mascarpone",
            ),
        )
    }

    override suspend fun getCategories(): List<Category> {
        delay(SIMULATED_LATENCY_MS)
        return categories
    }

    override suspend fun getProducts(): List<Product> {
        delay(SIMULATED_LATENCY_MS)
        return products
    }

    private fun MutableList<Product>.addAll(
        category: String,
        names: List<Pair<String, String>>,
    ) {
        names.forEachIndexed { index, (name, description) ->
            val id = "$category-$index"
            add(
                Product(
                    id = id,
                    name = name,
                    description = description,
                    imageUrl = foodImageUrl(name),
                    price = 9.0 + index * 3 + category.length,
                    categoryId = category,
                ),
            )
        }
    }

    /**
     * Builds a keyword-based food image URL from the product name. The name
     * words become the search keywords and a stable `lock` seed (derived from
     * the name) keeps the same photo across reloads.
     */
    private fun foodImageUrl(name: String): String {
        val keywords = name.trim().lowercase().replace(Regex("\\s+"), ",")
        val lock = name.hashCode().toLong() and 0xFFFFFFFFL
        return "https://loremflickr.com/400/300/$keywords,food?lock=$lock"
    }

    private companion object {
        const val SIMULATED_LATENCY_MS = 600L
    }
}
