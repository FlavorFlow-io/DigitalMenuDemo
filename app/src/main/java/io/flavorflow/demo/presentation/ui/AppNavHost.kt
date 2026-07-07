package io.flavorflow.demo.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.flavorflow.demo.presentation.CheckoutViewModel
import io.flavorflow.demo.presentation.MenuViewModel

private object Routes {
    const val MENU = "menu"
    const val CHECKOUT = "checkout"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            val viewModel: MenuViewModel = viewModel(factory = MenuViewModel.Factory)
            val uiState by viewModel.uiState.collectAsState()

            MenuScreen(
                uiState = uiState,
                onAddToCart = viewModel::addToCart,
                onIncrement = viewModel::increment,
                onDecrement = viewModel::decrement,
                onCartClick = { navController.navigate(Routes.CHECKOUT) },
                onRetry = viewModel::loadMenu,
            )
        }

        composable(Routes.CHECKOUT) {
            val viewModel: CheckoutViewModel = viewModel(factory = CheckoutViewModel.Factory)
            val uiState by viewModel.uiState.collectAsState()

            CheckoutScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onIncrement = viewModel::increment,
                onDecrement = viewModel::decrement,
                onRemove = viewModel::remove,
                onSelectPayment = viewModel::selectPayment,
                onPlaceOrder = viewModel::placeOrder,
                onDone = {
                    navController.popBackStack(Routes.MENU, inclusive = false)
                },
            )
        }
    }
}
