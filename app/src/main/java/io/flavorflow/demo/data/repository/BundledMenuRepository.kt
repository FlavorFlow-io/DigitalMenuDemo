package io.flavorflow.demo.data.repository

import android.content.res.AssetManager
import io.flavorflow.demo.data.menu.Menu
import io.flavorflow.demo.data.menu.MenuJson
import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.domain.repository.MenuRepository

/**
 * [MenuRepository] over the menu bundled into the app at build time.
 *
 * Each white-label client uploads its own menu to FlavorFlow as the `menu_json`
 * asset variable; CI packages that document, and the photos it references, into
 * `assets/`. So the menu needs no backend and no network: it is as available as
 * the app itself, on first frame and in airplane mode alike.
 *
 * Changing a menu means rebuilding — which is the trade this sample makes on
 * purpose, since the same bundled file is what lets the Play listing screenshots
 * render the client's real dishes.
 */
class BundledMenuRepository(private val assets: AssetManager) : MenuRepository {

    private val menu: Menu by lazy {
        MenuJson.parse(assets.open(MENU_ASSET).bufferedReader().use { it.readText() })
    }

    override suspend fun getBannerImageUrl(): String? = menu.bannerImageUrl

    override suspend fun getCategories(): List<Category> = menu.categories

    override suspend fun getProducts(): List<Product> = menu.products

    companion object {
        /** Written by the `prepareMenuAssets` Gradle task. */
        const val MENU_ASSET = "menu.json"
    }
}
