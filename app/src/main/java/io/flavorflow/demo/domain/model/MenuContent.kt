package io.flavorflow.demo.domain.model

/**
 * A whole menu ready to render: the restaurant's banner, if it has one, and its
 * categories with the products under each.
 */
data class MenuContent(
    val bannerImageUrl: String?,
    val sections: List<MenuSection>,
)
