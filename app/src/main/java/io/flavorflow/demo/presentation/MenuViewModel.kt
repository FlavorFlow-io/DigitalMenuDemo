package io.flavorflow.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.flavorflow.demo.di.ServiceLocator
import io.flavorflow.demo.domain.model.MenuSection
import io.flavorflow.demo.domain.repository.CartRepository
import io.flavorflow.demo.domain.usecase.GetMenuUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuViewModel(
    private val getMenu: GetMenuUseCase,
    private val cart: CartRepository,
) : ViewModel() {

    /** Loading-related state; the cart part of the UI state comes from [cart]. */
    private data class LoadState(
        val isLoading: Boolean = true,
        val sections: List<MenuSection> = emptyList(),
        val error: String? = null,
    )

    private val loadState = MutableStateFlow(LoadState())

    val uiState: StateFlow<MenuUiState> =
        combine(loadState, cart.items) { load, items ->
            MenuUiState(
                isLoading = load.isLoading,
                sections = load.sections,
                error = load.error,
                cart = items.associate { it.product.id to it.quantity },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    init {
        loadMenu()
    }

    fun loadMenu() {
        loadState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { getMenu() }
                .onSuccess { sections ->
                    loadState.update { it.copy(isLoading = false, sections = sections) }
                }
                .onFailure { throwable ->
                    loadState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Unknown error")
                    }
                }
        }
    }

    fun addToCart(productId: String) {
        val product = loadState.value.sections
            .firstNotNullOfOrNull { section -> section.products.firstOrNull { it.id == productId } }
            ?: return
        cart.add(product)
    }

    fun increment(productId: String) = cart.increment(productId)

    fun decrement(productId: String) = cart.decrement(productId)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MenuViewModel(ServiceLocator.getMenuUseCase, ServiceLocator.cartRepository)
            }
        }
    }
}
