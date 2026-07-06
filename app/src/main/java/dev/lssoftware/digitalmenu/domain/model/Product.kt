package dev.lssoftware.digitalmenu.domain.model

/**
 * A single product offered on the menu, belonging to one [Category].
 */
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    /** Price in the menu's currency units (e.g. BRL). */
    val price: Double,
    val categoryId: String,
)
