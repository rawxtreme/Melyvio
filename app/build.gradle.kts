import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val baseApplicationId = "com.aaditya.melyvio"
val applicationIdOverride = System.getenv("MELYVIO_APPLICATION_ID")?.takeIf { it.isNotBlank() }
val appNameOverride = System.getenv("MELYVIO_APP_NAME")?.takeIf { it.isNotBlank() }
val buildCommit =
    System.getenv("MELYVIO_BUILD_COMMIT")
        ?.trim()
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
        ?.take(7)
        ?.lowercase()
// ---- Signing --------------------------------------------------------------------------------
// The release key is NOT part of this repository. It is read from a private file called
// keystore.properties (git-ignored, see keystore.properties.example) or from environment variables.
// Whoever builds the app has to create their own key.
val keystoreProps =
    Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

fun signingValue(
    propName: String,
    envName: String,
): String? = (keystoreProps.getProperty(propName) ?: System.getenv(envName))?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = signingValue("storeFile", "MELYVIO_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "MELYVIO_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "MELYVIO_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "MELYVIO_KEY_PASSWORD")
val hasReleaseSigning =
    releaseStoreFilePath != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null &&
        rootProject.file(releaseStoreFilePath).exists()

if (!hasReleaseSigning) {
    logger.warn("Melyvio: no release key found (keystore.properties). Release builds use this computer's debug key. See keystore.properties.example.")
}

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aaditya.melyvio"
    compileSdk = 37

    defaultConfig {
        applicationId = applicationIdOverride ?: baseApplicationId
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        val baseVersionName = requireNotNull(versionName)
        buildConfigField("String", "BASE_VERSION_NAME", "\"$baseVersionName\"")
        buildCommit?.let { versionName = "$baseVersionName+$it" }
        resValue("string", "app_name", appNameOverride ?: "Melyvio")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        buildConfigField("Long", "DISCORD_APP_ID", "1447278780795064401L")
    }

    // Single flavor => exactly ONE APK per build type (no gms / izzy copies).
    flavorDimensions += listOf("variant")
    productFlavors {
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        // "debug" is Android's own per-computer debug key (created automatically, never in the repo).
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Real key if keystore.properties exists, otherwise this computer's own debug key
            // (fine for testing; create keystore.properties for a real release).
            signingConfig =
                if (hasReleaseSigning) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
        debug {
            isDebuggable = true
            if (appNameOverride == null) {
                resValue("string", "app_name", "Melyvio")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
        // Lint never gated anything here (abortOnError = false), so the
        // lintVital pass that assembleRelease implicitly triggers was pure
        // build time. Run lint on demand with ./gradlew :app:lintGmsRelease.
        checkReleaseBuilds = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols +=
                listOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

/**
 * AndroidIDE / Android Studio can inject android:testOnly="true" into the merged manifest of IDE builds.
 * An APK with that flag is rejected by the normal package installer on other phones
 * ("App not installed as package appears to be invalid"). This strips the flag from every variant.
 */
abstract class StripTestOnlyTask : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun strip() {
        val text = mergedManifest.get().asFile.readText()
        val cleaned = text.replace(Regex("\\s+android:testOnly=\"true\""), "")
        updatedManifest.get().asFile.writeText(cleaned)
    }
}

androidComponents {
    onVariants { variant ->
        val strip = tasks.register<StripTestOnlyTask>("${variant.name}StripTestOnly")
        variant.artifacts
            .use(strip)
            .wiredWithFiles(StripTestOnlyTask::mergedManifest, StripTestOnlyTask::updatedManifest)
            .toTransform(SingleArtifact.MERGED_MANIFEST)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
        suppressWarnings.set(false)
    }
}

// Android provides org.json as a platform API (/apex/com.android.art/javalib/core-libart.jar).
// The standalone org.json:json artefact bundles an older Apache Harmony copy of JSONArray that
// contains an internal `myArrayList` field absent from the platform class.  Without obfuscation
// R8 inlines against this internal field; at runtime the platform class is resolved instead,
// producing a NoSuchFieldError.  Excluding the artefact globally ensures only the platform
// class is ever referenced.
configurations.configureEach {
    exclude(group = "org.json", module = "json")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.materialKolor)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.browser)

    implementation(libs.ucrop)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    // Protobuf for message serialization (lite version for Android)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.ktor.client.mock)
}


