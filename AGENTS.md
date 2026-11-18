# HyperAlarm — AGENTS.md

本文件记录本项目的架构、开发约定、实机调试经验和发布前注意事项。  
面向后续接手的编码 Agent，目标是少踩坑、少做重复逆向。

---

## 1. 项目一句话

HyperAlarm 是一个基于 HyperCeiler 原生 MIUIX 代码 fork 的 Xposed 模块：

- 用户可见名称：`HyperAlarm`
- 包名（applicationId）：`io.github.zoeicx.hyperalarm`
- 代码命名空间：`com.hyperalarm`（Java/Kotlin package 与 `namespace` 都没动，只换了 applicationId）
  - 为什么不是 `com.hyperalarm`：LSPosed 模块仓库（`Xposed-Modules-Repo`）的机器人要求包名 **≥3 段**，
    2 段会被直接判 invalid；`io.github.<用户名>` 前缀还能走自动过审通道
- 唯一 Hook 作用域：`com.android.deskclock`（小米时钟）
- 核心能力：在小米时钟的闹钟触发时执行 `/data/adb/alarm_shell/scripts` 下的 root 脚本
- HyperAlarm 自己的主页 1:1 复刻小米时钟闹钟列表
- 闹钟的新增/删除/修改/查询优先通过时钟进程里的 Xposed 广播桥完成
- root 只用于执行脚本、读写脚本目录、同步日志
- 不在小米时钟内做大规模 UI Hook；例外只有右上角菜单入口和可选的“隐藏脚本任务”列表过滤

---

## 2. 代码根目录

即本仓库根目录，下文所有命令默认以仓库根为当前工作目录。

构建产物：

```text
app/build/outputs/apk/release/
```

注意：仓库所在路径必须保持 ASCII。中文路径会导致 AIDL / AGP 出现 `MalformedInputException`。

---

## 3. 构建环境

### JDK

需要 JDK 25，通过 `JAVA_HOME` 指向本机安装位置。

### 构建命令

```bash
GIT_ACTOR=<你的 GitHub 用户名> \
GIT_TOKEN=$(gh auth token) \
./gradlew :app:assembleRelease -x lint --max-workers=1
```

`GIT_ACTOR` / `GIT_TOKEN` 用于拉取 GitHub Packages 上的 `fan.miuix:*` 依赖。  
`GIT_TOKEN` 需要 `read:packages` 权限。二者的读取逻辑见 `settings.gradle.kts`。

### 常见构建问题

- `mergeDexRelease: 另一个程序正在使用此文件`
  - 先执行 `./gradlew --stop`，再重新构建。
- 构建很慢或 OOM
  - 保持 `--max-workers=1`。
- release 没有配置签名时
  - `:app:assembleRelease` / `bundleRelease` / `packageRelease` 在 task graph 阶段直接抛异常，
    lint 与 `checkI18n` 仍可运行；本地想跑通打包可加显式逃生口 `-PallowDebugSigning`。
  - 判据是 `signing.properties` 里有非空的 `storeFile`（只写 `gpr.user` / `gpr.key` 不算）。
  - 正式发布必须配置 `signing.properties`，见第 14 节。
- APK 安装后不生效
  - 先确认 LSPosed 里模块已启用、作用域只有 `com.android.deskclock`。
  - 再强制停止时钟并重新启动进程。
  - **卸载重装（尤其是换了签名）会让 LSPosed 丢掉这个模块的绑定**：日志里只剩
    `Failed to find package info of io.github.zoeicx.hyperalarm`，时钟进程里没有任何 `hooks installed`。
    必须回 LSPosed 管理器重新启用模块并重勾作用域，然后重启时钟进程。

---

## 4. 设备与运行环境

验证环境（任何等价设备均可，字段是兼容性基准）：

```text
Android 16 / SDK 36
HyperOS 3.x
KernelSU / Magisk root
LSPosed / Xposed API 102
Xiaomi Clock 18.28.0
```

约束：

- 不要随意重启设备。
- 不要自动重新打开小米时钟 UI。
- 需要重启时钟进程时只执行：
  ```bash
  adb shell am force-stop com.android.deskclock
  ```
- 重新拉起时钟进程优先用：
  ```bash
  adb shell am startservice -n com.android.deskclock/com.xiaomi.smarthome.ClockService
  ```
- `adb root` 在 production build 上通常不可用；shell uid 一般是 2000。
- 中文路径 push/pull 时注意 shell 的路径参数转换（Windows + MSYS2 下需关闭自动转换）。
- 截图：
  ```bash
  adb shell screencap -p /data/local/tmp/x.png
  adb pull /data/local/tmp/x.png ...
  ```

---

## 5. 目录结构

