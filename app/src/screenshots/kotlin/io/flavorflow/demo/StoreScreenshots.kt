package io.flavorflow.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.lucianosantos.storescreenshots.FormFactor
import dev.lucianosantos.storescreenshots.ScreenshotStyle
import dev.lucianosantos.storescreenshots.StoreScreenshotsTest
import io.flavorflow.demo.domain.model.CartItem
import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.MenuSection
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.presentation.CheckoutUiState
import io.flavorflow.demo.presentation.MenuUiState
import io.flavorflow.demo.presentation.PaymentMethod
import io.flavorflow.demo.presentation.ui.CheckoutScreen
import io.flavorflow.demo.presentation.ui.MenuScreen
import io.flavorflow.demo.ui.theme.DigitalMenuTheme
import org.junit.Test

/**
 * The Play listing's phone screenshots, rendered from the app's own screens.
 *
 * Both screens take their state as a parameter, so nothing here needs a
 * ViewModel, Firestore, or a device — and because the images come from the real
 * composables under the real theme, a client's branding lands in the listing
 * without anyone redrawing anything.
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
     * The listing's languages. Each entry renders the whole set again, with the banner copy, the
     * app's own chrome and the sample menu all resolved for that locale.
     */
    private val locales = listOf("en-US", "pt-BR")

    @Test
    fun menu() = screenshot(
        locales = locales,
        fileName = "01_menu",
        titleRes = R.string.screenshot_menu_title,
        descriptionRes = R.string.screenshot_menu_desc,
    ) {
        DigitalMenuTheme { MenuScreen(uiState = sampleMenu()) }
    }

    @Test
    fun cart() = screenshot(
        locales = locales,
        fileName = "02_cart",
        titleRes = R.string.screenshot_cart_title,
        descriptionRes = R.string.screenshot_cart_desc,
    ) {
        // Two items already in the cart: an empty basket makes a poor screenshot.
        DigitalMenuTheme { MenuScreen(uiState = sampleMenu(cart = mapOf("p1" to 2, "p3" to 1))) }
    }

    @Test
    fun checkout() = screenshot(
        locales = locales,
        fileName = "03_checkout",
        titleRes = R.string.screenshot_checkout_title,
        descriptionRes = R.string.screenshot_checkout_desc,
    ) {
        DigitalMenuTheme { CheckoutScreen(uiState = sampleCheckout()) }
    }
}

@Composable
private fun sampleMenu(cart: Map<String, Int> = emptyMap()) = MenuUiState(
    isLoading = false,
    cart = cart,
    sections = listOf(
        MenuSection(
            category = Category(id = "c1", name = stringResource(R.string.sample_category_starters)),
            products = listOf(
                Product("p1", "Pão de alho", stringResource(R.string.sample_garlic_bread_desc), "", 12.0, "c1"),
                Product("p2", "Coxinha", stringResource(R.string.sample_coxinha_desc), "", 9.5, "c1"),
            ),
        ),
        MenuSection(
            category = Category(id = "c2", name = stringResource(R.string.sample_category_mains)),
            products = listOf(
                Product("p3", "Feijoada", stringResource(R.string.sample_feijoada_desc), "", 46.0, "c2"),
                Product("p4", "Moqueca", stringResource(R.string.sample_moqueca_desc), "", 52.0, "c2"),
            ),
        ),
    ),
)

@Composable
private fun sampleCheckout() = CheckoutUiState(
    items = listOf(
        CartItem(
            product = Product("p1", "Pão de alho", stringResource(R.string.sample_garlic_bread_desc), "", 12.0, "c1"),
            quantity = 2,
        ),
        CartItem(
            product = Product("p3", "Feijoada", stringResource(R.string.sample_feijoada_desc), "", 46.0, "c2"),
            quantity = 1,
        ),
    ),
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
