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
package com.hyperalarm.provision.fragment;

import static com.hyperalarm.provision.utils.NetworkManager.isInternetAvailable;
import static com.hyperalarm.provision.utils.NetworkManager.isNetworkConnected;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyperalarm.common.utils.ThreadUtils;
import com.hyperalarm.common.utils.PermissionUtils;
import com.hyperalarm.provision.R;
import com.hyperalarm.provision.widget.PermissionItemView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fan.provision.OobeUtils;
import com.hyperalarm.common.utils.RootShell;

public class PermissionSettingsFragment extends BaseFragment {
    private static final int REQUEST_GET_INSTALLED_APPS = 1101;

    private View mNextView;

    PermissionItemView mRootPermissionItem;
    PermissionItemView mNetworkPermissionItem;
    PermissionItemView mLspPermissionItem;


    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private long lastNetworkCheck = 0;

    public static boolean isModuleActive = false;

    @Override
    protected int getLayoutId() {
        return R.layout.provision_permission_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootPermissionItem = view.findViewById(R.id.root);
        mNetworkPermissionItem = view.findViewById(R.id.network);
        mLspPermissionItem = view.findViewById(R.id.lsp);

        mRootPermissionItem.setItemTitle(R.string.provision_permission_root);
        mNetworkPermissionItem.setItemTitle(R.string.provision_permission_internet);
        mLspPermissionItem.setItemTitle(R.string.provision_permission_lsp);

        mNetworkPermissionItem.setEnabled(false);
        mRootPermissionItem.setEnabled(false);
        mLspPermissionItem.setEnabled(false);

        checkNetwork();
        checkRooted();
        checkLsp();
        registerNetworkCallback();
    }

    private void checkRooted() {
        ThreadUtils.runOnBackground(() -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (mRootPermissionItem != null) {
                    mRootPermissionItem.setChecked(isDeviceRooted());
                }
            });
        });
    }

    private void checkLsp() {
        ThreadUtils.runOnBackground(() -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (mLspPermissionItem != null) {
                    mLspPermissionItem.setChecked(isModuleActive);
                }
            });
        });
    }

    private void checkNetwork() {
        ThreadUtils.runOnBackground(() -> {
            boolean connected = isNetworkConnected(requireContext());
            boolean internet = connected && isInternetAvailable();

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                mNetworkPermissionItem.setChecked(internet);
                setAllowNext(internet);
            });
        });
    }

    private void updateNetworkState(boolean state) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            mNetworkPermissionItem.setChecked(state);
            setAllowNext(state);
        });
    }

    private void setAllowNext(boolean allowNext) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            mNextView = OobeUtils.getNextView(getActivity());
            mNextView.setEnabled(allowNext);
            mNextView.setAlpha(allowNext ? OobeUtils.NO_ALPHA : OobeUtils.HALF_ALPHA);
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        checkNetwork();
        checkRooted();
        checkLsp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterNetworkCallback();
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager)
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                checkNetworkDebounced();
            }

            @Override
            public void onLost(@NonNull Network network) {
                updateNetworkState(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    updateNetworkState(false);
                } else {
                    checkNetworkDebounced();
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }

    private void checkNetworkDebounced() {
        long now = System.currentTimeMillis();
        if (now - lastNetworkCheck < 1500) return;
        lastNetworkCheck = now;
        checkNetwork();
    }

    private boolean isDeviceRooted() {
        String[] paths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        // The shell check above is a heuristic; ask the shared root shell for the real answer.
        return com.hyperalarm.common.utils.RootShell.isRoot();
    }




}