```text
app/
  src/main/java/com/hyperalarm/
    home/                 主页、闹钟列表、详情 BottomSheet、铃声、脚本
    script/               脚本目录、SAF DocumentsProvider
    log/                  应用日志 / 脚本日志 UI
    settings/             设置页
    ui/                   Splash、HomePageActivity
  src/main/res/
    layout/               主页 / 详情 / 重复 / 轮班布局
    drawable/             卡片、BottomSheet、图标
    xml/prefs_settings.xml 设置项

library/
  libhook/
    com/hyperalarm/alarm/ModernInit.java
    com/hyperalarm/alarm/hook/AlarmHooks.java
    com/hyperalarm/alarm/hook/ShiftBridge.java
    com/hyperalarm/alarm/hook/HookAlarmQuery.java
    com/hyperalarm/alarm/core/TaskManager.java
    com/hyperalarm/alarm/core/ScriptPath.java
    com/hyperalarm/alarm/core/SettingsKeys.java
    com/hyperalarm/alarm/core/UnlockHelper.java
  common/                 PrefsBridge、AppSettingsStore、日志、RootShell（utils/RootShell.java）
  core/
  provision/
```

---

## 6. Xposed 入口

现代 Xposed API 入口：

```text
library/libhook/src/main/resources/META-INF/xposed/java_init.list
```

内容：

```text
com.hyperalarm.alarm.ModernInit
```

`ModernInit` 只在：

```text
com.android.deskclock
```

进程加载。

静态作用域：

```text
META-INF/xposed/scope.list -> com.android.deskclock
```

`module.prop`：

```properties
minApiVersion=101
targetApiVersion=102
autoHotReload=false
staticScope=true
```

`autoHotReload=false` 是有意的：热重载会给时钟换一代模块 classloader，而 `installed` / `registered` /
`alarmChangeObserver` 这些幂等守卫守护的是进程级对象（钩子、广播接收器、ContentObserver），换代后
守卫归零就会重复安装 → 每条桥广播执行两次。守卫本身也改成了 `System.setProperty` 跨代存活，但更新
模块后仍然要求重启时钟进程（设置页有「重启小米时钟」）。

不要改回动态作用域，也不要加其他包。

---

## 7. 时钟进程内 Hook 架构

### 7.1 AlarmHooks

`AlarmHooks.install(ClassLoader)` 做这些事：

1. Hook `com.android.deskclock.DeskClockApp.onCreate`
   - 初始化通知渠道
   - 注册 `ShiftBridge` 广播接收器
2. Hook `com.android.deskclock.alarm.alert.AlarmService.handleAlarm`
   - 识别标签为 `.sh` 的闹钟
   - 执行脚本
3. Hook `com.android.deskclock.TabNavigatorContentFragment.onCreateOptionsMenu`
   - 在时钟右上角菜单加入 `HyperAlarm` 入口
4. Hook `com.android.deskclock.alarm.AlarmAdapter.initData`
   - 根据全局设置过滤脚本任务
5. 注册 `AlarmRingtonePickerActivity.finish`，回传铃声选择结果

### 7.2 脚本闹钟标签约定

```text
script.sh      静默执行脚本
@script.sh     执行脚本，同时保留闹钟 UI / 铃声
```

`@` 前缀是“铃声一级开关”的持久化方式，不要另外引入数据库字段。

执行时：

- 标签去掉 `@` 后作为脚本名
- 用户可在 HyperAlarm 详情页切换“铃声”一级开关
- 列表显示时自动隐藏 `@`

### 7.3 AlarmService 处理逻辑

`AlarmHooks.onHandleAlarm`：

- 从参数中反射找 `com.android.deskclock.Alarm`
- 读取 `label`
- 只处理以 `.sh` 结尾的标签
- 非 `@` 任务：
  - `param.setResult(null)` 抑制闹钟 UI
  - 重新调度下一次闹钟
  - 异步执行脚本
- `@` 任务：
  - 保留原闹钟流程
  - 并行执行脚本

修改这里时务必确认：

- 一次性闹钟会正确删除/重排
- 重复闹钟不会因为抑制 UI 而停止下一次触发
- 脚本执行失败不会把时钟进程打崩

---

## 7.4 已在实机验证的行为（2026-11-09）

