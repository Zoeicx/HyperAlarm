package com.hyperalarm.about;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.hyperalarm.R;
import com.hyperalarm.dashboard.SettingsPreferenceFragment;
import com.hyperalarm.utils.GithubUserContentGetter;

import java.util.ArrayList;
import java.util.HashSet;
import com.hyperalarm.common.utils.ThreadUtils;

public class AboutContributorsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {

    PreferenceCategory mContributors;
    PreferenceCategory mProcessing;
    PreferenceCategory mLoadFailed;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_about_contributor;
    }

    private PreferenceCategory mContributorsCategory;

    @Override
    public void initPrefs() {
        mContributorsCategory = findPreference("prefs_key_about_contributors");
        mContributors = findPreference("prefs_key_about_contributors");
        mProcessing = findPreference("prefs_key_about_contributors_progressing");
        mLoadFailed = findPreference("prefs_key_about_contributors_load_failed");

        if (mContributorsCategory == null) return;

        ThreadUtils.runOnBackground(() -> {
            ArrayList<String> contributors = getContributors("Zoeicx", "HyperAlarm");

            Activity activity = getActivity();
            if (isAdded() && activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    Context context = getContext();
                    if (context == null) {
                        return;
                    }
                    if (contributors.isEmpty()) {
                        mProcessing.setVisible(false);
                        mLoadFailed.setVisible(true);
                        return;
                    }
                    mContributorsCategory.removeAll();
                    for (String item : contributors) {
                        String[] parts = item.split(",", 2);
                        String login = parts[0].replace("@", "");
                        String nickname = parts.length > 1 ? parts[1] : login;

                        Preference p = new Preference(context);
                        p.setTitle(nickname);
                        p.setSummary("@" + login);
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/" + login));
                        p.setIntent(intent);

                        mContributorsCategory.addPreference(p);
                    }

                    mProcessing.setVisible(false);
                    mContributors.setVisible(true);
                });
            }
        });
    }

    /** Contributors of this repository, one entry per login. */
    public static ArrayList<String> getContributors(String owner, String repo) {
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> seenLogins = new HashSet<>();
        for (String item : GithubUserContentGetter.getContributors(owner, repo)) {
            if (seenLogins.add(item.split(",", 2)[0])) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        return false;
    }
}
