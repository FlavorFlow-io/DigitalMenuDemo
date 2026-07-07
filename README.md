# Digital Menu — a FlavorFlow white-label showcase

A single Jetpack Compose food-ordering app that is **re-branded per client at build
time** with [FlavorFlow](https://flavor-flow.io), and serves each client's menu live
from a **shared Cloud Firestore** backend. One codebase → many branded apps, each
with its own name, colors, icon, and menu.

## What it demonstrates

| Concern | How |
| --- | --- |
| **White-labeling** | FlavorFlow's `apply-flavor-action` rewrites the Compose theme, `app_name`, `applicationId`, and launcher icon for each client in CI. See [`.github/workflows/build-white-label.yml`](.github/workflows/build-white-label.yml). |
| **Multi-tenant backend** | Menus live in Firestore under `restaurants/{applicationId}/…`. The app reads its own tenant via `BuildConfig.APPLICATION_ID`, so a single Firebase project serves every branded build. |
| **No `google-services.json`** | Firestore is read over its REST API (public read rules). That keeps one backend working across many different `applicationId`s — a plain Firebase SDK setup binds to fixed package names and would break white-labeling. |
| **Clean architecture / MVVM** | `domain` (models, repository interfaces, use cases) · `data` (Firestore + fallback repositories) · `presentation` (ViewModels, Compose UI). |
| **Resilience** | If Firestore is unreachable, the app falls back to a bundled in-memory menu so the showcase never shows a blank screen. |

## Architecture

```
presentation/  MenuViewModel, CheckoutViewModel, Compose screens
     │  (StateFlow)
domain/        Category, Product, MenuSection · MenuRepository, CartRepository · GetMenuUseCase
     │  (interfaces)
data/          FirestoreMenuRepository ──REST──▶ Cloud Firestore
               FakeMenuRepository (offline fallback)
               InMemoryCartRepository
```

## Firestore data model

```
restaurants/{applicationId}
  name: string
  categories/{categoryId}   { name, sortOrder }
  products/{productId}      { name, description, imageUrl, price, categoryId }
```

Rules ([`firestore.rules`](firestore.rules)) make menus public read-only; writes are
server-side (the seed script / Admin SDK).

## Getting started

**Run the base (un-branded) app** — reads the `io.flavorflow.demo` tenant:

```bash
./gradlew assembleDebug
```

**Set up the backend:**

```bash
firebase login
firebase deploy --only firestore:rules          # deploy security rules
cd firebase/seed && npm install && npm run seed  # seed demo menus (see its README)
```

**Produce white-label builds:** create a project on [flavor-flow.io](https://flavor-flow.io),
add a client per brand, set repo secrets `TEST_API_KEY` / `TEST_PROJECT_ID`, and push
to `main`. CI builds one APK per client. Give each client a Firestore tenant matching
its package name (add it to `firebase/seed/seed.mjs`).

Reference wiring: [FlavorFlow-io/android-jetpack-compose-sample](https://github.com/FlavorFlow-io/android-jetpack-compose-sample).
