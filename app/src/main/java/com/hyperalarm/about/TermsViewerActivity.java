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
package com.hyperalarm.about;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.hyperalarm.R;
import com.hyperalarm.provision.terms.TermsAssets;
import com.hyperalarm.provision.widget.MarkdownView;

import fan.appcompat.app.AppCompatActivity;

/**
 * Renders the bundled user agreement or privacy policy.
 *
 * <p>The documents ship inside the APK (see {@link TermsAssets}), so they open offline and always
 * match the build the user is running. Both the about page and the notice dialog point here instead
 * of sending the user to an external web page.</p>
 */
public class TermsViewerActivity extends AppCompatActivity {
    private static final String EXTRA_DOCUMENT = "document";

    public static Intent intent(Context context, String document) {
        return new Intent(context, TermsViewerActivity.class).putExtra(EXTRA_DOCUMENT, document);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String document = getIntent().getStringExtra(EXTRA_DOCUMENT);
        if (!TermsAssets.PRIVACY.equals(document)) document = TermsAssets.USER_AGREEMENT;
        setTitle((CharSequence) getString(TermsAssets.PRIVACY.equals(document)
                ? com.hyperalarm.core.R.string.about_privacy
                : com.hyperalarm.core.R.string.about_protocol));
        setContentView(R.layout.activity_terms_viewer);
        MarkdownView content = findViewById(R.id.terms_content);
        content.loadMarkdownFromAsset(TermsAssets.path(this, document));
    }
}
