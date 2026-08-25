package io.flavorflow.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lucianosantos.storescreenshots.DeviceMockup
import dev.lucianosantos.storescreenshots.FormFactor
import dev.lucianosantos.storescreenshots.StoreScreenshotsTest
import io.flavorflow.demo.presentation.ui.MenuScreen
import io.flavorflow.demo.ui.theme.DigitalMenuTheme
import org.junit.Test

/**
 * The 1024x500 banner at the top of the listing.
 *
 * Composed by hand rather than framed automatically — a feature graphic is
 * promotional art, so the form factor offers no title/description frame. The
 * colours come from the app's own theme, so a rebranded build produces a
 * rebranded banner with no extra work.
 */
class FeatureGraphic : StoreScreenshotsTest(FormFactor.GooglePlayFeatureGraphic) {

    @Test
    fun banner() = customScreenshot(fileName = "feature_graphic") {
        DigitalMenuTheme {
            val scheme = MaterialTheme.colorScheme
            Row(
                Modifier.fillMaxSize().background(scheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f).padding(start = 48.dp, end = 20.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = scheme.onPrimaryContainer,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.screenshot_feature_title),
                        color = scheme.onPrimaryContainer,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.screenshot_feature_desc),
                        color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
                // A real screen in the frame, so the banner shows the actual app.
                Box(Modifier.fillMaxHeight().padding(end = 40.dp), Alignment.Center) {
                    DeviceMockup(formFactor = FormFactor.Phone, modifier = Modifier.fillMaxHeight()) {
                        MenuScreen(uiState = featureMenuState())
                    }
                }
            }
        }
    }
}

/** A populated menu, so the framed device shows content rather than a spinner. */
private fun featureMenuState() = io.flavorflow.demo.presentation.MenuUiState(
    isLoading = false,
    sections = listOf(
        io.flavorflow.demo.domain.model.MenuSection(
            category = io.flavorflow.demo.domain.model.Category(id = "c1", name = "Starters"),
            products = listOf(
                io.flavorflow.demo.domain.model.Product(
                    "p1", "Pão de alho", "Grilled garlic bread", "", 12.0, "c1",
                ),
                io.flavorflow.demo.domain.model.Product(
                    "p2", "Coxinha", "Shredded chicken, crisp crumb", "", 9.5, "c1",
                ),
            ),
        ),
        io.flavorflow.demo.domain.model.MenuSection(
            category = io.flavorflow.demo.domain.model.Category(id = "c2", name = "Mains"),
            products = listOf(
                io.flavorflow.demo.domain.model.Product(
                    "p3", "Feijoada", "Black beans, pork, farofa", "", 46.0, "c2",
                ),
            ),
        ),
    ),
)
