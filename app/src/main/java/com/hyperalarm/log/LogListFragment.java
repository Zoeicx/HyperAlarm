/*
 * This file is part of HyperAlarm.
 *
 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperAlarm Contributions
 */
package com.hyperalarm.log;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperalarm.R;
import com.hyperalarm.log.db.LogDao;
import com.hyperalarm.log.db.LogEntry;
import com.hyperalarm.common.log.AndroidLog;
import com.hyperalarm.log.db.LogRepository;
import com.hyperalarm.widget.PullToRefreshListener;
import com.hyperalarm.widget.PullViewHelper;

import java.util.ArrayList;
import java.util.List;

import fan.recyclerview.card.CardItemDecoration;
import fan.springback.view.SpringBackLayout;
import com.hyperalarm.common.utils.ThreadUtils;

public class LogListFragment extends Fragment {

    private static final String ALL_TAG_VALUE = "";
    private static final int STATE_LOADING = 0;
    private static final int STATE_CONTENT = 1;
    private static final int STATE_EMPTY = 2;

    private int mModule;
    private String mModuleName;
    private String mFixedTag;
    private LogAdapter mAdapter;
    private boolean isLoaded = false;

    private View mLoadingLayout;
    private View mEmptyLayout;
    private SpringBackLayout mSpringBackLayout;
    private RecyclerView mRecyclerView;
    private TextView mEmptyText;
    private ImageView mEmptyIcon;

    private String mCurrentKeyword = "";
    private String mCurrentLevel = LogLevelFilter.ALL.getValue();
    private String mCurrentTag = ALL_TAG_VALUE;

    private PullViewHelper mPullViewHelper;
    private boolean mHasRootPermission = false;
    private boolean mHasAnyModuleLogs = false;
    /** Record requested by a notification, or 0 for the plain list. */
    private long mTargetTimestamp;
    private boolean mTargetOpened;

    public static LogListFragment newInstance(int module) {
        return newInstance(module, null, null);
    }

    public static LogListFragment newInstance(int module, String fixedTag, String moduleName) {
        return newInstance(module, fixedTag, moduleName, 0L);
    }

