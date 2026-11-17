package com.hyperalarm.home.task;

import static com.hyperalarm.utils.NoticeProcessor.isNeedShowTosDialog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hyperalarm.common.utils.PrefsBridge;

import com.hyperalarm.home.manager.PageDecorator;
import com.hyperalarm.libhook.utils.pkg.CheckModifyUtils;
import com.hyperalarm.ui.HomePageActivity;
import com.hyperalarm.utils.LSPosedScopeHelper;
import com.hyperalarm.utils.LanguageHelper;
import com.hyperalarm.utils.LogServiceUtils;
import com.hyperalarm.utils.NoticeProcessor;

import java.util.List;

import io.github.libxposed.service.XposedServiceHelper;

/**
 * 业务管理类：定义任务图及原本属于 Activity 的计算逻辑
 */
public class AppTaskManager {

    private static final List<String> CHECK_LIST = List.of(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    );

    public static void attach(Context context) {
        TaskRunner runner = TaskRunner.getInstance();

        runner.addTask(new Task("attachBaseContext", false) { // 主线程同步执行
            @Override
            public void execute() {
                if (context instanceof XposedServiceHelper.OnServiceListener listener) {
                    XposedServiceHelper.registerListener(listener);
                }
                PrefsBridge.initForApp(context);
            }
        });
    }

    /**
     * 第一阶段任务定义：Application 级别
     */
    public static void setupAppTasks(Context context) {
        TaskRunner runner = TaskRunner.getInstance();

        runner.addTask(new Task("LanguageInit", false) { // 主线程同步执行
            @Override
            public void execute() {
                // 提前初始化语言，保证首屏渲染就是正确的语言
                LanguageHelper.init(context);
            }
        });

        runner.addTask(new Task("BaseEnv", false) {
            @Override
            public void execute() {
                LSPosedScopeHelper.init();
            }
        });
    }

    /**
     * 第二阶段任务定义：Activity 级别
     */
    public static void setupActivityTasks(HomePageActivity activity, Context context) {
        TaskRunner runner = TaskRunner.getInstance();

        // UI 装饰任务：必须在主线程执行 (isAsync = false)
        runner.addTask(new Task("UI_Theme", false) {
            @Override
            public void execute() {
                // 调用装饰器：处理滤镜和节日特效
                PageDecorator.decorate(activity);
                LogServiceUtils.init(activity);


            }
        });

        // 异步业务逻辑：初始化 Shell、执行签名校验
        runner.addTask(new Task("BusinessLogic", true, "BaseEnv", "attachBaseContext") {
            @Override
            public void execute() {
                // 原 Activity 的 checkAppMod 逻辑迁移到此
                CHECK_LIST.parallelStream().forEach(pkg -> {
                    checkAppMod(activity, pkg);
                });
            }
        });

    }

    private static void checkAppMod(Context context, String pkg) {
        boolean check = CheckModifyUtils.INSTANCE.isApkModified(context, pkg, CheckModifyUtils.XIAOMI_SIGNATURE);
        CheckModifyUtils.INSTANCE.setCheckResult(pkg, check);
    }
}
