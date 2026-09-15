package io.flavorflow.demo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.MenuSection
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.presentation.MenuUiState
import io.flavorflow.demo.presentation.ui.MENU_BANNER_TAG
import io.flavorflow.demo.presentation.ui.MenuScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The banner gives its height to the menu as it scrolls, and takes it back at the
 * top — the behaviour a diner feels but no screenshot can show.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MenuScreenBannerTest {

    // The classic rule rather than the v2 one, to match the dispatcher the
    // store-screenshot capture runs under.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createComposeRule()

    /** 0 once the banner is fully closed: it stops composing rather than laying out an empty box. */
    private fun bannerHeight(): Int =
        compose.onAllNodesWithTag(MENU_BANNER_TAG).fetchSemanticsNodes()
            .firstOrNull()?.size?.height ?: 0

    // Explicit start and end: swipeUp() defaults to the node's bottom edge but
    // swipeDown() defaults to its *top* — which on the root is the app bar, so the
    // grid never sees the gesture. Both stay inside the grid this way.
    private fun swipeMenuUp() = compose.onRoot().performTouchInput {
        swipeUp(startY = bottom * 0.9f, endY = bottom * 0.3f)
    }

    private fun swipeMenuDown() = compose.onRoot().performTouchInput {
        swipeDown(startY = bottom * 0.3f, endY = bottom * 0.9f)
    }

    private fun showMenu() {
        compose.setContent { MenuScreen(uiState = uiState) }
        compose.waitForIdle()
    }

    @Test
    fun `banner starts expanded`() {
        showMenu()
        assertTrue("banner should open at full height", bannerHeight() > 0)
    }

    @Test
    fun `scrolling the menu up collapses the banner`() {
        showMenu()
        val expanded = bannerHeight()

        swipeMenuUp()
        compose.waitForIdle()

        assertTrue(
            "banner should shrink on scroll (was $expanded, still ${bannerHeight()})",
            bannerHeight() < expanded,
        )
    }

    @Test
    fun `scrolling back to the top restores the banner`() {
        showMenu()
        val expanded = bannerHeight()

        swipeMenuUp()
        compose.waitForIdle()

        // The grid takes what it still needs to reach its start and the banner
        // gets only the leftover, so how many drags this takes depends on how far
        // the first flick threw the list.
        repeat(MAX_SWIPES_BACK) {
            if (bannerHeight() == expanded) return
            swipeMenuDown()
            compose.waitForIdle()
        }

        assertEquals("banner should be fully back at the top", expanded, bannerHeight())
    }

    private companion object {
        /** Generous: a fast flick can throw the grid several screens down. */
        const val MAX_SWIPES_BACK = 12
    }

    private val uiState = MenuUiState(
        isLoading = false,
        bannerImageUrl = null,
        sections = List(4) { section ->
            MenuSection(
                category = Category("c$section", "Category $section"),
                // Enough cells that the grid has somewhere to scroll to.
                products = List(6) { index ->
                    Product("p$section-$index", "Dish $index", "", "", 10.0, "c$section")
                },
            )
        },
    )
}
