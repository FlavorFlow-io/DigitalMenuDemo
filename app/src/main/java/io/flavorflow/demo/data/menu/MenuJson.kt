package io.flavorflow.demo.data.menu

import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Reads the menu document that FlavorFlow's `menu_json` asset variable carries
 * and the `prepareMenuAssets` Gradle task packages as `assets/menu.json`:
 *
 * ```json
 * {
 *   "defaultLanguage": "pt-BR",
 *   "banner": "file:///android_asset/menu/1c04….jpg",
 *   "categories": [
 *     { "id": "starters", "name": { "pt-BR": "Entradas", "en": "Starters" } }
 *   ],
 *   "products": [
 *     { "id": "p1", "categoryId": "starters",
 *       "name": { "pt-BR": "Pão de alho", "en": "Garlic bread" },
 *       "description": { "pt-BR": "Manteiga da casa", "en": "House butter" },
 *       "price": 12.0,
 *       "imageUrl": "file:///android_asset/menu/9f3a….jpg" }
 *   ]
 * }
 * ```
 *
 * `name` and `description` are either a plain string — one language, as every
 * menu was written before this — or an object keyed by language tag. Both shapes
 * parse, so a client that has only ever uploaded one language keeps working.
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
     * Parses [json] into the menu's categories and products, in document order,
     * with every translated field resolved for [locale].
     *
     * @throws MalformedMenuException if the document is not readable as a menu.
     */
    fun parse(json: String, locale: Locale = Locale.getDefault()): Menu {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw MalformedMenuException("menu.json is not valid JSON", e)
        }

        return try {
            val categories = root.getJSONArray("categories")
            val products = root.getJSONArray("products")
            // The language to fall back to when a dish has no translation for
            // the reader's: the one the menu was written in.
            val fallbackLanguage = root.optString("defaultLanguage").takeIf { it.isNotEmpty() }
            Menu(
                // Optional: a client with no cover photo gets the branded
                // fallback banner rather than a hole at the top of the menu.
                bannerImageUrl = root.optString("banner").takeIf { it.isNotEmpty() },
                categories = (0 until categories.length()).map { index ->
                    val category = categories.getJSONObject(index)
                    Category(
                        id = category.getString("id"),
                        name = category.required("name").localized(locale, fallbackLanguage),
                    )
                },
                products = (0 until products.length()).map { index ->
                    val product = products.getJSONObject(index)
                    Product(
                        id = product.getString("id"),
                        name = product.required("name").localized(locale, fallbackLanguage),
                        description = product.opt("description").localized(locale, fallbackLanguage),
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

    /** The value at [key], or a [JSONException] naming it — `name` is not optional. */
    private fun JSONObject.required(key: String): Any {
        val value = opt(key)
        if (value == null || value == JSONObject.NULL) throw JSONException("No value for $key")
        return value
    }

    /**
     * This field as [locale] should read it: the value itself when the menu is
     * written in one language, or the closest translation it carries.
     */
    private fun Any?.localized(locale: Locale, fallbackLanguage: String?): String = when {
        this == null || this == JSONObject.NULL -> ""
        this is JSONObject -> pick(locale, fallbackLanguage)
        else -> toString()
    }

    /**
     * Picks a translation for [locale], preferring an exact tag (`pt-BR`), then
     * any variant of the same language (`pt`, `pt-PT`), then the menu's own
     * default language, and finally whatever it does carry — a diner reading a
     * dish in the wrong language is a worse outcome than no dish at all only in
     * theory; an empty menu is the one that looks broken.
     */
    private fun JSONObject.pick(locale: Locale, fallbackLanguage: String?): String {
        val tags = keys().asSequence().toList()
        val wanted = locale.toLanguageTag()
        val language = locale.language

        fun matching(predicate: (String) -> Boolean): String? =
            tags.firstOrNull(predicate)?.let { optString(it).takeIf { value -> value.isNotEmpty() } }

        return matching { it.equals(wanted, ignoreCase = true) }
            ?: matching { it.equals(language, ignoreCase = true) }
            ?: matching { it.substringBefore('-').equals(language, ignoreCase = true) }
            ?: fallbackLanguage?.let { fallback -> matching { it.equals(fallback, ignoreCase = true) } }
            ?: matching { true }
            ?: ""
    }
}

/** The whole menu as the bundled document describes it, before it is grouped. */
data class Menu(
    val bannerImageUrl: String?,
    val categories: List<Category>,
    val products: List<Product>,
)
