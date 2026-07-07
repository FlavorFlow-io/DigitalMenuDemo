package io.flavorflow.demo.di

import io.flavorflow.demo.BuildConfig
import io.flavorflow.demo.data.repository.FakeMenuRepository
import io.flavorflow.demo.data.repository.FirestoreMenuRepository
import io.flavorflow.demo.data.repository.InMemoryCartRepository
import io.flavorflow.demo.domain.repository.CartRepository
import io.flavorflow.demo.domain.repository.MenuRepository
import io.flavorflow.demo.domain.usecase.GetMenuUseCase

/**
 * Minimal manual dependency container. Holds the app-scoped singletons so the
 * menu and checkout ViewModels share one [CartRepository]. In a larger app this
 * would be replaced by a DI framework (Hilt/Koin).
 */
object ServiceLocator {
    // Menu comes from Firestore, keyed by this build's applicationId tenant, so
    // every white-label package reads its own restaurant. The bundled fake repo
    // is the offline/error fallback that keeps the showcase from ever going blank.
    val menuRepository: MenuRepository by lazy {
        FirestoreMenuRepository(
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            tenantId = BuildConfig.APPLICATION_ID,
            apiKey = BuildConfig.FIRESTORE_API_KEY,
            fallback = FakeMenuRepository(),
        )
    }
    val cartRepository: CartRepository by lazy { InMemoryCartRepository() }
    val getMenuUseCase: GetMenuUseCase by lazy { GetMenuUseCase(menuRepository) }
}
