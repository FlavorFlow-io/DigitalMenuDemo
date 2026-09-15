package io.flavorflow.demo.di

import android.content.Context
import io.flavorflow.demo.data.repository.BundledMenuRepository
import io.flavorflow.demo.data.repository.InMemoryCartRepository
import io.flavorflow.demo.domain.repository.CartRepository
import io.flavorflow.demo.domain.repository.MenuRepository
import io.flavorflow.demo.domain.usecase.GetMenuUseCase

/**
 * Minimal manual dependency container. Holds the app-scoped singletons so the
 * menu and checkout ViewModels share one [CartRepository]. In a larger app this
 * would be replaced by a DI framework (Hilt/Koin).
 *
 * The menu comes from the JSON this build was branded with, so the only thing
 * that needs a [Context] is reading the app's own assets — passed in by the
 * ViewModel factory rather than held here, so nothing outlives the process.
 */
object ServiceLocator {

    private var menuRepository: MenuRepository? = null

    val cartRepository: CartRepository by lazy { InMemoryCartRepository() }

    fun menuRepository(context: Context): MenuRepository =
        menuRepository ?: BundledMenuRepository(context.applicationContext.assets)
            .also { menuRepository = it }

    fun getMenuUseCase(context: Context): GetMenuUseCase = GetMenuUseCase(menuRepository(context))
}
