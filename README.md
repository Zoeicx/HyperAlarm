<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

**简体中文** | [English](docs/README_en-US.md) | [繁體中文](docs/README_zh-TW.md) | [繁體中文（香港）](docs/README_zh-HK.md) | [梗体中文](docs/README_zh-ME.md) | [日本語](docs/README_ja-JP.md) | [Русский](docs/README_ru-RU.md) | [Українська](docs/README_uk-UA.md) | [Português (Brasil)](docs/README_pt-BR.md) | [Deutsch](docs/README_de-DE.md) | [Español](docs/README_es-ES.md) | [Italiano](docs/README_it-IT.md) | [Polski](docs/README_pl-PL.md) | [Türkçe](docs/README_tr-TR.md) | [Tiếng Việt](docs/README_vi-VN.md) | [العربية](docs/README_ar-SA.md) | [Bahasa Indonesia](docs/README_in-ID.md)

</div>

---

HyperAlarm 是一个 Xposed 模块，**唯一作用域是小米时钟**。它把小米时钟的闹钟变成 shell 脚本的触发器。

## 模块宿主

仅在以下版本验证。其余版本请自测，或提供安装包以便适配，或升级/降级到以下版本：

- 小米时钟 `com.android.deskclock`
- 版本 `18.28.0`，版本号 `1306002800`

## 安装

1. 准备 root（KernelSU / Magisk）与 LSPosed（Xposed API 101 以上）。
2. 安装本模块，在 LSPosed 里启用 HyperAlarm。
3. 作用域只勾「小米时钟」`com.android.deskclock`，不要加别的包。
4. 强制停止小米时钟，再重新启动它的进程，模块才会生效。

## 用法

- 新建任务：主页**顶部操作栏右侧**的 `+`。
- 任务标签就是脚本名，必须以 `.sh` 结尾：
  - `alert.sh`：到点静默执行，不响铃、不亮闹钟界面
  - `@alert.sh`：执行脚本，同时保留闹钟界面与铃声
- 列表里 `@` 会自动隐藏；打开「铃声」开关即可在两种模式之间来回切。
- 「立即执行」可以单独试跑脚本，不用等到点。
- 轮班制任务按班次触发，班次由开始日期与周期推算。

## 脚本约定

脚本放在 `/data/adb/alarm_shell/scripts/`，标签里的名字必须和文件名一致。

```text
/data/adb/alarm_shell/scripts/      # 脚本目录
/data/adb/alarm_shell/password      # 锁屏密码
/data/adb/alarm_shell/unlock_phone  # 默认解锁脚本
```

- 必须是 LF 换行，Windows 的 CRLF 会让脚本直接失败。
- 脚本以 root 身份执行，运行在系统主命名空间里，能看到其他应用的数据目录。
- 每次执行都会留日志：应用内的「脚本日志」能看，同时镜像到
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<脚本名>/`。
- 静默任务执行前，如果设过锁屏密码，会先用它解锁屏幕。

## 设置

- **时钟只显示闹钟**：小米时钟列表里只留真正的闹钟，脚本任务不显示。
- **隐藏响铃任务**：连开了铃声的任务一起藏起来。
- 日志输出等级 / 应用日志 / 脚本日志：排查问题时先把等级调高。
- **锁屏密码设置**：给静默任务解锁用，留空表示不修改。
- 重启教程、复制脚本目录、备份与恢复模块配置。

## 已知限制

- 只在 Android 16 / HyperOS 3 / 小米时钟 18.28.0 上验证过，其他组合请自测。
- 轮班制依赖时钟自己的内部实现，升级时钟后需要重新验证。
- 没有 root 时模块无法工作，设置页会给出提示。
- 脚本日志会镜像到外部存储，root 与 adb shell 都能读，别把密钥回显到脚本输出里。

## 常见问题

- **加了任务但时钟没反应**：确认 LSPosed 里模块已启用、作用域只有小米时钟，并且重启过时钟进程。
- **任务不出现在时钟列表里**：这是「时钟只显示闹钟」在起作用，关掉即可。
- **到点没执行**：看应用内的脚本日志，脚本缺失或标签拼错都会有明确报错。

## 许可证

- 本仓库自有代码：**AGPL-3.0**（见 `LICENSE`）
- 第三方组件与各自的许可证见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)

## 致谢

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)：本项目的起点，MIUIX 界面与设置框架大多来自它
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix)：小米风格 UI 组件
- [libsu](https://github.com/topjohnwu/libsu)、[libxposed](https://github.com/libxposed/api)：root 与 Xposed API
- 所有提交 issue、反馈日志的用户
