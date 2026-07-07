package io.flavorflow.demo.data.repository

import io.flavorflow.demo.data.remote.FirestoreClient
import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.domain.repository.MenuRepository

/**
 * [MenuRepository] backed by Cloud Firestore over its REST API.
 *
 * Menus are multi-tenant: every white-label build reads its own restaurant under
 * `restaurants/{tenantId}`, where [tenantId] is the build's applicationId. So one
 * Firebase project serves all clients, and adding a client is just seeding a new
 * document — no code, no per-client config file.
 *
 * On any network/backend error (or an empty tenant) it falls back to [fallback]
 * so the showcase always renders a menu instead of an error screen.
 */
class FirestoreMenuRepository(
    projectId: String,
    private val tenantId: String,
    apiKey: String = "",
    private val fallback: MenuRepository? = null,
) : MenuRepository {

    private val client = FirestoreClient(projectId, apiKey)

    override suspend fun getCategories(): List<Category> = try {
        client.listDocuments("restaurants/$tenantId/categories")
            .sortedBy { it.int("sortOrder") }
            .map { Category(id = it.id, name = it.string("name")) }
            .ifEmpty { fallback?.getCategories() ?: emptyList() }
    } catch (e: Exception) {
        fallback?.getCategories() ?: throw e
    }

    override suspend fun getProducts(): List<Product> = try {
        client.listDocuments("restaurants/$tenantId/products")
            .map {
                Product(
                    id = it.id,
                    name = it.string("name"),
                    description = it.string("description"),
                    imageUrl = it.string("imageUrl"),
                    price = it.double("price"),
                    categoryId = it.string("categoryId"),
                )
            }
            .ifEmpty { fallback?.getProducts() ?: emptyList() }
    } catch (e: Exception) {
        fallback?.getProducts() ?: throw e
    }
}
