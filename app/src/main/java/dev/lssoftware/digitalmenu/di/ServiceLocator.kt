package dev.lssoftware.digitalmenu.di

import dev.lssoftware.digitalmenu.data.repository.FakeMenuRepository
import dev.lssoftware.digitalmenu.data.repository.InMemoryCartRepository
import dev.lssoftware.digitalmenu.domain.repository.CartRepository
import dev.lssoftware.digitalmenu.domain.repository.MenuRepository
import dev.lssoftware.digitalmenu.domain.usecase.GetMenuUseCase

/**
 * Minimal manual dependency container. Holds the app-scoped singletons so the
 * menu and checkout ViewModels share one [CartRepository]. In a larger app this
 * would be replaced by a DI framework (Hilt/Koin).
 */
object ServiceLocator {
    val menuRepository: MenuRepository by lazy { FakeMenuRepository() }
    val cartRepository: CartRepository by lazy { InMemoryCartRepository() }
    val getMenuUseCase: GetMenuUseCase by lazy { GetMenuUseCase(menuRepository) }
}
