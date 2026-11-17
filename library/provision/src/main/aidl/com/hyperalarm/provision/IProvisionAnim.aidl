package com.hyperalarm.provision;

import com.hyperalarm.provision.IAnimCallback;

interface IProvisionAnim {

    boolean isAnimEnd();

    void playBackAnim(int animY);
    void playNextAnim(int i);

    void registerRemoteCallback(IAnimCallback callback);
    void unregisterRemoteCallback(IAnimCallback callback);
}
