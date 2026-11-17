plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.lsparanoid)
}

lsparanoid {
    seed = 227263
    classFilter = { true }
    includeDependencies = true
    variantFilter = { variant ->
        variant.buildType != "debug"
    }
}

android {
    namespace = "com.hyperalarm.libhook"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
        consumerProguardFiles("diagnostics-rules.pro")

        buildConfigField("String", "APP_MODULE_ID", "\"com.hyperalarm\"")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
    }
}

android {
    lint {
        /*
         * Same reason as the other library modules: the inherited translations cover a subset of
         * locales. The i18n guard already enforces that every string this fork declares has a
         * 梗体中文 entry, which is the coverage that matters here.
         */
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }

    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+MultiDollarInterpolation")
    }
}

dependencies {
    api(libs.core)
    api(libs.fragment)
    api(libs.recyclerview)
    api(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    api(libs.core.ktx)
    compileOnlyApi(projects.library.hiddenApi)

    // libxposed API 102 (compileOnly, runs against framework on device)
    compileOnlyApi(libs.libxposed.api)
    api(libs.libxposed.service)

    api(libs.ezhooktool.core)
    api(libs.ezhooktool.xposed102)
    api(libs.gson)
    api(libs.hyperfocusapi)
    implementation("com.github.topjohnwu.libsu:core:5.0.5")
    api(libs.lunarcalendar)

    api(projects.library.common)
}