- 08:00 的真实闹钟（`valorant.sh`）触发后脚本以 root 执行，`exitCode=0`，日志镜像到
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/valorant.sh/`，重复闹钟重排到次日同时间
- 受控测试闹钟（+2 分钟、标签 `itest.sh`）准点触发，脚本内 `id` 输出为 `uid=0(root)`，
  证明走的是 `nsenter -t 1 -m -- sh`（时钟进程自身是 u0_a284）
- 触发后一次性脚本闹钟会从 `alarm` 表删除：`reschedule()` 调完 `enableAlarm(false)` 与
  `tryDeleteOneshotAlarm` 后再显式 delete 一次（时钟自己只在下次刷新时清，而抑制 UI 的
  路径永远等不到那次刷新），日志里会出现 `[AlarmHooks]: one-shot alarm removed: <id>`
- 「隐藏脚本任务」用的全局键是 `hyperalarm_hide_script_alarms`，只影响列表显示，不影响调度

## 8. 广播桥

常量在 `ShiftBridge`：

```text
com.hyperalarm.action.CREATE_SHIFT
com.hyperalarm.action.ADD_ALARM
com.hyperalarm.action.DELETE_SHIFT
com.hyperalarm.action.RUN_SCRIPT
com.hyperalarm.action.QUERY_ALARMS
com.hyperalarm.action.UPDATE_ALARM
com.hyperalarm.action.DELETE_ALARM
com.hyperalarm.action.SET_ENABLED
com.hyperalarm.action.SET_RINGTONE
com.hyperalarm.action.OPEN_RINGTONE
com.hyperalarm.action.RINGTONE_PICKED
```

### 8.1 权限

广播桥必须使用签名权限：

```text
hyperalarm.permission.BRIDGE
```

- Manifest 中定义 `protectionLevel="signature"`
- App 声明 `uses-permission`
- `ShiftBridge.registerReceiver` 注册时带该权限（**这是唯一的强制点**：系统会校验「发送方持有 BRIDGE」）
- App 发送时**不要**再传接收方权限：
  - `sendBroadcast(intent)`
  - `sendOrderedBroadcast(intent, null, ...)`

为什么：`sendBroadcast(intent, permission)` 的语义是「**接收方**必须持有该权限」。BRIDGE 是本模块的
signature 权限，小米时钟签名不同、永远拿不到它，于是所有桥广播会被系统直接丢弃
（`dumpsys activity broadcasts` 里是 `SKIPPED ... Permission Denial ... requires hyperalarm.permission.BRIDGE`）。
表现就是：列表还能显示（查询失败后回退 root `content query`），但新增/编辑/删除/开关/铃声/轮班全部静默失效。

权限之外还有第二层：`ShiftBridge` 的接收器用 `getSentFromUid()` 校验发送方 uid 是否等于
`BridgeContract.MODULE_PACKAGE`（自定义权限归先声明者，先装的恶意应用可以把它声明成 normal）。两层都不要去掉。

没有这个权限，任意应用都能给时钟发广播，进而以 root 执行任意脚本。

### 8.1.1 数据契约

闹钟行的字段名、provider 列名、URI、`type`/`days` 常量集中在：

```text
library/common/src/main/java/com/hyperalarm/common/alarm/AlarmContract.java
library/common/src/main/java/com/hyperalarm/common/alarm/AlarmItem.java
```

- 时钟进程用 Gson 把 `List<AlarmItem>` 序列化成 JSON（`HookAlarmQuery`），app 用 Gson 反序列化
  （app 侧是 `home/ClockBridge` 的 `parse()`）——**字段名就是线上格式**，改字段等于改协议，
  必须两边同时改
- root 兜底解析 `content query` 也用 `AlarmContract.ALARM_PROJECTION` / `COL_*`，不要再手写列名
- 轮班摘要（`N天M班`）不再由时钟进程拼字符串，而是由 app 用 `R.string.shift_info` 渲染
  （`ShiftSummary.of()`），避免把中文塞进跨进程数据

### 8.2 查询流程

app 侧 `ClockBridge`：

1. `startService(ClockService)` 拉起时钟进程但不打开 UI
2. 等待约 250ms
3. 发 ordered broadcast `QUERY_ALARMS`
4. 250ms 内没结果再重试一次（+300ms）
5. 时钟进程通过 `HookAlarmQuery.query(context)` 返回 JSON
6. 失败时回退到 root `content query`

### 8.3 写入流程

`AlarmHookWriter`：

- 先 `startService(ClockService)`
- 延迟约 220ms 发送写广播
- 所有写入异步，不阻塞 UI

写入动作：

- `UPDATE_ALARM`
- `DELETE_ALARM`
- `SET_ENABLED`
- `SET_RINGTONE`
- `ADD_ALARM`
- `CREATE_SHIFT`
- `DELETE_SHIFT`
- `RUN_SCRIPT`

---

## 9. 小米时钟 18.28 数据模型

不要按旧版本字段写：

```text
message       -> 闹钟标签（不是 label）
daysofweek    -> 重复位掩码
alert         -> 铃声 URI
enabled       -> 是否启用
hour/minutes  -> 时间
```

Provider：

```text
content://com.android.deskclock/alarm
content://com.android.deskclock/shift_alarms
```

轮班制：

- `shift_alarms` 中 `type=1` 是轮班组
- `type=0` 是具体班次
- 当前班次由 `starttime` / `duration` 推算
- 轮班写入必须通过 `ShiftBridge` 在时钟进程内反射：
  - `ShiftAlarmGroup.getDefault()`
  - `resetDuration`
  - `insertShiftAlarmGroup`
  - `updateShiftAlarmGroup`
  - `ShiftAlarmDataHelper.resetCache`
  - `AlarmHelper.setNextAlert`

不要试图在外面直接写 `shift_alarms` 表。

---

## 10. 脚本执行

### 10.1 路径

```text
/data/adb/alarm_shell/scripts
/data/adb/alarm_shell/password
/data/adb/alarm_shell/unlock_phone
```

保持这些兼容路径，不要改。

### 10.2 执行方式

时钟进程里的 root shell 与外层 mount namespace 不同，看不到其他应用的数据目录。  
所有脚本必须通过：

```text
/system/bin/nsenter -t 1 -m -- sh <script>
```

执行。

### 10.3 日志

时钟进程内：

```text
/data/data/com.android.deskclock/cache/log/<script>/yyyyMMddHHmmss-<alarmId>-<nano>.log
```

同时镜像到：

```text
/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<script>/
```

权限（脚本 cache 目录由时钟进程自己占有；镜像目录见 §10.3.1）：

```text
镜像目录 700 / 镜像文件 600，属主 chown 给 HyperAlarm
```

镜像文件供非 root 的 HyperAlarm 日志导入器读取。

### 10.3.1 日志镜像权限

`/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<script>/` 由 root 写入后立刻
`chown -R <app uid>:<app gid>`（取自 `files` 目录的属主）+ 目录 `700` / 文件 `600`：
只有 HyperAlarm 自己能读（它的日志导入器），其它应用读不到。root 与 adb shell 仍可读，
这是外部存储的固有限制，需真正保密时不要把密钥写进脚本输出。

`/data/adb/alarm_shell/password` 由 `ensureSetup` 显式 `chmod 600`。注意它仍以 `$1` 参数
交给 `unlock_phone`（为兼容既有用户脚本），因此会短暂出现在该子进程的 argv 里——能读
`/proc` 的只有 root，属可接受残留；若要彻底消除需同时改默认脚本读取 stdin。

### 10.4 脚本坑

已经踩过的：

- `USER_ID` 在 mksh 中是整数环境变量，脚本里如冲突需 `unset USER_ID`
- toybox grep 不支持 `\{0,1\}`，用 `[0-9-]*` 之类写法
- Windows 换行会导致脚本失败，必须是 LF
- 某些应用数据目录只有 nsenter 后的 mount namespace 可见
- `RUN_SCRIPT` 只允许执行 `/data/adb/alarm_shell/scripts/` 下的脚本，已做 canonical path 校验

---

## 11. UI 约定

### 11.1 主页

- `HyperAlarmHomeFragment extends fan.appcompat.app.Fragment`
- 复用 `fragment_settings` 的 `R.id.prefs_container`
- 子页面是 `AlarmListFragment extends BasePreferenceFragment`
- 通过动态 `PreferenceScreen` 构建列表
- 不要改成普通 RecyclerView / Compose，否则很难保持 MIUIX header 折叠和滚动行为

列表卡片：

- `AlarmPreference`
- `item_alarm_clock.xml`
- 卡片高度 `65dp`
- 圆角 `20dp`
- 背景 `#242424`
- 时间 `32sp`
- 摘要 `12sp`，格式：`重复 | 脚本名`
- 开关使用 `fan.slidingwidget.widget.SlidingButton`
- 开关和复选框统一 `translationY=5dp`
- 时间容器 `translationY=3dp`
- 多选时切换为 `CheckBox clock_select`，整项高亮 `#4A4A4A`

