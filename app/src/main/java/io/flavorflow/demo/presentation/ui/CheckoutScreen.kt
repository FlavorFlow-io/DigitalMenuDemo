package io.flavorflow.demo.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.flavorflow.demo.domain.model.CartItem
import io.flavorflow.demo.domain.model.Product
import io.flavorflow.demo.presentation.CheckoutUiState
import io.flavorflow.demo.presentation.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    onBack: () -> Unit = {},
    onIncrement: (String) -> Unit = {},
    onDecrement: (String) -> Unit = {},
    onRemove: (String) -> Unit = {},
    onSelectPayment: (String) -> Unit = {},
    onPlaceOrder: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.orderPlaced) "Order confirmed" else "Checkout") },
                navigationIcon = {
                    IconButton(onClick = if (uiState.orderPlaced) onDone else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.orderPlaced && !uiState.isEmpty) {
                CheckoutBottomBar(uiState = uiState, onPlaceOrder = onPlaceOrder)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.orderPlaced -> OrderSuccess(onDone = onDone)
                uiState.isEmpty -> EmptyCart(onBack = onBack)
                else -> CheckoutContent(
                    uiState = uiState,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    onRemove = onRemove,
                    onSelectPayment = onSelectPayment,
                )
            }
        }
    }
}

@Composable
private fun CheckoutContent(
    uiState: CheckoutUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSelectPayment: (String) -> Unit,
) {
    var itemPendingRemoval by remember { mutableStateOf<CartItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("Your order") }

        items(uiState.items, key = { it.product.id }) { item ->
            CartItemRow(
                item = item,
                onIncrement = { onIncrement(item.product.id) },
                onDecrement = { onDecrement(item.product.id) },
                onRemove = { itemPendingRemoval = item },
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SectionTitle("Payment method")
        }

        items(uiState.paymentMethods, key = { it.id }) { method ->
            PaymentOption(
                method = method,
                selected = method.id == uiState.selectedPaymentId,
                onClick = { onSelectPayment(method.id) },
            )
        }
    }

    itemPendingRemoval?.let { pending ->
        RemoveItemDialog(
            item = pending,
            onConfirm = {
                onRemove(pending.product.id)
                itemPendingRemoval = null
            },
            onDismiss = { itemPendingRemoval = null },
        )
    }
}

@Composable
private fun RemoveItemDialog(
    item: CartItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove item?") },
        text = { Text("Remove \"${item.product.name}\" from your order?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.product.imageUrl)
                .crossfade(true)
                .build(),
            placeholder = ColorPainter(Color.LightGray),
            error = ColorPainter(Color.LightGray),
            contentDescription = item.product.name,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatPrice(item.lineTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        MiniStepper(
            quantity = item.quantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
        ),
    ) {
        StepButton(symbol = "–", onClick = onDecrement)
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp),
        )
        StepButton(symbol = "+", onClick = onIncrement)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentOption(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = method.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun CheckoutBottomBar(
    uiState: CheckoutUiState,
    onPlaceOrder: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow("Subtotal", formatPrice(uiState.subtotal))
            Spacer(Modifier.height(4.dp))
            SummaryRow("Delivery", formatPrice(uiState.deliveryFee))
            Spacer(Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            SummaryRow(
                label = "Total",
                value = formatPrice(uiState.total),
                emphasized = true,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = "Place order · ${formatPrice(uiState.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val style =
            if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        val weight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        Text(text = label, style = style, fontWeight = weight)
        Text(text = value, style = style, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OrderSuccess(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Order placed!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your food is being prepared and will be on its way soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Back to menu", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyCart(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your cart is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) {
            Text("Browse menu")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckoutScreenPreview() {
    val items = listOf(
        CartItem(Product("b1", "Cheeseburger", "Beef, cheddar", "", 25.0, "burgers"), 2),
        CartItem(Product("d1", "Lemonade", "Fresh lemons", "", 12.0, "drinks"), 1),
    )
    CheckoutScreen(
        uiState = CheckoutUiState(
            items = items,
            paymentMethods = listOf(
                PaymentMethod("pix", "Pix", "Instant payment"),
                PaymentMethod("credit", "Credit card", "Visa •••• 4242"),
            ),
            selectedPaymentId = "pix",
        ),
    )
}
