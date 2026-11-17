package com.hyperalarm.provision.terms;

import android.content.Context;

/**
 * Online location of the agreement and privacy policy.
 *
 * <p>The documents are written once under {@code docs/terms/<locale>/} in this repository and
 * served from there, so the text a user reads over the network is byte-for-byte the text that is
 * bundled into the APK as a fallback (see {@link TermsAssets}). Updating the agreement is then a
 * repository change rather than an app release.</p>
 *
 * <p>{@link #BASE} has to be a URL that actually exists. Until the repository is published under its
 * own account, every fetch fails and the bundled copy is used - which is the designed fallback, not
 * a broken state, but it also means the online path stays unverified.</p>
 */
public final class TermsUrls {

    /** Raw file base; the last path segment is the locale directory. */
    public static final String BASE =
            "https://raw.githubusercontent.com/Zoeicx/HyperAlarm/main/docs/terms/";

    private TermsUrls() {
    }

    /** Absolute URL of one document for the locale the app is currently running in. */
    public static String url(Context context, String document) {
        return BASE + TermsAssets.localeDir(context) + "/" + document;
    }
}