刷新策略（`AlarmListFragment`）：

- 数据只能经桥取：app 直接读时钟 provider 会被 `miui.permission.ACCESS_ALARM_PROVIDER` 拒绝
  （`registerContentObserver` 抛 SecurityException），所以 **观察者必须挂在时钟进程**
- 时钟进程在 `DeskClockApp.onCreate` 里 `observeAlarms(app)` 监听 `alarm` / `shift_alarms`，
  防抖 300ms 后广播 `BridgeContract.ACTION_ALARMS_CHANGED`（`setPackage(BridgeContract.MODULE_PACKAGE)`）
- app 侧在 `onResume` 注册运行时接收器（**必须 EXPORTED**，发送方是时钟这个别的应用；伪造该广播
  只能触发一次刷新，刷新本身仍走带 BRIDGE 权限的查询），防抖 400ms → `invalidate()` + 重查
- 自己写入后 2 秒内的广播用 `ClockBridge.lastWriteAt()` 抑制，避免一次写查两遍

顶部下一任务：

- `NextAlarmPreference`
- `item_alarm_next.xml`
- 两行：
  ```text
  X小时X分钟后
  执行 script.sh 任务
  ```
- 脚本名过长自动缩小字号
- 倒计时是相对时间，必须自己走：`NextAlarmPreference.setTextSupplier(...)` 在 attach 期间按整分钟
  边界 `postDelayed` 重算（detach / `stopTicking()` 时移除），不要只在 `rebuildPreferences()` 里
  setText 一次，否则「10小时12分钟后」会停在建列表那一刻

