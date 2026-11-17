plugins {
    alias(libs.plugins.android.library)
}
android {

    namespace = "com.hyperalarm.provision"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    buildFeatures {
        aidl = true
    }

    /**
     * The agreement and privacy texts live in docs/terms/<locale>/ at the repository root, and are
     * copied into the APK from there. One source of truth: the copy fetched over the network at
     * runtime points at the same files, so a bundled fallback can never drift from the online text.
     */
    val termsSource = rootProject.layout.projectDirectory.dir("docs/terms")
    val copyTerms = tasks.register<CopyTermsAssets>("copyTermsAssets") {
        // 落点是 assets/terms/<locale>，与 TermsAssets.ROOT 保持一致
        from(termsSource) { into("terms") }
    }
    androidComponents.onVariants { variant ->
        // addGeneratedSourceDirectory wants a DirectoryProperty, so the task carries one.
        variant.sources.assets?.addGeneratedSourceDirectory(copyTerms, CopyTermsAssets::outputDir)
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

android {
    lint {
        /*
         * The setup wizard carries the parent project's partial translations: its strings exist in
         * some locales only. Treating that as an error hid the checks that matter for this module,
         * and the i18n guard already enforces full locale coverage for the strings this fork owns.
         */
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    api(projects.library.common)
    api(projects.library.libhook)
}

/**
 * Mirrors docs/terms into the APK's assets. The task is wired through
 * {@code addGeneratedSourceDirectory}, which requires the output directory to be a property rather
 * than the plain `destinationDir` Copy already has.
 */
abstract class CopyTermsAssets : Sync() {
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    init {
        // Sync, not Copy: a locale removed from docs/terms has to disappear from the APK as well.
        // With Copy the stale directory stayed in the generated output and kept shipping.
        into(outputDir)
    }
}
