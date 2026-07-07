package io.flavorflow.demo.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.flavorflow.demo.R
import io.flavorflow.demo.domain.model.Category
import io.flavorflow.demo.domain.model.MenuSection
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.presentation.MenuUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    uiState: MenuUiState,
    onAddToCart: (String) -> Unit = {},
    onIncrement: (String) -> Unit = {},
    onDecrement: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // The white-label app name; FlavorFlow rewrites app_name per client.
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        floatingActionButton = {
            CartFab(
                itemCount = uiState.cartItemCount,
                total = uiState.cartTotal,
                onClick = onCartClick,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = uiState.error)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                            Text(text = "Retry")
                        }
                    }
                }

                else -> MenuContent(
                    sections = uiState.sections,
                    cart = uiState.cart,
                    onAddToCart = onAddToCart,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                )
            }
        }
    }
}

/**
 * A single flattened entry in the grid: either a full-width category header or a
 * product cell. Flattening the sections lets one [LazyVerticalGrid] render every
 * category continuously while we still know the grid index of each header.
 */
private sealed interface MenuGridItem {
    data class Header(val category: Category) : MenuGridItem
    data class ProductCell(val product: Product) : MenuGridItem
}

@Composable
private fun MenuContent(
    sections: List<MenuSection>,
    cart: Map<String, Int>,
    onAddToCart: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val gridItems = remember(sections) {
        buildList {
            sections.forEach { section ->
                add(MenuGridItem.Header(section.category))
                section.products.forEach { add(MenuGridItem.ProductCell(it)) }
            }
        }
    }

    // Grid index of each section's header, in section order. Used both to scroll
    // to a section (tab click) and to resolve the active section (scrolling).
    val headerIndices = remember(gridItems) {
        gridItems.mapIndexedNotNull { index, item ->
            if (item is MenuGridItem.Header) index else null
        }
    }

    // The active tab follows the top-most visible item: the last header at or
    // above the first visible index wins, so reaching a group's start selects it.
    val selectedTabIndex by remember(headerIndices) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            headerIndices.indexOfLast { it <= firstVisible }.coerceAtLeast(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            sections.forEachIndexed { index, section ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = {
                        scope.launch {
                            gridState.animateScrollToItem(headerIndices[index])
                        }
                    },
                    text = { Text(text = section.category.name) },
                )
            }
        }

        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.weight(1f),
            columns = GridCells.Fixed(2),
            // Extra bottom padding so the cart FAB never covers the last row.
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems.forEach { item ->
                when (item) {
                    is MenuGridItem.Header -> item(
                        key = "header-${item.category.id}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        CategoryHeader(name = item.category.name)
                    }

                    is MenuGridItem.ProductCell -> item(key = "product-${item.product.id}") {
                        ProductCard(
                            product = item.product,
                            quantity = cart[item.product.id] ?: 0,
                            onAdd = { onAddToCart(item.product.id) },
                            onIncrement = { onIncrement(item.product.id) },
                            onDecrement = { onDecrement(item.product.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ProductCard(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .build(),
                placeholder = ColorPainter(Color.LightGray),
                error = ColorPainter(Color.LightGray),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPrice(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    QuantityControl(
                        quantity = quantity,
                        onAdd = onAdd,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                    )
                }
            }
        }
    }
}

/**
 * Shows a circular add button when the product is not in the cart, and a
 * −/quantity/+ stepper once it is.
 */
@Composable
private fun QuantityControl(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    if (quantity == 0) {
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add to cart",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.primary, shape = shape),
        ) {
            StepperButton(symbol = "–", onClick = onDecrement)
            Text(
                text = quantity.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton(symbol = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CartFab(
    itemCount: Int,
    total: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = itemCount > 0,
        enter = slideInVertically { it * 2 } + fadeIn(),
        exit = slideOutVertically { it * 2 } + fadeOut(),
        modifier = modifier,
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            val label = if (itemCount == 1) "1 item" else "$itemCount items"
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatPrice(total),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    val sections = listOf(
        MenuSection(
            category = Category("burgers", "Burgers"),
            products = listOf(
                Product("b1", "Cheeseburger", "Beef, cheddar, lettuce", "", 25.0, "burgers"),
                Product("b2", "Bacon Deluxe", "Double bacon, BBQ", "", 29.0, "burgers"),
            ),
        ),
        MenuSection(
            category = Category("drinks", "Drinks"),
            products = listOf(
                Product("d1", "Lemonade", "Fresh lemons, mint", "", 12.0, "drinks"),
            ),
        ),
    )
    MenuScreen(
        uiState = MenuUiState(
            isLoading = false,
            sections = sections,
            cart = mapOf("b1" to 2),
        ),
    )
}