### 11.2 详情 BottomSheet

`AlarmDetailBottomSheet extends fan.bottomsheet.BottomSheetModal`

布局：

```text
view_alarm_detail_sheet.xml
```

卡片顺序：

```text
铃声（一级开关）
选择铃声
响铃时振动
重复
脚本
立即执行
```

“铃声”一级开关：

- 关闭：隐藏“选择铃声”和“响铃时振动”，脚本静默执行
- 打开：显示这两个选项，脚本执行时保留闹钟 UI
- 显示/隐藏动画：淡入 + 上下 8dp 滑动，180ms / 150ms
- 持久化使用标签 `@` 前缀

时间轮：

- `AlarmTimeWheelView`
- 独立 `hourOffset` / `minuteOffset`
- 无限滚动
- 5 个可见槽位
- `OverScroller` fling
- 字体使用系统 `Typeface.DEFAULT` / `DEFAULT_BOLD`
- 不要重新引入 MitypeMono / MiSans 字体

重复：

- `AlarmRepeatText.get(days)`
- `0=只响一次`
- `127=每天`
- `128=法定工作日`
- `256=法定节假日`
- `512=轮班制`
- `31/62=周一至周五`
- `96=周末`
- 自定义位：Monday=bit0

### 11.3 多选

- 长按进入
- 返回键退出
- 取消全部选中仍保持多选模式
- 删除通过 `DELETE_ALARM` / `DELETE_SHIFT`
- 浮动导航栏隐藏，中央删除 FAB 出现

---

## 12. 设置页

`app/src/main/res/xml/prefs_settings.xml`

现有重要设置：

```text
显示所有闹钟
时钟列表只显示真正的闹钟
日志输出等级
应用日志
脚本日志
复制脚本目录
```

“时钟列表只显示真正的闹钟”：

- 存储：**只有**系统全局设置 `hyperalarm_hide_script_alarms`（键常量见
  `library/libhook/.../alarm/core/SettingsKeys.java`），不要再镜像进模块 prefs / provider，
  否则 root 写入失败时会出现「设置显示已开但时钟不生效」
- App 读：`Settings.Global.getInt(...)`（普通应用即可读，无需 root）
- App 写：root `settings put global hyperalarm_hide_script_alarms 0|1`；无 root 时开关回弹并提示
- Hook 在 `AlarmAdapter.initData` 读取同一个 key
- 只过滤标签以 `.sh` 结尾的 `AlarmBean`
- 开关打开时，时钟主页“X小时X分钟后响铃”也会跳过脚本任务：`AlarmHooks` 在
  `AlarmClockFragment.updateNextAlertTime` 的 after 里把文案改成“下一个真实闹钟”的倒计时
  （更早的脚本任务被跳过；没有真实闹钟时隐藏该行）。**只改文案**：
  `AlarmHelper.setNextAlert` / `calculateNextAlert` / `AlarmStateManager` 仍然看全量闹钟，
  脚本任务照常触发，绝不能在调度路径上过滤
- 轮班制也参与比较：`ShiftAlarmDataHelper.getShowAlarms()` 返回每个轮班组已经算好 `time` 的
  `Alarm`，与普通闹钟取最早的一个。不要去复刻轮班的轮转逻辑——那是时钟自己的实现，升级就会漂
- 需要 root 写入；无 root 时设置页会提示
- 切换后重新进入时钟闹钟页或重启时钟进程生效

---

## 13. 日志 UI

- `LogViewerActivity`
- `ScriptLogActivity extends LogViewerActivity`
- `AlarmLogImporter`
- `AlarmLogImporter` 从镜像目录导入，按 `ts|message` 去重
- 解析：
  ```text
  --- stdout ---
  --- stderr ---
  ```
- stdout 为空时把 stderr 合并到输出
- `LogListFragment.onResume()` 会强制刷新
- 导入必须串行：`AlarmLogImporter.importNew` 用一把静态锁包住整趟扫描，且每导入一个文件就推进
  `last_ts`。日志页每次刷新都会触发导入，两趟并发会读到同一个 `last_ts` 并把同一批文件各插一遍
  （历史 bug：每个脚本执行出现两条记录）。去重键是 `ts|message`，另加一次性
  `deleteDuplicateScriptLogs()` 收掉旧版本插重的行
- 文件名时间戳是 `yyyyMMddHHmmss`，导入时**忽略未来时间**的文件：否则一个伪造的未来文件名会把
  `last_ts` 顶到未来，之后所有真实日志都被当成“更旧”而永远不再导入
