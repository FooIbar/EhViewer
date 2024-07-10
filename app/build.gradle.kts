import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP

val isRelease: Boolean
    get() = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    compileSdk = if (isRelease) 35 else 34
    buildToolsVersion = "35.0.0"
    ndkVersion = "27.0.11902837-rc1"
    androidResources.generateLocaleConfig = true

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

    val chromeVersion = rootProject.layout.projectDirectory.file("chrome-for-testing/LATEST_RELEASE_STABLE").asFile
        .readText().substringBefore('.')

    val githubToken = gradleLocalProperties(rootDir, providers)["GITHUB_TOKEN"] as? String
        ?: System.getenv("GITHUB_TOKEN").orEmpty()

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        minSdk = 26
        targetSdk = 35
        versionCode = 180058
        versionName = "1.12.0"
        versionNameSuffix = "-SNAPSHOT"
        resourceConfigurations.addAll(
            listOf(
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
            ),
        )
        buildConfigField("String", "RAW_VERSION_NAME", "\"$versionName${versionNameSuffix.orEmpty()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("long", "COMMIT_TIME", commitTime)
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
        buildConfigField("String", "CHROME_VERSION", "\"$chromeVersion\"")
        buildConfigField("String", "GITHUB_TOKEN", "\"$githubToken\"")
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
        create("default")
        create("marshmallow") {
            minSdk = 23
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
            lint {
                abortOnError = false
            }
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
        viewBinding = true
    }

    namespace = "com.hippo.ehviewer"
}

composeCompiler {
    enableNonSkippingGroupOptimization = true
    enableStrongSkippingMode = true
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(
            "/META-INF/**",
            "/kotlin/**",
            "**.txt",
            "**.bin",
        )
    }
}

baselineProfile {
    mergeIntoMain = true
}

dependencies {
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

    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // https://developer.android.com/jetpack/androidx/releases/room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime)
    implementation(libs.photoview)
    implementation(libs.material.motion.core)

    implementation(libs.bundles.splitties)

    implementation(libs.okio.jvm)

    implementation(libs.logcat)

    implementation(libs.diff)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.accompanist.drawable.painter)

    implementation(libs.insetter) // Dead Dependency

    // implementation(libs.reorderable)

    implementation(platform(libs.arrow.stack))
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.resilience)

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(libs.telephoto.zoomable)

    implementation(libs.ktor.client.core)

    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jsoup)

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.cronet.embedded)

    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":benchmark"))

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs = listOf(
            // https://kotlinlang.org/docs/compiler-reference.html#progressive
            "-progressive",
            "-Xjvm-default=all",
            "-Xcontext-receivers",

            "-opt-in=coil3.annotation.ExperimentalCoilApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.paging.ExperimentalPagingApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=splitties.experimental.ExperimentalSplittiesApi",
            "-opt-in=splitties.preferences.DataStorePreferencesPreview",
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
