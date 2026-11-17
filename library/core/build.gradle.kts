plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hyperalarm.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.hyperalarm\"")
    }

    buildFeatures {
        buildConfig = true
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
         * This module carries the fork's inherited translations. Most of those strings are gone
         * (the screens that used them were removed) and the rest were never translated into every
         * locale the parent project shipped. The i18n guard covers the strings this fork declares,
         * so keeping these two as lint errors only hides the checks that do matter here - unused
         * resources in particular.
         */
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // api(projects.library.hook)
    api(projects.library.common)
    api(projects.library.provision)
    api(libs.appiconloader)
}