- 通知点击：脚本执行结果通知由时钟进程发，时钟不能启动本模块的非导出 Activity，所以
  `TaskManager.logIntent()` 指向导出的 `com.hyperalarm.log.ScriptLogLauncher`（校验脚本名后
  再启动真正的 `ScriptLogActivity`）。不要再把通知指向时钟自己的 Activity

---

## 14. 发布前检查清单

### 代码

- [ ] `./gradlew :app:assembleRelease -x lint --max-workers=1` 通过
- [ ] `./gradlew :app:lintRelease` 无阻断项
- [ ] 检查 `AndroidManifest.xml` 是否还有调试 Activity / 调试广播
- [ ] `hyperalarm.permission.BRIDGE` 仍存在，**只在接收端**注册（`registerReceiver(..., BRIDGE, ...)`），
      发送端必须不传该权限（传了语义反了，时钟会被系统丢弃广播），且 `ShiftBridge` 仍校验发送方 uid
- [ ] `scope.list` 只有 `com.android.deskclock`
- [ ] 包名没有 `dev` / `debug` 残留
- [ ] 用户可见名称是 `HyperAlarm`
- [ ] `README.md`（中文，仓库根）与 `docs/README_*.md`（其余 16 种语言）与本次改动的行为一致
      （安装步骤、脚本约定、已知限制）；改完中文记得同步翻译，语言切换行的相对路径不要写错
- [ ] `THIRD_PARTY_NOTICES.md` 列的都是仓库里还在的组件
- [ ] `.github/ISSUE_TEMPLATE` 问的是时钟版本 / 脚本 / 日志，不是上游的作用域与规则

### 功能

- [ ] 主页能读取真实闹钟
- [ ] 新增 / 编辑 / 删除 / 开关正常
- [ ] 铃声一级开关打开后脚本和闹钟 UI 同时出现
- [ ] 关闭后脚本静默执行
- [ ] 立即执行能产生脚本日志
- [ ] 轮班制创建 / 编辑 / 删除正常
- [ ] 时钟列表隐藏脚本任务设置正常
- [ ] 时钟右上角 `HyperAlarm` 菜单入口存在
- [ ] 重启时钟按钮会 force-stop 后后台启动 `ClockService`

### 签名

`app/build.gradle.kts` 在没有 `signing.properties` 时**拒绝构建** release（见 §3）。  
正式发布必须创建本地 `signing.properties`（该文件已在 `.gitignore` 中）：

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

注意：

- 换正式签名后，之前 debug 签名的安装包不能直接覆盖安装。
- 发布更新必须一直使用同一个 keystore。
- 当前这把 release keystore 在 `C:/Users/Grace/hyperalarm-release.jks`，密码另存
  `C:/Users/Grace/hyperalarm-release.password`；`signing.properties` 指向它。
  **这两个文件是唯一无法重建的东西**，必须离线备份；CI 用的是同一把（GitHub secrets
  `SIGNING_KEY` / `KEY_STORE_PASSWORD` / `ALIAS`）。
- 仓库地址：`https://github.com/Zoeicx/HyperAlarm`（public），`origin` 指它；
  上游 HyperCeiler 保留为 `upstream` 远端。发布流程标注见下：tag 与 Release 用 `gh release create`。

### 版本

两套号，各管一件事（都在 `app/build.gradle.kts` 里，不要另建配置）：

| | 值 | 谁看 | 怎么变 |
| --- | --- | --- | --- |
| `versionName` | 语义化 `1.0.0` | 用户 / LSPosed / issue | 发布时手改：修 bug 升 patch，新功能升 minor，破坏性变更（数据契约、桥协议）升 major |
| `versionCode` | `yyyyMMdd * 10 + 当日序号` | Android 安装器 | 自动，随日期前进 |

```kotlin
val appVersionName = "1.0.0"                 // 手改这一处
val dailyBuild = (findProperty("dailyBuild") as String?)?.toIntOrNull() ?: 0
// -> 202611100
```

为什么不用 git 提交数：rebase / squash / 强推会让计数**变小**，那样的包装不到已装的版本上；
日期只会前进，与历史、分支无关。同一天发第二次包加 `-PdailyBuild=1`（0..9，一位留给它）。

CI（`release_build.yml`）先由 `set_tag_version` 打 tag，`build` 用 `needs` 等它，`post_release`
再 `download-artifact` 取 APK 并建 release（`$GITHUB_ENV` 不跨 job，所以路径在 post job 里重建）；
`build` 还会跑 `checkI18n` 与 `:app:lintRelease`。看到提交信息
`app: update version <旧> to <新> [release]` 时触发，所以发布步骤是：改 `appVersionName` → 用这条格式写提交信息 → 推。

---

## 15. 已知限制

