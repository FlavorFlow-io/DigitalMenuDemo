package io.flavorflow.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.flavorflow.demo.di.ServiceLocator
import io.flavorflow.demo.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CheckoutViewModel(
    private val cart: CartRepository,
) : ViewModel() {

    private val paymentMethods = listOf(
        PaymentMethod(id = "pix", label = "Pix", detail = "Instant payment"),
        PaymentMethod(id = "credit", label = "Credit card", detail = "Visa •••• 4242"),
        PaymentMethod(id = "cash", label = "Cash", detail = "Pay on delivery"),
    )

    private val selectedPaymentId = MutableStateFlow(paymentMethods.first().id)
    private val orderPlaced = MutableStateFlow(false)

    val uiState: StateFlow<CheckoutUiState> =
        combine(cart.items, selectedPaymentId, orderPlaced) { items, paymentId, placed ->
            CheckoutUiState(
                items = items,
                paymentMethods = paymentMethods,
                selectedPaymentId = paymentId,
                orderPlaced = placed,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CheckoutUiState(paymentMethods = paymentMethods, selectedPaymentId = paymentMethods.first().id),
        )

    fun increment(productId: String) = cart.increment(productId)

    fun decrement(productId: String) = cart.decrement(productId)

    fun remove(productId: String) = cart.remove(productId)

    fun selectPayment(paymentId: String) {
        selectedPaymentId.value = paymentId
    }

    fun placeOrder() {
        if (cart.items.value.isEmpty()) return
        // A real app would submit the order to a backend here.
        orderPlaced.value = true
        cart.clear()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CheckoutViewModel(ServiceLocator.cartRepository)
            }
        }
    }
}
