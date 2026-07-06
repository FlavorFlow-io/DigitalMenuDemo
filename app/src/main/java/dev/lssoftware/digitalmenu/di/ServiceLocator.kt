package dev.lssoftware.digitalmenu.di

import dev.lssoftware.digitalmenu.BuildConfig
import dev.lssoftware.digitalmenu.data.repository.FakeMenuRepository
import dev.lssoftware.digitalmenu.data.repository.FirestoreMenuRepository
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
