package io.flavorflow.demo

import io.flavorflow.demo.data.menu.MenuJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * How a dish reads for a given diner: the menu carries its own languages, and
 * the app resolves rather than requires them — a client who wrote one language
 * must keep working, and a diner whose language is missing must still be able
 * to order.
 *
 * Robolectric, not a plain JVM test: [MenuJson] reads with `org.json`, which is
 * only a stub in the Android JAR the unit tests compile against.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MenuJsonLocalizationTest {

    private val localized = """
        {
          "defaultLanguage": "pt-BR",
          "categories": [
            { "id": "c1", "name": { "pt-BR": "Entradas", "en": "Starters" } }
          ],
          "products": [
            {
              "id": "p1", "categoryId": "c1", "price": 12.0,
              "name": { "pt-BR": "Pão de alho", "en": "Garlic bread" },
              "description": { "pt-BR": "Manteiga da casa", "en": "House butter" }
            }
          ]
        }
    """.trimIndent()

    private fun productName(json: String, locale: Locale) =
        MenuJson.parse(json, locale).products.single().name

    @Test
    fun `exact language tag wins`() {
        assertEquals("Pão de alho", productName(localized, Locale.forLanguageTag("pt-BR")))
        assertEquals("Garlic bread", productName(localized, Locale.forLanguageTag("en")))
    }

    @Test
    fun `a region variant reads its language`() {
        // pt-PT is not in the document; Portuguese is, so a Lisbon diner reads it.
        assertEquals("Pão de alho", productName(localized, Locale.forLanguageTag("pt-PT")))
        // en-GB against a plain "en" entry.
        assertEquals("Garlic bread", productName(localized, Locale.forLanguageTag("en-GB")))
    }

    @Test
    fun `an unknown language falls back to the menu's own`() {
        assertEquals("Pão de alho", productName(localized, Locale.forLanguageTag("ja-JP")))
    }

    @Test
    fun `categories and descriptions localize too`() {
        val menu = MenuJson.parse(localized, Locale.forLanguageTag("en"))
        assertEquals("Starters", menu.categories.single().name)
        assertEquals("House butter", menu.products.single().description)
    }

    @Test
    fun `a single-language menu still parses`() {
        val plain = """
            {
              "categories": [{ "id": "c1", "name": "Burgers" }],
              "products": [{ "id": "p1", "categoryId": "c1", "name": "Cheeseburger",
                             "description": "Cheddar, lettuce", "price": 16.0 }]
            }
        """.trimIndent()
        assertEquals("Cheeseburger", productName(plain, Locale.forLanguageTag("pt-BR")))
        assertEquals("Burgers", MenuJson.parse(plain, Locale.FRANCE).categories.single().name)
    }

    @Test
    fun `a missing description is empty rather than fatal`() {
        val noDescription = """
            {
              "categories": [{ "id": "c1", "name": "Sides" }],
              "products": [{ "id": "p1", "categoryId": "c1", "name": "Couve", "price": 14.0 }]
            }
        """.trimIndent()
        assertEquals("", MenuJson.parse(noDescription, Locale.US).products.single().description)
    }

    @Test
    fun `a product with no name is a malformed menu`() {
        val noName = """
            {
              "categories": [{ "id": "c1", "name": "Sides" }],
              "products": [{ "id": "p1", "categoryId": "c1", "price": 14.0 }]
            }
        """.trimIndent()
        assertThrows(MenuJson.MalformedMenuException::class.java) {
            MenuJson.parse(noName, Locale.US)
        }
    }
}
