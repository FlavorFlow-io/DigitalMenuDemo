package io.flavorflow.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.lucianosantos.storescreenshots.FormFactor
import dev.lucianosantos.storescreenshots.ScreenshotStyle
import dev.lucianosantos.storescreenshots.StoreScreenshotsTest
import io.flavorflow.demo.domain.model.CartItem
import io.flavorflow.demo.presentation.CheckoutUiState
import io.flavorflow.demo.presentation.MenuUiState
import io.flavorflow.demo.presentation.PaymentMethod
import io.flavorflow.demo.presentation.ui.CheckoutScreen
import io.flavorflow.demo.presentation.ui.MenuScreen
import io.flavorflow.demo.ui.theme.DigitalMenuTheme
import org.junit.Before
import org.junit.Test

/**
 * The Play listing's phone screenshots, rendered from the app's own screens.
 *
 * Both screens take their state as a parameter, so nothing here needs a
 * ViewModel or a device — and because the images come from the real composables
 * under the real theme, over the menu this build was branded with, a client's
 * branding *and* its dishes land in the listing without anyone redrawing
 * anything.
 *
 * File names are numbered: Play orders screenshots by name.
 */
class StoreScreenshots : StoreScreenshotsTest(
    formFactor = FormFactor.Phone,
    // Rendered without the app's own window insets, so the frame reserves the status bar rather
    // than letting the top app bar sit under the clock and the camera cutout.
    style = ScreenshotStyle(edgeToEdge = false),
) {

    /**
     * The listing's languages. Each entry renders the whole set again, with the app's own chrome
     * and the dishes themselves resolved for that locale — a menu that carries translations gets
     * a listing in each, and one written in a single language reads the same in both.
     */
    private val locales = listOf("en-US", "pt-BR")

    @Before
    fun useSynchronousImageLoader() = BundledMenu.installSynchronousImageLoader()

    @Test
    fun menu() = screenshot(
        locales = locales,
        fileName = "01_menu",
        titleRes = R.string.screenshot_menu_title,
        descriptionRes = R.string.screenshot_menu_desc,
    ) {
        DigitalMenuTheme { MenuScreen(uiState = bundledMenu()) }
    }

    @Test
    fun cart() = screenshot(
        locales = locales,
        fileName = "02_cart",
        titleRes = R.string.screenshot_cart_title,
        descriptionRes = R.string.screenshot_cart_desc,
    ) {
        // Items already in the cart: an empty basket makes a poor screenshot.
        DigitalMenuTheme { MenuScreen(uiState = bundledMenu(cart = BundledMenu.sampleCart())) }
    }

    @Test
    fun checkout() = screenshot(
        locales = locales,
        fileName = "03_checkout",
        titleRes = R.string.screenshot_checkout_title,
        descriptionRes = R.string.screenshot_checkout_desc,
    ) {
        DigitalMenuTheme { CheckoutScreen(uiState = bundledCheckout()) }
    }
}

private fun bundledMenu(cart: Map<String, Int> = emptyMap()) = MenuUiState(
    isLoading = false,
    cart = cart,
    bannerImageUrl = BundledMenu.content.bannerImageUrl,
    sections = BundledMenu.sections,
)

/** The same cart as `02_cart`, one screen further along. */
@Composable
private fun bundledCheckout(): CheckoutUiState {
    val cart = BundledMenu.sampleCart()
    val products = BundledMenu.sections.flatMap { it.products }.associateBy { it.id }
    return CheckoutUiState(
        items = cart.mapNotNull { (id, quantity) ->
            products[id]?.let { CartItem(product = it, quantity = quantity) }
        },
        paymentMethods = listOf(
            PaymentMethod("pix", "Pix", stringResource(R.string.sample_payment_pix_desc)),
            PaymentMethod(
                "card",
                stringResource(R.string.sample_payment_card),
                stringResource(R.string.sample_payment_card_desc),
            ),
        ),
        selectedPaymentId = "pix",
    )
}
