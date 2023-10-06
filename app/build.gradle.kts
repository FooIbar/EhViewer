import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val isRelease: Boolean
    get() = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.aboutlibrariesPlugin)
    alias(libs.plugins.rustAndroidPlugin)
    alias(libs.plugins.composeCompilerReportGenerator)
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.0.10792818"

    splits {
        abi {
            isEnable = true
            reset()
            if (isRelease) {
                include("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
                isUniversalApk = true
            } else {
                include("arm64-v8a")
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

    val buildTime by lazy {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneOffset.UTC)
        formatter.format(Instant.now())
    }

    val repoName = providers.exec {
        commandLine = "git remote get-url origin".split(' ')
    }.standardOutput.asText.get().trim().removePrefix("https://github.com/").removePrefix("git@github.com:")
        .removeSuffix(".git")

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        minSdk = 23
        targetSdk = 34
        versionCode = 180043
        versionName = "1.8.10.0-SNAPSHOT"
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
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
    }

    externalNativeBuild {
        cmake {
            path = File("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            // https://kotlinlang.org/docs/compiler-reference.html#progressive
            "-progressive",
            "-Xjvm-default=all",
            "-Xlambdas=indy",
            "-XXLanguage:+BreakContinueInInlineLambdas",

            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
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

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable.add("MissingTranslation")
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "**.txt"
            excludes += "**.bin"
        }
    }

    dependenciesInfo.includeInApk = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signConfig
            buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "BUILD_TIME", "\"\"")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    namespace = "com.hippo.ehviewer"
}

dependencies {
    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.collection)

    // https://developer.android.com/jetpack/androidx/releases/compose-material3
    api(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.androidx.core)

    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.fragment)
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(libs.androidx.lifecycle.process)

    // https://developer.android.com/jetpack/androidx/releases/navigation
    implementation(libs.bundles.androidx.navigation)

    // https://developer.android.com/jetpack/androidx/releases/paging
    implementation(libs.bundles.androidx.paging)

    implementation(libs.bundles.androidx.recyclerview)

    // https://developer.android.com/jetpack/androidx/releases/room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime)
    implementation(libs.drawer)
    implementation(libs.photoview) // Dead Dependency
    implementation(libs.directionalviewpager) // Dead Dependency
    // https://github.com/google/accompanist/releases
    implementation(libs.bundles.accompanist)
    implementation(libs.material)

    implementation(libs.bundles.splitties)

    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.okhttp)

    implementation(libs.okio.jvm)

    implementation(libs.aboutlibraries.core)

    implementation(libs.insetter) // Dead Dependency
    implementation(libs.bundles.rikkax)

    implementation(platform(libs.arrow.stack))
    implementation(libs.arrow.fx.coroutines)

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(libs.bundles.ktor)

    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)

    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.nop)

    debugImplementation(libs.leakcanary.android)

    coreLibraryDesugaring(libs.desugar)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
    exclusionPatterns = listOf(
        "androidx\\..*".toPattern(),
        ".*annotations".toPattern(),
        "com\\.google\\.code\\..*".toPattern(),
        "com\\.google\\.guava:listenablefuture".toPattern(),
        "com\\.google\\.protobuf:protobuf-javalite".toPattern(),
        "org\\.checkerframework:checker-qual".toPattern(),
        "org\\.chromium\\.net:cronet-embedded".toPattern(),
        "org\\.slf4j:slf4j-api".toPattern(),
    )
}

cargo {
    module = "src/main/rust"
    libname = "ehviewer_rust"
    targets = if (isRelease) listOf("arm", "x86", "arm64", "x86_64") else listOf("arm64")
    if (isRelease) profile = "release"
}

val ktlintVersion = libs.versions.ktlint.get()

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

tasks.configureEach {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("cargoBuild")
        // fix mergeDebugJniLibFolders  UP-TO-DATE
        inputs.dir(layout.buildDirectory.dir("rustJniLibs/android"))
    }
}
