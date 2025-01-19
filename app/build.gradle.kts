import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

val isRelease: Boolean
    get() = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.aboutlibrariesPlugin)
    alias(libs.plugins.composeCompilerReportGenerator)
    alias(libs.plugins.baselineprofile)
}

val supportedAbis = arrayOf("arm64-v8a", "x86_64", "armeabi-v7a")

android {
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf(
            "zh",
            "zh-rCN",
            "zh-rHK",
            "zh-rTW",
            "es",
            "ja",
            "ko",
            "fr",
            "de",
            "th",
            "tr",
            "nb-rNO",
        )
    }

    splits {
        abi {
            isEnable = true
            reset()
            if (isRelease) {
                include(*supportedAbis)
                isUniversalApk = true
            } else {
                include("arm64-v8a", "x86_64")
            }
        }
    }

    val signConfig = signingConfigs.create("release") {
        storeFile = File(projectDir.path + "/keystore/androidkey.jks")
        storePassword = "000000"
        keyAlias = "key0"
        keyPassword = "000000"
        enableV3Signing = true
        enableV4Signing = true
    }

    val commitSha = providers.exec {
        commandLine = "git rev-parse --short=7 HEAD".split(' ')
    }.standardOutput.asText.get().trim()

    val commitTime = providers.exec {
        commandLine = "git log -1 --format=%ct".split(' ')
    }.standardOutput.asText.get().trim()

    val repoName = providers.exec {
        commandLine = "git remote get-url origin".split(' ')
    }.standardOutput.asText.get().trim().removePrefix("https://github.com/").removePrefix("git@github.com:")
        .removeSuffix(".git")

    val chromeVersion = rootProject.layout.projectDirectory.file("chrome.version").asFile.readText().trim()

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        versionCode = 180062
        versionName = "1.14.0"
        versionNameSuffix = "-SNAPSHOT"
        buildConfigField("String", "RAW_VERSION_NAME", "\"$versionName${versionNameSuffix.orEmpty()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("long", "COMMIT_TIME", commitTime)
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
        buildConfigField("String", "CHROME_VERSION", "\"$chromeVersion\"")
        ndk {
            if (isRelease) {
                abiFilters.addAll(supportedAbis)
            }
            debugSymbolLevel = "FULL"
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
            }
        }
    }

    flavorDimensions += "api"

    productFlavors {
        create("default") {
            minSdk = 26
        }
        create("marshmallow") {
            applicationIdSuffix = ".m"
            versionNameSuffix = "-M"
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        checkReleaseBuilds = false
        disable += setOf("MissingTranslation", "MissingQuantity")
        error += setOf("InlinedApi")
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
        resources {
            // Required by Layout Inspector
            pickFirsts += "/META-INF/androidx.compose.ui_ui.version"

            excludes += listOf(
                "/META-INF/**",
                "/kotlin/**",
                "**.txt",
                "**.bin",
                "/okhttp3/**", // Okhttp public suffix
            )
        }
    }

    dependenciesInfo.includeInApk = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signConfig
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        create("benchmarkRelease") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    namespace = "com.hippo.ehviewer"
}

composeCompiler {
    featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

baselineProfile {
    mergeIntoMain = true
}

dependencies {
    implementation(projects.core.common)

    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)

    // https://developer.android.com/jetpack/androidx/releases/compose-material3
    api(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.compiler)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.graphics.path)

    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.compose)

    // https://developer.android.com/jetpack/androidx/releases/paging
    implementation(libs.androidx.paging.compose)

    // https://developer.android.com/jetpack/androidx/releases/room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime)
    implementation(libs.material.motion.core)
    implementation(libs.material.kolor)

    implementation(libs.bundles.splitties)

    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.android)

    implementation(libs.okio.jvm)

    implementation(libs.logcat)

    implementation(libs.diff)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.accompanist.drawable.painter)

    implementation(libs.reorderable)

    implementation(platform(libs.arrow.stack))
    implementation(libs.bundles.arrow)

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(libs.telephoto.zoomable)

    implementation(libs.ktor.client.okhttp)

    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jsoup)

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":benchmark"))

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}

kotlin {
    jvmToolchain(21)

    // https://kotlinlang.org/docs/gradle-compiler-options.html#all-compiler-options
    compilerOptions {
        progressiveMode = true
        optIn.addAll(
            "coil3.annotation.ExperimentalCoilApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
            "androidx.paging.ExperimentalPagingApi",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "splitties.experimental.ExperimentalSplittiesApi",
            "splitties.preferences.DataStorePreferencesPreview",
        )
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
            "-Xcontext-receivers",
            "-Xwhen-guards",
            "-Xsuppress-warning=CONTEXT_RECEIVERS_DEPRECATED",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("compose-destinations.codeGenPackageName", "com.hippo.ehviewer.ui")
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
}

val ktlintVersion = libs.ktlint.get().version

spotless {
    kotlin {
        // https://github.com/diffplug/spotless/issues/111
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}
