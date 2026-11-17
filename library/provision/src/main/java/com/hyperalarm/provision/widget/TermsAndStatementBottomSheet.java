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
package com.hyperalarm.provision.widget;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.hyperalarm.provision.R;

import fan.provision.OobeUtils;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;

public class TermsAndStatementBottomSheet {

    static FragmentActivity mActivity;
    BottomSheetModal mBottomSheet;

    @SuppressLint("StaticFieldLeak")
    static ProgressBar mProgressBar;
    @SuppressLint("StaticFieldLeak")
    static TextView verificationCodeTip;
    static MarkdownView mMarkdownView;

    public TermsAndStatementBottomSheet(FragmentActivity activity) {
        mActivity = activity;
        mBottomSheet = new BottomSheetModal(activity);
        mBottomSheet.setDragHandleViewEnabled(true);
        BottomSheetBehavior<FrameLayout> behavior = mBottomSheet.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setDraggable(true);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);
        behavior.setFixedHeightRatioEnabled(false);

        mBottomSheet.setContentView(R.layout.fragment_bottom_sheet_web);
        View rootView = mBottomSheet.getRootView();
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mMarkdownView = rootView.findViewById(R.id.markdown);
        verificationCodeTip = rootView.findViewById(R.id.verification_code_tip);
        initView();
    }

    public static void initView() {
        OobeUtils.refreshSecureSixDigit();

        mProgressBar.setVisibility(View.VISIBLE);
        mMarkdownView.setVisibility(View.INVISIBLE);
        verificationCodeTip.setVisibility(View.GONE);

        String verificationCode = OobeUtils.getSecureSixDigit();

        mMarkdownView.setOnMarkdownLoadListener(success -> {
            if (success) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mMarkdownView.setVisibility(View.VISIBLE);
                verificationCodeTip.setText(
                    mActivity.getString(
                        R.string.provision_terms_of_use_verification_code_tip,
                        verificationCode
                    )
                );
                verificationCodeTip.setVisibility(View.VISIBLE);
                return;
            }
            // Online first, then the copy inside the APK. Both failing is the only case where the
            // user gets the failure notice - and then the verification code stays hidden, because
            // nobody has actually been shown an agreement.
            if (!mTriedBundled && sDocument != null && mActivity != null) {
                mTriedBundled = true;
                mMarkdownView.loadMarkdownFromAsset(
                        com.hyperalarm.provision.terms.TermsAssets.path(mActivity, sDocument));
                return;
            }
            mProgressBar.setVisibility(View.INVISIBLE);
            verificationCodeTip.setVisibility(View.GONE);
            mMarkdownView.setVisibility(View.VISIBLE);
            mMarkdownView.render(mActivity.getString(R.string.provision_get_md_failed)
                    .replace("%URL%", sDocumentUrl != null ? sDocumentUrl : sDocument));
        });
    }


    /** Which document is on screen, so the "refresh" link can reload the same one. */
    private static String sDocument;
    private static String sDocumentUrl;
    /** True once the bundled copy has been tried for the current load. */
    private static boolean mTriedBundled;

    /**
     * Loads one document: online first, bundled copy as the fallback.
     *
     * <p>The text lives at {@code docs/terms/<locale>/} in the repository and is served from there,
     * so an update does not need a release; the copy in the APK is what makes the wizard readable
     * with no network at all.</p>
     */
    public static void loadDocument(android.content.Context context, String document) {
        if (mMarkdownView == null || context == null) return;
        sDocument = document;
        mTriedBundled = false;
        sDocumentUrl = com.hyperalarm.provision.terms.TermsUrls.url(context, document);
        mProgressBar.setVisibility(View.VISIBLE);
        mMarkdownView.setVisibility(View.INVISIBLE);
        mMarkdownView.loadMarkdownFromUrl(sDocumentUrl);
    }

    /** The failure notice's "refresh" link: same document, same online-then-bundled chain. */
    public static void reload() {
        if (sDocument == null || mActivity == null) return;
        loadDocument(mActivity, sDocument);
    }

    public static void loadMarkdown(String uri) {
        if (mMarkdownView != null) {
            mMarkdownView.loadMarkdownFromUrl(uri);
        }
    }

    public void show() {
        if (mBottomSheet != null) {
            mBottomSheet.show();
        }
    }

    private void dismiss() {
        if (mBottomSheet != null) {
            mBottomSheet.dismiss();
        }
    }
}
