package io.flavorflow.demo

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import io.flavorflow.demo.data.repository.BundledMenuRepository
import io.flavorflow.demo.domain.model.MenuContent
import io.flavorflow.demo.domain.model.MenuSection
import io.flavorflow.demo.domain.usecase.GetMenuUseCase
import kotlinx.coroutines.runBlocking
import org.robolectric.RuntimeEnvironment
import java.util.Locale
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The menu this build was branded with, as the listing art renders it.
 *
 * The screenshots deliberately read the app's own bundled `menu.json` rather
 * than a hand-written fixture: that file is the client's real menu, and its
 * photos were downloaded into the app at build time by `prepareMenuAssets`. So
 * the listing shows the dishes the client actually sells, and it stays true
 * without anyone editing this file when a menu changes.
 */
object BundledMenu {

    // One parse per language, not per screenshot: the capture renders the whole
    // set again for each locale, and the dishes now translate with it.
    private val perLocale = mutableMapOf<String, MenuContent>()

    /** The locale the capture is currently rendering — its own, not the JVM's. */
    private fun locale(): Locale =
        RuntimeEnvironment.getApplication().resources.configuration.locales[0]

    /** The bundled menu as the locale being captured reads it. */
    val content: MenuContent
        get() {
            val current = locale()
            return perLocale.getOrPut(current.toLanguageTag()) {
                val assets = RuntimeEnvironment.getApplication().assets
                runBlocking { GetMenuUseCase(BundledMenuRepository(assets) { current })() }
            }
        }

    val sections: List<MenuSection> get() = content.sections

    /**
     * Makes Coil load images inline instead of on its own dispatchers.
     *
     * The capture is a Robolectric unit test: `waitForIdle()` pumps the main
     * looper and then the PNG is written, which an ordinary asynchronous fetch
     * loses the race to — that is what left every product photo grey. With
     * [EmptyCoroutineContext] the fetch and decode run in the caller's context,
     * so the bitmap is on screen before the capture happens.
     */
    // setUnsafe rather than setSafe: this runs per test class, and the loader
    // installed by a previous one must be replaced, not deferred to.
    @OptIn(DelicateCoilApi::class)
    fun installSynchronousImageLoader() {
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(RuntimeEnvironment.getApplication())
                .coroutineContext(EmptyCoroutineContext)
                .build()
        )
    }

    /**
     * A cart holding [quantity] of the first product in each of the first
     * [sectionCount] sections — an empty basket makes a poor screenshot, and
     * picking by position keeps this working for any client's menu.
     */
    fun sampleCart(sectionCount: Int = 2, quantity: Int = 2): Map<String, Int> =
        sections.take(sectionCount)
            .mapNotNull { it.products.firstOrNull() }
            .withIndex()
            .associate { (index, product) -> product.id to if (index == 0) quantity else 1 }
}
