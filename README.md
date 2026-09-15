# Digital Menu — a FlavorFlow white-label showcase

A single Jetpack Compose food-ordering app that is **re-branded per client at build
time** with [FlavorFlow](https://flavor-flow.io) — theme, name, package, icon *and*
the restaurant's own menu. One codebase → many branded apps, each shipping its own
dishes, photos and prices.

## What it demonstrates

| Concern | How |
| --- | --- |
| **White-labeling** | FlavorFlow's `apply-flavor-action` rewrites the Compose theme, `app_name`, `applicationId`, and launcher icon for each client in CI. See [`.github/workflows/build-white-label.yml`](.github/workflows/build-white-label.yml). |
| **Per-client data** | Each client uploads its menu to FlavorFlow as the `menu_json` **asset variable**. CI downloads it, fetches every product photo it names, and packages both as app assets — so the menu is as available as the app itself, offline and on first frame. |
| **No backend, no `google-services.json`** | Nothing is fetched at runtime. That also keeps one codebase working across many different `applicationId`s, which a plain Firebase SDK setup — bound to fixed package names — would not. |
| **Clean architecture / MVVM** | `domain` (models, repository interfaces, use cases) · `data` (menu parsing, bundled + in-memory repositories) · `presentation` (ViewModels, Compose UI). |
| **Listing art from the real app** | `:app:storeScreenshots` renders the Play screenshots and feature graphic from the app's own composables, over the same bundled menu — so the listing shows the client's actual dishes, not stock art. |

## Architecture

```
presentation/  MenuViewModel, CheckoutViewModel, Compose screens
     │  (StateFlow)
domain/        Category, Product, MenuSection · MenuRepository, CartRepository · GetMenuUseCase
     │  (interfaces)
data/          BundledMenuRepository ──▶ assets/menu.json  (bundled by :app:prepareMenuAssets)
               MenuJson (parser, shared with the screenshot fixtures)
               InMemoryCartRepository
```

## The menu document

`menu_json` is one JSON file per client. Categories render in array order:

```json
{
  "banner": "https://example.com/dining-room.jpg",
  "categories": [
    { "id": "starters", "name": "Starters" },
    { "id": "mains", "name": "Mains" }
  ],
  "products": [
    {
      "id": "p1",
      "categoryId": "starters",
      "name": "Pão de alho",
      "description": "Grilled garlic bread, house butter",
      "price": 12.0,
      "imageUrl": "https://example.com/pao-de-alho.jpg"
    }
  ]
}
```

`banner` is the restaurant's cover photo. It sits above the category tabs and
collapses as the menu scrolls, giving the grid the full screen once a diner is
browsing, and comes back when they scroll to the top. It is optional — a client
without one gets a branded header carrying the app name instead.

`banner` and every `imageUrl` are fetched at **build time** by the
`prepareMenuAssets` Gradle task and rewritten to `file:///android_asset/menu/…`,
so no photo is ever loaded over the network at runtime.

A category with no products is dropped rather than rendered as an empty tab. A URL
that cannot be fetched is left alone, with a warning — the build still succeeds,
that product just shows a placeholder.

[`menu.sample.json`](menu.sample.json) at the repository root is a complete example
— five categories, fifteen products, one of them deliberately without a photo or a
description to show that both are optional.

### Where the document comes from — `$MENU_JSON`

`prepareMenuAssets` resolves its input in this order:

1. **`$MENU_JSON`** — a path to the menu document. In CI, `apply-flavor-action`
   downloads the asset variables of the client it is building and exports each
   one's path under its upper-cased name, so `menu_json` arrives as `$MENU_JSON`.
   Set it yourself to build any client's menu locally.
2. **[`app/menu/menu.json`](app/menu/menu.json)** — the checked-in fallback, used
   whenever `$MENU_JSON` is not set: local builds, forks with no API key, and any
   client that has not uploaded a menu.

The resolved file is a task input, so switching menus re-runs the bundling on the
next build, and a `$MENU_JSON` that points nowhere fails the build rather than
quietly falling back to the checked-in menu.

## Getting started

**Run the base (un-branded) app** — uses the checked-in menu:

```bash
./gradlew assembleDebug
```

**Render the store listing art:**

```bash
./gradlew :app:storeScreenshots     # → app/screenshots/{en-US,pt-BR}/images/
```

**Preview another client's menu without a FlavorFlow project** — point
`$MENU_JSON` at any menu document, starting with the bundled sample:

```bash
MENU_JSON=$PWD/menu.sample.json ./gradlew :app:storeScreenshots
MENU_JSON=/path/to/that-client.json ./gradlew assembleDebug
```

**Produce white-label builds:** create a project on [flavor-flow.io](https://flavor-flow.io),
declare a **mandatory** project variable `menu_json` of type **Asset**, add a client per
brand and upload each one's menu, set repo secrets `TEST_API_KEY` / `TEST_PROJECT_ID`,
and push to `main`. CI builds one APK per client.

> Asset variables are an Enterprise plan feature — see the pricing page.

Reference wiring: [FlavorFlow-io/android-jetpack-compose-sample](https://github.com/FlavorFlow-io/android-jetpack-compose-sample).
