// file:noinspection DependencyNotationArgument
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.security.KeyStore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
}

val apkId = "HyperAlarm"
val gitHash: String by lazy { runGitCommand("rev-parse", "--short", "HEAD") ?: "unknown" }
val gitHashLong: String by lazy { runGitCommand("rev-parse", "HEAD") ?: "unknown" }
val gitBranch: String by lazy { runGitCommand("branch", "--show-current") ?: "unknown" }

/**
 * The version users see and talk about: semantic, bumped by hand when releasing.
 *
 * <p>1.0.0 first usable release, 1.0.x bug fixes, 1.x.0 new features, 2.0.0 a breaking change
 * (data contract, bridge protocol). The tag pushed by CI uses this same string.</p>
 */
val appVersionName = "1.0.1"

/**
 * The version Android compares: {@code yyyyMMdd * 10 + build-of-the-day}.
 *
 * <p>Derived from the date rather than from git history on purpose. A commit count silently goes
 * backwards after a rebase, a squash or a force push, and such a build cannot be installed over
 * the one already on the phone; a date only ever moves forward, no matter what happens to history,
 * or which branch built it.</p>
 *
 * <p>Same day, more than one build: pass {@code -PdailyBuild=1} (up to 9).</p>
 */
val dailyBuild: Int = (findProperty("dailyBuild") as String?)?.toIntOrNull() ?: 0
val appVersionCode: Int by lazy {
    require(dailyBuild in 0..9) {
        "dailyBuild must be 0..9 (yyyyMMdd * 10 leaves one digit for the day), got $dailyBuild"
    }
    val format = SimpleDateFormat("yyyyMMdd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    format.format(Date()).toInt() * 10 + dailyBuild
}

fun runGitCommand(vararg args: String): String? = runCatching {
    ProcessBuilder(listOf("git") + args)
        .redirectErrorStream(true)
        .start()
        .let { process ->
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }
}.getOrNull()

fun loadPropertiesFromFile(fileName: String): Properties? =
    rootProject.file(fileName).takeIf { it.exists() }?.let { file ->
        Properties().apply { load(file.inputStream()) }
    }

/**
 * SHA-256 of the certificate the build signs with, used by the in-app signature self check.
 *
 * Reading the keystore here keeps the expected value in one place: it always describes the key the
 * APK is actually signed with, instead of a hard coded digests of some other project. A build without
 * `signing.properties` falls back to the debug key and injects an empty value, which makes the check
 * report "unknown" rather than warning the user about a mismatch nobody can verify.
 */
fun expectedSignatureSha256(): String = runCatching {
    val signing = loadPropertiesFromFile("signing.properties") ?: return@runCatching ""
    val storeFile = signing.getProperty("storeFile") ?: return@runCatching ""
    val storePassword = signing.getProperty("storePassword").orEmpty()
    val keyAlias = signing.getProperty("keyAlias") ?: return@runCatching ""
    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    rootProject.file(storeFile).inputStream().use {
        keystore.load(it, storePassword.toCharArray())
    }
    val certificate = keystore.getCertificate(keyAlias) ?: return@runCatching ""
    val hash = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
    hash.joinToString("") { byte -> "%02X".format(byte) }
}.getOrDefault("")

/** GPR credentials live in the same file, so its presence alone does not mean "there is a keystore". */
val hasKeystore: Boolean = loadPropertiesFromFile("signing.properties")
    ?.getProperty("storeFile")?.isNotBlank() == true

val properties = loadPropertiesFromFile("signing.properties")

/** True when this invocation builds a release variant; used to keep build-host details out of it. */
val isReleaseBuild: Boolean = gradle.startParameter.taskNames.any {
    it.contains("assembleRelease", ignoreCase = true) || it.contains("bundleRelease", ignoreCase = true)
}

/** Escape hatch for local test builds: allow the public debug key when no keystore is configured. */
val allowDebugSigning: Boolean = providers.gradleProperty("allowDebugSigning").isPresent

/**
 * Refuses to produce a publishable APK signed with the public debug key.
 *
 * <p>The check lives in the task graph rather than in the build-type configuration: configuring a
 * build type happens for every task, including lint and the i18n check, and those must keep working
 * on a machine without a keystore.</p>
 */
gradle.taskGraph.whenReady {
    if (allowDebugSigning || hasKeystore) return@whenReady
    // Only the tasks that actually produce an installable artifact. lintRelease also pulls in
    // packageReleaseResources, so a substring match on "package" would refuse to lint.
    val publishableSuffixes = listOf("Release", "Beta", "Canary")
    val publishable = allTasks.any { task ->
        val path = task.path
        path.startsWith(":app:") && publishableSuffixes.any { suffix ->
            path == ":app:assemble$suffix" ||
                path == ":app:bundle$suffix" ||
                path == ":app:package$suffix"
        }
    }
    if (publishable) {
        throw GradleException(
            "signing.properties is required to build a publishable APK. Copy " +
                "signing.properties.example, point it at your keystore, or pass " +
                "-PallowDebugSigning for a throwaway local build."
        )
    }
    logger.warn(
        "HyperAlarm: signing.properties is missing; lint and checks still run, publishable APKs " +
            "are refused (or use -PallowDebugSigning)."
    )
}
// Only when a keystore is configured: signing.properties doubles as the GPR credential file, and a
// file holding just gpr.user/gpr.key must not break lint or checkI18n on a fresh clone.
if (hasKeystore && expectedSignatureSha256().isEmpty()) {
    throw GradleException(
        "signing.properties names a storeFile but its certificate digest could not be read; the " +
            "in-app signature check would be silently disabled. Check storeFile / storePassword / keyAlias."
    )
}

android {
    namespace = "com.hyperalarm"
    compileSdk = 37
    compileSdkMinor = 0
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.zoeicx.hyperalarm"
        // Room writes the schema here so migrations can be reviewed and tested.
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
        minSdk = 35
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())

        val buildConfigData = mapOf(
            "BUILD_TIME" to buildTime,
            "BUILD_OS_NAME" to if (isReleaseBuild) "" else System.getProperty("os.name"),
            // Only debug/beta builds describe the machine; a release APK must not carry it.
            "BUILD_USER_NAME" to if (isReleaseBuild) "" else System.getProperty("user.name"),
            "BUILD_JAVA_VERSION" to System.getProperty("java.version"),
            "GIT_BRANCH" to gitBranch
        )

        for ((key, value) in buildConfigData) {
            buildConfigField("String", key, "\"$value\"")
        }

        buildConfigField("String", "EXPECTED_SIGNATURE_SHA256", "\"${expectedSignatureSha256()}\"")

    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x36")
    }

    packaging {
        resources {
            merges += listOf("META-INF/xposed/*")
            excludes += listOf("**")
        }
        jniLibs {
            useLegacyPackaging = false
        }
        dex {
            useLegacyPackaging = true
        }
    }

    fun getString(propertyName: String, environmentName: String, prompt: String): String =
        properties?.getProperty(propertyName)
            ?: System.getenv(environmentName)
            ?: System.console()?.readLine("\n$prompt: ").orEmpty()

    val buildTimeSuffix: String by lazy {
        SimpleDateFormat("MMddHHmm").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())
    }
    // Same clock as appVersionCode: a CI runner in UTC between 16:00 and midnight put one date into
    // the version code and the previous one into the version name, so tag, file name and code
    // disagreed on which day the build belonged to.
    val dateSuffix: String by lazy {
        DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
    }

    signingConfigs {
        create("hasProperties") {
            if (hasKeystore) {
                storeFile = rootProject.file(getString("storeFile", "STORE_FILE", "Store file"))
                storePassword = getString("storePassword", "STORE_PASSWORD", "Store password")
                keyAlias = getString("keyAlias", "KEY_ALIAS", "Key alias")
                keyPassword = getString("keyPassword", "KEY_PASSWORD", "Key password")
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        val configSigning: ApplicationBuildType.() -> Unit = {
            if (hasKeystore) {
                signingConfig = signingConfigs.findByName("hasProperties")
            } else {
                // The debug key is public: an APK signed with it cannot be published, and a later
                // build with the real key cannot be installed over it. Whether that is acceptable is
                // decided per task in the task-graph check below, so tasks that only read sources
                // (lint, checkI18n, unit tests) keep working without a keystore.
                signingConfig = signingConfigs.findByName("debug")
            }
        }

        val applyBase: ApplicationBuildType.() -> Unit = {
            optimization.enable = true
            buildConfigField("String", "GIT_CODE", "\"$appVersionCode\"")
        }

        release {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            versionNameSuffix = "-$dateSuffix"
        }

        create("beta") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-$dateSuffix"
        }

        create("canary") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-${gitHash}-r${appVersionCode}"
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            buildConfigField("String", "GIT_CODE", "\"$appVersionCode\"")
            versionNameSuffix = "-${buildTimeSuffix}-r${appVersionCode}"
            if (properties != null) {
                signingConfig = signingConfigs.findByName("hasProperties")
            }
        }
    }

}

afterEvaluate {
    base {
        val buildTypeName = gradle.startParameter.taskNames
            .firstOrNull { it.contains("assemble", ignoreCase = true) }
            ?.substringAfterLast(":")
            ?.replace("assemble", "", ignoreCase = true)
            ?.lowercase() ?: "debug"
        val suffix = android.buildTypes.findByName(buildTypeName)?.versionNameSuffix ?: ""
        archivesName.set("$apkId-${android.defaultConfig.versionName}$suffix")
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin.jvmToolchain(25)

dependencies {
    implementation(projects.library.core)
    implementation(projects.library.common)

    api (libs.room.runtime)
    // FTS 支持
    annotationProcessor (libs.room.compiler)
    implementation("com.github.topjohnwu.libsu:core:5.0.5")
}

