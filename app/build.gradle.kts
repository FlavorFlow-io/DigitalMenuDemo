import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    // Renders the store screenshots and feature graphic from the app's own
    // Compose UI, so the listing art carries whatever branding the build has.
    id("io.github.lucianosantosdev.storescreenshots")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hotswan.compiler)
}

// Signing comes from the environment so the keystore never lives in the repo.
// All of it absent is the normal case for a local debug build, and release then
// stays unsigned rather than failing the build.
val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasSigning = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

// Play rejects a bundle whose versionCode it has seen before, so CI overrides
// these per release. Each white-label client is its own Play app with its own
// sequence, which is why this is passed in rather than derived from the repo.
val buildVersionCode = (providers.gradleProperty("versionCode").orNull)?.toInt() ?: 1
val buildVersionName = providers.gradleProperty("versionName").orNull ?: "1.0"

android {
    namespace = "io.flavorflow.demo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.flavorflow.demo"
        minSdk = 24
        versionCode = buildVersionCode
        versionName = buildVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // findByName, not getByName: null simply means "unsigned", which is
            // what a developer building release locally should get.
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.hotswan.preview)
}

storeScreenshots {
    // Fastlane's layout, so the same folder can feed an upload either way.
    destDir = layout.projectDirectory.dir("screenshots")
}

// ── The client's menu, bundled at build time ────────────────────────────────
//
// A client's menu is a JSON document uploaded to FlavorFlow as the `menu_json`
// asset variable. In CI, apply-flavor-action downloads it and exports its path
// as $MENU_JSON; a local build falls back to the checked-in `app/menu/menu.json`.
//
// The product photos are fetched here rather than at runtime, and packaged as
// app assets. That is what makes the store screenshots show real food: they are
// captured by a Robolectric unit test, which has neither a device nor a network,
// so an https URL would only ever render the placeholder.
abstract class PrepareMenuAssets : DefaultTask() {

    /** The client's menu JSON — `$MENU_JSON` in CI, the checked-in default locally. */
    @get:InputFile
    abstract val source: RegularFileProperty

    /** Receives `menu.json` plus a `menu/` directory of downloaded photos. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun prepare() {
        val sourceFile = source.get().asFile
        val parsed = try {
            groovy.json.JsonSlurper().parse(sourceFile, "UTF-8")
        } catch (e: Exception) {
            throw GradleException("$sourceFile is not valid JSON: ${e.message}")
        }

        @Suppress("UNCHECKED_CAST")
        val menu = parsed as? MutableMap<String, Any?>
            ?: throw GradleException("$sourceFile must be a JSON object with `categories` and `products`.")

        @Suppress("UNCHECKED_CAST")
        val products = menu["products"] as? MutableList<Any?>
        if (products.isNullOrEmpty()) {
            throw GradleException("$sourceFile has no `products`. A build with an empty menu would ship a blank app.")
        }
        if ((menu["categories"] as? List<*>).isNullOrEmpty()) {
            throw GradleException("$sourceFile has no `categories`. Products are grouped by category, so the menu would render empty.")
        }

        val out = outputDir.get().asFile
        // Wiped rather than merged: a previous run's photos belong to whatever
        // client was built then, and nothing in the new menu would overwrite them.
        out.deleteRecursively()
        val imagesDir = File(out, "menu").apply { mkdirs() }

        // The restaurant's cover photo, bundled the same way as the dishes so the
        // menu's header is painted from disk on first frame.
        (menu["banner"] as? String)?.let { banner ->
            if (banner.startsWith("http://") || banner.startsWith("https://")) {
                download(banner, imagesDir)?.let { menu["banner"] = "file:///android_asset/menu/${it.name}" }
            }
        }

        products.forEachIndexed { index, raw ->
            @Suppress("UNCHECKED_CAST")
            val product = raw as? MutableMap<String, Any?>
                ?: throw GradleException("$sourceFile: products[$index] is not a JSON object.")
            val url = product["imageUrl"] as? String ?: return@forEachIndexed
            if (!url.startsWith("http://") && !url.startsWith("https://")) return@forEachIndexed
            val downloaded = download(url, imagesDir) ?: return@forEachIndexed
            product["imageUrl"] = "file:///android_asset/menu/${downloaded.name}"
        }

        File(out, "menu.json").writeText(
            groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(menu)),
            Charsets.UTF_8,
        )
        logger.lifecycle("Bundled ${products.size} products from $sourceFile, ${imagesDir.list()?.size ?: 0} photos.")
    }

    /**
     * Fetches [url] into [dir], named after the URL's digest so the same photo
     * used twice is stored once. Returns null — with a warning rather than a
     * failure — when it cannot be fetched: an offline build should still produce
     * an app, it just produces one with placeholders where the photos go.
     */
    private fun download(url: String, dir: File): File? = try {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "image/*")
            // Some image hosts refuse the default Java user agent outright, and
            // Wikimedia's robot policy wants a contactable one — without the URL
            // it answers a run of requests with 429 and the menu loses photos.
            setRequestProperty(
                "User-Agent",
                "FlavorFlow-DigitalMenu/1.0 (+https://github.com/FlavorFlow-io/DigitalMenuDemo)"
            )
        }
        connection.inputStream.use { stream ->
            val digest = MessageDigest.getInstance("SHA-1")
                .digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val target = File(dir, digest + extensionFor(url, connection.contentType))
            target.outputStream().use { stream.copyTo(it) }
            target
        }
    } catch (e: Exception) {
        logger.warn("Could not fetch $url (${e.message}). That product keeps its remote URL, so its photo will be missing from the screenshots.")
        null
    }

    /** The URL's own extension when it has a usable one, else the served type. */
    private fun extensionFor(url: String, contentType: String?): String {
        val fromPath = URI(url).path.substringAfterLast('.', "").lowercase()
        if (fromPath in setOf("jpg", "jpeg", "png", "webp", "gif")) return ".$fromPath"
        return when {
            contentType == null -> ".jpg"
            contentType.startsWith("image/png") -> ".png"
            contentType.startsWith("image/webp") -> ".webp"
            contentType.startsWith("image/gif") -> ".gif"
            else -> ".jpg"
        }
    }
}

val prepareMenuAssets = tasks.register<PrepareMenuAssets>("prepareMenuAssets") {
    description = "Bundles the client's menu JSON and its product photos as app assets."
    source.set(
        layout.file(providers.environmentVariable("MENU_JSON").map { File(it) })
            .orElse(layout.projectDirectory.file("menu/menu.json"))
    )
}

androidComponents {
    onVariants { variant ->
        // AGP owns the output location and the task dependency; every variant,
        // including the unit test that captures the screenshots, sees the assets.
        variant.sources.assets?.addGeneratedSourceDirectory(prepareMenuAssets, PrepareMenuAssets::outputDir)
    }
}