    /**
     * @param targetTimestamp record to show on arrival, or 0; used when a notification asks for one
     *                        specific run instead of the whole list
     */
    public static LogListFragment newInstance(int module, String fixedTag, String moduleName,
                                              long targetTimestamp) {
        LogListFragment fragment = new LogListFragment();
        Bundle args = new Bundle();
        args.putInt("module", module);
        args.putString("fixed_tag", fixedTag);
        args.putString("module_name", moduleName);
        args.putLong("target_ts", targetTimestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_list, container, false);
        mModule = getArguments().getInt("module", 0);
        mFixedTag = getArguments().getString("fixed_tag");
        mModuleName = getArguments().getString("module_name");
        mTargetTimestamp = getArguments().getLong("target_ts", 0L);

        mSpringBackLayout = view.findViewById(R.id.spring_back);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mLoadingLayout = view.findViewById(R.id.loading_layout);
        mEmptyLayout = view.findViewById(R.id.empty_layout);
        mEmptyText = view.findViewById(R.id.empty_text);
        mEmptyIcon = view.findViewById(R.id.empty_icon);

        mAdapter = new LogAdapter(requireContext(), this::showLogDetail);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.addItemDecoration(new CardItemDecoration(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

        mPullViewHelper = new PullViewHelper(requireContext(), new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false, true, true);
            }

            @Override
            public void onLoadMore() {
                loadMore();
            }

            @Override
            public void onEnterPrivate() {
            }
        });
        mPullViewHelper.attachSpringBackLayout(mSpringBackLayout);
        mPullViewHelper.setEnablePullRefresh(true);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        maybeLoadInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) forceRefresh();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            maybeLoadInitialData();
        }
    }

    public void applyFilter(String keyword, String level, String tag) {
        if (mCurrentKeyword.equals(keyword) && mCurrentLevel.equals(level) && mCurrentTag.equals(tag)) {
            return;
        }
        mCurrentKeyword = keyword;
        mCurrentLevel = level;
        mCurrentTag = tag;
        if (getView() == null) return;
        // Every keystroke used to start its own query; a slower earlier one could land last.
        if (mPendingFilter != null) mSearchHandler.removeCallbacks(mPendingFilter);
        mPendingFilter = () -> {
            mPendingFilter = null;
            if (getView() != null) refreshData(false, false, false);
        };
        mSearchHandler.postDelayed(mPendingFilter, 250L);
    }

    public void forceRefresh() {
        if (getView() != null) {
            refreshData(true, false, true);
        }
    }

    private void maybeLoadInitialData() {
        if (getView() == null || !getUserVisibleHint() || isLoaded) {
            return;
        }
        forceRefresh();
        isLoaded = true;
    }

    private void refreshData() {
        refreshData(true, false, false);
    }

    /** Rows per page; the list appends on demand instead of stopping at one capped query. */
    private static final int PAGE_SIZE = 300;
    /** Rows currently shown, in query order, so a page can be appended to them. */
    private final List<LogEntry> mLoaded = new ArrayList<>();
    /** Bumped for every query; only the newest result may be applied. */
    private final java.util.concurrent.atomic.AtomicInteger mRefreshGeneration =
            new java.util.concurrent.atomic.AtomicInteger();
    private volatile boolean mLoadingMore;
    /** How often the notification deep link has tried to find its record. */
    private int mTargetAttempts;
    private final android.os.Handler mSearchHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable mPendingFilter;

    private void refreshData(boolean showLoading, boolean fromPullRefresh, boolean syncBeforeQuery) {
        if (showLoading) {
            showState(STATE_LOADING);
        }

        final Context context = getContext();
        final String module = mModuleName != null ? mModuleName : (mModule == 0 ? "App" : "Xposed");
        final String keyword = mCurrentKeyword;
        final String level = mCurrentLevel;
        final String tag = mFixedTag != null ? mFixedTag : mCurrentTag;
        final boolean isFiltering = !keyword.isEmpty() || !LogLevelFilter.isAll(level) || !ALL_TAG_VALUE.equals(tag);

        ThreadUtils.runOnBackground(() -> {
            boolean hasRootPermission;
            if (("App".equals(module) || "Script".equals(module)) && context != null) {
                AlarmLogImporter.importNew(context.getApplicationContext());
            }
            if (mModule == 1 || syncBeforeQuery) {
                hasRootPermission = LogSyncDiagnostics.checkRoot(LogRepository.getInstance().getDao());
                if (syncBeforeQuery && context != null) {
                    // The loader persists denied Root access too; never silently skip collection.
                    XposedLogLoader.syncLogsToDatabaseSync(context.getApplicationContext());
                }
            } else {
                hasRootPermission = false;
            }

            LogDao dao = LogRepository.getInstance().getDao();
            final int generation = mRefreshGeneration.incrementAndGet();
            List<LogEntry> logs = dao.queryLogsPage(module, level, tag, keyword, PAGE_SIZE, 0);
            boolean hasAnyModuleLogs = isFiltering
                ? !dao.queryLogs(module, LogLevelFilter.ALL.getValue(), ALL_TAG_VALUE, "").isEmpty()
                : !logs.isEmpty();

            if (getActivity() != null) {
                final List<LogEntry> page = logs;
                getActivity().runOnUiThread(() -> {
                    if (generation != mRefreshGeneration.get()) return;
                    mHasRootPermission = hasRootPermission;
                    mHasAnyModuleLogs = hasAnyModuleLogs;
                    mLoaded.clear();
                    mLoaded.addAll(page);
                    mAdapter.updateData(new ArrayList<>(mLoaded), keyword);
                    showState(page.isEmpty() ? STATE_EMPTY : STATE_CONTENT);
                    openTargetRecord(page);
                    if (fromPullRefresh) {
                        mPullViewHelper.onPullRefreshComplete();
                    }
                });
            }
        });
    }

    /**
     * Appends the next page.
     *
     * <p>The list used to be a single capped query, so everything older than the newest page stayed
     * in the database and was unreachable from this screen.</p>
     */
    private void loadMore() {
        if (mLoadingMore) return;
        final Context context = getContext();
        if (context == null) return;
        final int offset = mLoaded.size();
        if (offset == 0) return;
        final String module = mModuleName != null ? mModuleName : (mModule == 0 ? "App" : "Xposed");
        final String keyword = mCurrentKeyword;
        final String level = mCurrentLevel;
        final String tag = mFixedTag != null ? mFixedTag : mCurrentTag;
        final int generation = mRefreshGeneration.get();
        mLoadingMore = true;
        ThreadUtils.runOnBackground(() -> {
            List<LogEntry> page;
            try {
                page = LogRepository.getInstance().getDao()
                        .queryLogsPage(module, level, tag, keyword, PAGE_SIZE, offset);
            } catch (Throwable t) {
                page = new ArrayList<>();
            }
            final List<LogEntry> more = page;
            if (getActivity() == null) {
                mLoadingMore = false;
                return;
            }
            getActivity().runOnUiThread(() -> {
                mLoadingMore = false;
                if (generation != mRefreshGeneration.get()) return;   // a new query already took over
                if (!more.isEmpty()) {
                    mLoaded.addAll(more);
                    mAdapter.updateData(new ArrayList<>(mLoaded), keyword);
                }
                mPullViewHelper.onPullRefreshComplete();
            });
        });
    }

    /**
     * Opens the record a notification pointed at, once the list that contains it is on screen.
     *
     * <p>Only the first load does this: refreshing the list afterwards must not resurrect a sheet
     * the user already closed.</p>
     */
    private void openTargetRecord(List<LogEntry> logs) {
        if (mTargetTimestamp == 0L || mTargetOpened) return;
        // Mark it opened only once the record is actually found: the target may not be imported yet,
        // and giving up on the first load made the notification tap open nothing at all.
        for (LogEntry entry : logs) {
            if (entry.getTimestamp() == mTargetTimestamp) {
                mTargetOpened = true;
                showLogDetail(entry);
                return;
            }
        }
        // Not on this page: the list is paged, so ask the database instead of scrolling through it.
        if (++mTargetAttempts > 3) {
            mTargetOpened = true;
            return;
        }
        final String module = mModuleName != null ? mModuleName : (mModule == 0 ? "App" : "Xposed");
        final long target = mTargetTimestamp;
        ThreadUtils.runOnBackground(() -> {
            LogEntry hit = null;
            try {
                hit = LogRepository.getInstance().getDao().queryByTimestamp(module, target);
            } catch (Throwable t) {
                AndroidLog.w("LogListFragment", "target lookup failed: " + t);
            }
            if (hit == null || getActivity() == null) return;
            final LogEntry found = hit;
            getActivity().runOnUiThread(() -> {
                if (mTargetOpened) return;
                mTargetOpened = true;
                showLogDetail(found);
            });
        });
    }

    private void showLogDetail(LogEntry entry) {
        if (getActivity() == null) {
            return;
        }
        LogDetailBottomSheet detailBottomSheet = new LogDetailBottomSheet(requireActivity());
        detailBottomSheet.showRecord(entry);
    }

    private void showState(int state) {
        mLoadingLayout.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(state == STATE_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyLayout.setVisibility(state == STATE_EMPTY ? View.VISIBLE : View.GONE);

        if (state == STATE_EMPTY) {
            boolean isFiltering = !mCurrentKeyword.isEmpty() || !LogLevelFilter.isAll(mCurrentLevel) ||
                !ALL_TAG_VALUE.equals(mCurrentTag);

            if (mModule == 1 && !mHasRootPermission && !mHasAnyModuleLogs) {
                mEmptyText.setText(R.string.log_empty_root_required);
                mEmptyIcon.setImageResource(R.drawable.ic_empty);
            } else if (isFiltering) {
                mEmptyText.setText(R.string.log_empty_no_results);
                mEmptyIcon.setImageResource(R.drawable.search_list_empty);
            } else {
                mEmptyText.setText(R.string.log_empty_no_logs);
                mEmptyIcon.setImageResource(R.drawable.ic_empty);
            }
        }
    }
}