- “隐藏脚本任务”打开时，时钟主页“下一闹钟”文案会跳过脚本任务，普通闹钟与轮班制都参与比较
  （见 §12）；`ShiftAlarmDataHelper.getShowAlarms()` 拿不到结果时保留时钟原文案
- 切换“隐藏脚本任务”后，如果时钟进程已经在运行，列表可能需要重新进入闹钟页才刷新。
- 铃声选择依赖时钟自己的 `AlarmRingtonePickerActivity`，不同 HyperOS 版本字段可能变化。
- 轮班制高度依赖 Clock 18.28 的反射字段；升级时钟版本后要重新验证。
- release 签名尚未配置正式 keystore。
- `targetSdk = 37` 而我们只在 Android 16 / SDK 36 上验证过；`compileSdk 37` 还需要
  `android.suppressUnsupportedCompileSdk=37.0` 压掉 AGP 警告。真要收紧就一起改回 36。
- `lintRelease` 尚未完成一次完整通过记录。

---

## 16. 调试命令

### 查看模块日志

```bash
adb logcat -d | grep -iE 'HyperAlarm|LSPosed|Xposed'
```

### 查看时钟进程

```bash
adb shell ps -A | grep deskclock
```

### 强制停止并后台启动

```bash
adb shell am force-stop com.android.deskclock
adb shell am startservice -n com.android.deskclock/com.xiaomi.smarthome.ClockService
```

### 查询闹钟

```bash
adb shell content query --uri content://com.android.deskclock/alarm
```

### 查询轮班

```bash
adb shell content query --uri content://com.android.deskclock/shift_alarms
```

### 触发脚本广播（仅调试，需 root/shell 持有签名权限时不可用）

普通 adb shell 没有签名权限，不能直接发桥广播。  
调试时优先从 HyperAlarm UI 操作，或临时在模块内加日志。

---

## 17. 编码约定

- 不要用 `android.app.AlertDialog`，统一用 `fan.appcompat.app.AlertDialog`
- `Preference` 内部刷新用公开的 `refresh()` 包装，不要直接调 `notifyChanged()`
- Java lambda 不能捕获会被重新赋值的局部变量，需要 `final`
- 不要批量删除 HyperCeiler 资源，很多 MIUIX 样式仍被依赖
- 不要重命名 `com.sevtinge.hyperceiler:hyperceiler-expansion` 的包名
- 新增用户可见文本优先中文，必要时再加英文资源
- 「梗体中文」不是新语言，就是已有的 `zh-ME` locale（`values-zh-rME/`，设置里显示为
  `settings_app_zh_me`）。写它的规矩：**从头重新写梗，不是把简中改两个字**；`%1$s` / `%1$d`
  的数量与顺序必须和 `values/strings.xml` 完全一致（否则运行时 `StringFormatInvalid`）；
  脚本名、路径、`Root`、`LSPosed`、`HyperAlarm`、小米时钟等专有名词不玩梗；危险操作提示照旧说清
- 覆盖率是硬要求：`./gradlew checkI18n` 的第 4 条规则要求 `app` / `core` / `provision` / `libhook`
  每一个 `translatable="true"` 的字符串都有 zh-ME 条目，缺一条就失败（`PROBLEM meme <module> <name>`）。
  新增文案时随手补上梗体版本，否则 CI 直接红。`common` 的字符串全部 `translatable="false"`，不参与
- 这些库模块的 lint 都关掉了 `MissingTranslation` / `ExtraTranslation`：它们带的是父项目本就残缺的
  翻译，留着只会淹没真正要看的检查；本 fork 自己声明的字符串由上面的规则兜底
- 可见文案一律走 `@string`：lint 的 `HardcodedText` 只查 `res/layout`，**不查 `res/xml`（偏好项标题/摘要）
  和 Java 字面量**，所以靠 lint 拦不住，提交前必须跑：
  ```bash
  ./gradlew checkI18n                # 五条规则，有任何一条就失败（没有 baseline 豁免）：res 里的字面量、
                                     # Java 里的 CJK、每个模块的字符串在 16 个 locale 里齐全、占位符一致、
                                     # zh-ME 覆盖每个模块的每一条字符串
  ```
  CI（`ci_build.yml` / `release_build.yml`）会跑同一条命令，新增问题直接失败。
  hook 侧（`library/libhook/**`）的日志文案保持中文是刻意的：时钟进程加载不到本模块资源。
- 根 shell 命令必须用 `RootShell.quote()` 包参数
- SAF 的 documentId 一律当外部输入：`ScriptDocumentsProvider.pathOf()` 只接受
  `scripts` 或 `scripts/<ScriptPath.isPlainName 通过的文件名>`，`isChildDocument` 用同一套
  解析；provider 是只读的，不实现也不广告 create/delete
