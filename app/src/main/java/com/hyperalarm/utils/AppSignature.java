/*
 * This file is part of HyperAlarm.

 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperAlarm Contributions
 */
package com.hyperalarm.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import com.hyperalarm.BuildConfig;
import com.hyperalarm.common.log.AndroidLog;

import java.security.MessageDigest;

/**
 * Signature self check, used only to tell an official build apart from a repackaged one.
 *
 * <p>The expected digest is injected at build time from the keystore the build actually signs with
 * ({@code EXPECTED_SIGNATURE_SHA256} in {@code BuildConfig}), so the comparison never depends on
 * another project's key. While a build signs with the debug key the value stays empty and the check
 * reports "unknown" instead of inventing a verdict.</p>
 */
public final class AppSignature {
    private static final String TAG = "AppSignature";

    private AppSignature() {
    }

    /** SHA-256 of the certificate this APK is signed with, upper case hex, or "" when unreadable. */
    public static String getSha256Signature(Context context) {
        if (context == null) return "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo == null) return "";
                Signature[] signatures = info.signingInfo.hasMultipleSigners()
                        ? info.signingInfo.getApkContentsSigners()
                        : info.signingInfo.getSigningCertificateHistory();
                return signatures == null || signatures.length == 0 ? "" : digest(signatures[0]);
            }
            info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            return info.signatures == null || info.signatures.length == 0
                    ? "" : digest(info.signatures[0]);
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to read the signing certificate", t);
            return "";
        }
    }

    /** SHA-256 the build was configured to expect, or "" when the build used the debug key. */
    public static String getExpectedSignature() {
        String expected = BuildConfig.EXPECTED_SIGNATURE_SHA256;
        return expected == null ? "" : expected.trim();
    }

    /**
     * @return true when the installed certificate matches the one the build expected. A build
     * without an injected digest cannot answer, so it reports true instead of warning the user
     * about a mismatch it cannot prove.
     */
    public static boolean isSignCheckPass(Context context) {
        String expected = getExpectedSignature();
        if (expected.isEmpty()) return true;
        String actual = getSha256Signature(context);
        return !actual.isEmpty() && expected.equalsIgnoreCase(actual);
    }

    private static String digest(Signature signature) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (Throwable t) {
            return "";
        }
    }
}
