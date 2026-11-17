package com.hyperalarm.provision.terms;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Locates the bundled user agreement / privacy policy for the current locale.
 *
 * <p>The documents ship inside the APK under {@code assets/terms/<locale>/}, so they render offline,
 * do not depend on any third party host, and are versioned together with the code that the user is
 * actually agreeing to. {@code locales_config.xml} lists every locale the app supports; when one is
 * missing or its file cannot be read the resolver falls back to the language, then to English.</p>
 */
public final class TermsAssets {
    public static final String USER_AGREEMENT = "USER_AGREEMENT.md";
    public static final String PRIVACY = "PRIVACY.md";

    private static final String ROOT = "terms/";
    /** Any document; only its presence matters when picking a directory. */
    private static final String FALLBACK_DOC = USER_AGREEMENT;
    private static final String FALLBACK_LOCALE = "en";

    private TermsAssets() {
    }

    /** Asset path of the document in the best matching locale. */
    public static String path(Context context, String document) {
        if (context == null) return ROOT + FALLBACK_LOCALE + "/" + document;
        AssetManager assets = context.getAssets();
        for (String dir : candidates(context)) {
            String path = ROOT + dir + "/" + document;
            if (exists(assets, path)) return path;
        }
        return ROOT + FALLBACK_LOCALE + "/" + document;
    }

    /**
     * Locale directory that actually has a document, falling back to the language, then English.
     *
     * <p>Used for the bundled copy and for building the online URL, so both point at the same
     * locale - a user reading German must not get the online German text and the bundled English one.</p>
     */
    public static String localeDir(Context context) {
        if (context == null) return FALLBACK_LOCALE;
        AssetManager assets = context.getAssets();
        for (String dir : candidates(context)) {
            if (exists(assets, ROOT + dir + "/" + FALLBACK_DOC)) return dir;
        }
        return FALLBACK_LOCALE;
    }

    private static List<String> candidates(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        Locale locale = configuration.getLocales().isEmpty()
                ? Locale.getDefault() : configuration.getLocales().get(0);
        String language = normalizeLanguage(locale.getLanguage());
        String country = locale.getCountry();
        List<String> dirs = new ArrayList<>(3);
        if (!country.isEmpty()) dirs.add(language + "-" + country);
        dirs.add(language);
        dirs.add(FALLBACK_LOCALE);
        return dirs;
    }

    /** Android still reports a few legacy codes; the resource folders use the modern ones. */
    private static String normalizeLanguage(String language) {
        if ("in".equals(language)) return "id";
        if ("iw".equals(language)) return "he";
        if ("ji".equals(language)) return "yi";
        return language;
    }

    private static boolean exists(AssetManager assets, String path) {
        try {
            assets.open(path).close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