- root 调用只有一套：`com.hyperalarm.common.utils.RootShell`（libsu 封装，`run` / `runOutput` /
  `succeeded` / `isRoot` / `quote` / `realPath`）。不要再引入 `ProcessBuilder("su")`、`Runtime.exec("su")`
  或自制常驻 shell；历史遗留的 `ShellUtils` / `ShellExec` / `ShellInit` 已删除。
  唯一允许的例外是**需要流式输出**的 logcat 抓取（`LogcatLogLoader` 用 `su -c logcat` 自己读管道并带 watchdog），
  因为 `RootShell.run` 会缓冲全部输出且没有单命令超时。
- 所有跨进程广播必须带 `hyperalarm.permission.BRIDGE`
- 修改 Hook 后必须实机验证时钟进程不会崩
- release **不要**剥离 `android.util.Log`：`ModuleLog` / `AndroidLog` / `XposedLog` 最终都落到
  `android.util.Log`，一条 `-assumenosideeffects` 会把本模块自己的日志页掏空。历史上那份
  `app/release/keepRules/log.keep` 从未被 applied（`configuration.txt` 里没有），已删除
- 日志分层（不要再新增日志类）：
  | 位置 | 用什么 | 说明 |
  | --- | --- | --- |
  | app / provision / core 的界面与业务 | `AndroidLog` | 落 logcat（`HyperAlarm.Raw.<tag>`）+ 应用日志库 |
  | 日志基础设施自身（`LogRepository`、`LogSyncDiagnostics`） | `ModuleLog` | 只写 logcat 的原始层，避免写库递归 |
  | 时钟进程内 `com.hyperalarm.alarm.**` | `XposedLog` | 由 `ModernInit.onModuleLoaded` 里 `XposedLog.init(this)` 初始化；输出 `[Tag]: msg`，LSPosed 前缀里带 `HyperAlarm` 所以 app 的日志导入仍能识别 |
  `XposedLog` 在“日志等级尚未初始化”的进程里不做过滤（钩子进程读不到模块偏好），
  否则所有日志会被静默丢弃。
- 后台线程与主线程投递统一走 `com.hyperalarm.common.utils.ThreadUtils`：
  `runOnBackground(Runnable)` / `postOnBackgroundThread(Callable)` / `postOnMainThread(...)`。
  不要再 `new Thread(...).start()`，也不要自建 `Executors.newSingleThreadExecutor()`。
  仍保留的专用线程只有四处，都属于「有明确串行或生命周期要求」：
  `LogRepository`（日志 DB 写入串行）、`TaskRunner`（应用初始化任务图）、
  `LogcatLogLoader`（抓取 watchdog）、`TaskManager`（每个脚本一个命名线程，便于 logcat 定位）

---

## 18. 交付产物

交付物就是构建出的 release APK：

```text
app/build/outputs/apk/release/*.apk
```

需要校验时：

```bash
APK=$(find app/build/outputs/apk/release -name '*.apk' | head -1)
sha256sum "$APK"
```

---

## 19. LSPosed 模块仓库（Xposed-Modules-Repo）

模块已提交并被自动批准，镜像仓：`https://github.com/Xposed-Modules-Repo/io.github.zoeicx.hyperalarm`。

- **包名是硬门槛**：`bot.ts` 的 `checkPackageName()` 要求 applicationId 至少三段、每段
  `[A-Za-z_][A-Za-z0-9_]*`、不含 `example`、不以 `com.android/com.google/org.lsposed/io.github.lsposed/com.xiaomi`
  开头。这就是 applicationId 从 `com.hyperalarm` 改成 `io.github.zoeicx.hyperalarm` 的唯一原因
  （代码命名空间仍是 `com.hyperalarm`）。
- **自动过审只有三条路**：包名以 `io.github.<用户名，小写、`-`→`_`>` 开头；或是你有权限的组织同前缀；
  或在包名域名的根域放 TXT `lsposed-modules-repo-verification=<github 用户名>`。
  否则 issue 会被打 `invalid` 直接关闭，没有人工。
- **镜像仓的三条硬要求**：仓库 description 不能空（= 模块显示名 `HyperAlarm`，**只能在网页 UI 设**，
  maintain 权限的 API 会 404）、至少一个 release、tag 必须是 `<VersionCode>-<VersionName>`
  （如 `202611180-1.0.1-20261118`）。全齐后约 3 个工作日出现在管理器与站点。
- 镜像仓自己维护 `README.md` / `SUMMARY` / `SOURCE_URL` / `icon.svg`，与源码仓是两套内容。
- **踩过的坑**：`release_build.yml` 的 `post_release` 曾按 artifact 目录名（`HyperAlarm Release`）
  找 APK，`files:` 解析为空，release 建成但**没有附件**，而且失败是静默的。现在改成按文件名匹配
  `*-release.apk`，并打印 `find` 结果、找不到就直接 `exit 1`。

