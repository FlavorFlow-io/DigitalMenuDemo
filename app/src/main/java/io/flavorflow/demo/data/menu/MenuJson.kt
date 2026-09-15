package io.flavorflow.demo.data.menu

import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads the menu document that FlavorFlow's `menu_json` asset variable carries
 * and the `prepareMenuAssets` Gradle task packages as `assets/menu.json`:
 *
 * ```json
 * {
 *   "banner": "file:///android_asset/menu/1c04….jpg",
 *   "categories": [ { "id": "starters", "name": "Starters" } ],
 *   "products": [
 *     { "id": "p1", "categoryId": "starters", "name": "Garlic bread",
 *       "description": "House butter", "price": 12.0,
 *       "imageUrl": "file:///android_asset/menu/9f3a….jpg" }
 *   ]
 * }
 * ```
 *
 * Categories render in array order — there is no `sortOrder` field, because a
 * hand-written menu already sits in the order its author wants.
 *
 * `org.json` rather than a serialization library: it is part of Android, and
 * Robolectric ships a real implementation, so the store-screenshot capture reads
 * the very same file the app does.
 */
object MenuJson {

    /** Thrown for a document this app cannot render; the message names the flaw. */
    class MalformedMenuException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)

    /**
     * Parses [json] into the menu's categories and products, in document order.
     *
     * @throws MalformedMenuException if the document is not readable as a menu.
     */
    fun parse(json: String): Menu {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw MalformedMenuException("menu.json is not valid JSON", e)
        }

        return try {
            val categories = root.getJSONArray("categories")
            val products = root.getJSONArray("products")
            Menu(
                // Optional: a client with no cover photo gets the branded
                // fallback banner rather than a hole at the top of the menu.
                bannerImageUrl = root.optString("banner").takeIf { it.isNotEmpty() },
                categories = (0 until categories.length()).map { index ->
                    val category = categories.getJSONObject(index)
                    Category(id = category.getString("id"), name = category.getString("name"))
                },
                products = (0 until products.length()).map { index ->
                    val product = products.getJSONObject(index)
                    Product(
                        id = product.getString("id"),
                        name = product.getString("name"),
                        description = product.optString("description"),
                        imageUrl = product.optString("imageUrl"),
                        price = product.getDouble("price"),
                        categoryId = product.getString("categoryId"),
                    )
                },
            )
        } catch (e: JSONException) {
            throw MalformedMenuException("menu.json is missing a field this app needs: ${e.message}", e)
        }
    }
}

/** The whole menu as the bundled document describes it, before it is grouped. */
data class Menu(
    val bannerImageUrl: String?,
    val categories: List<Category>,
    val products: List<Product>,
)
