<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | **梗体中文** | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm 是个 Xposed 模块，**只管小米时钟一家**，别的包一个都不碰。干的活儿很朴素：闹钟一响，它就把你的 shell 脚本叫起来干活。

## 认哪家的时钟

只认下面这口锅。别的版本你自己试水，或者把安装包甩过来让我们适配，或者老老实实升/降到这版：

- 小米时钟 `com.android.deskclock`
- 版本 `18.28.0`，版本号 `1306002800`

## 怎么装

1. 先把 root（KernelSU / Magisk）和 LSPosed（Xposed API 101 以上）备齐。
2. 装上本模块，去 LSPosed 里把 HyperAlarm 打开。
3. 作用域只勾「小米时钟」`com.android.deskclock`，别的包别手贱多加。
4. **强制停止小米时钟**，再让它进程重新爬起来，不然模块在那儿干瞪眼不干活。

## 咋用

- 建任务：主页**顶上那条操作栏最右边**那个 `+`。
- 标签就是脚本名，尾巴必须挂 `.sh`：
  - `alert.sh`：到点闷声干活，不响不亮，闹钟界面不出来
  - `@alert.sh`：活照干，闹钟界面和铃声也留着
- 列表里那个 `@` 会被自动藏起来；把「铃声」开关一拨，两种模式随便切。
- 「立即执行」可以先空跑一遍，不用熬到点。
- 轮班制任务按班次上岗，班次是从开工日期和周期推出来的。

## 脚本的规矩

脚本都住 `/data/adb/alarm_shell/scripts/`，标签里的名字得跟文件名一模一样。

```text
/data/adb/alarm_shell/scripts/      # 脚本窝
/data/adb/alarm_shell/password      # 锁屏那串咒语
/data/adb/alarm_shell/unlock_phone  # 默认解锁脚本
```

- 换行必须是 LF。从 Windows 拖进来的 CRLF 会让脚本当场暴毙。
- 脚本是 root 身份在干活，跑在系统主命名空间里，别的应用的数据目录它都看得见。
- 每次干活都留痕：应用里的「脚本日志」页面看得到，另外还镜像了一份到
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<脚本名>/`。
- 闷声任务动手之前，你要是设过锁屏密码，它会先拿那串咒语把屏幕解开。

## 设置里这些

- **时钟只显示闹钟**：小米时钟那边只留真闹钟，脚本任务一律不露脸。
- **隐藏响铃任务**：连开了铃声的任务也一并藏好。
- 日志输出等级 / 应用日志 / 脚本日志：出事了先把等级调高再看，别拿「一般日志」硬猜。
- **锁屏密码设置**：给闷声任务解锁用的，留空就是不动它。
- 重启教程、复制脚本目录、备份与恢复模块配置。

## 它现在还不行的地儿

- 只在 Android 16 / HyperOS 3 / 小米时钟 18.28.0 上验过，换一套组合自己试。
- 轮班制是踩着时钟自己的内部实作在跑，时钟一升级就得重新验一遍。
- 没 root 的话它基本是个摆设，设置页会老实告诉你。
- 脚本日志会镜像到外部存储，root 和 adb shell 都读得到——**别把密钥回显到脚本输出里**。

## 翻车了先看这

- **加了任务时钟一点动静没有**：先查 LSPosed 里模块开没开、作用域是不是只有小米时钟，再确认时钟进程重启过。
- **任务在时钟列表里没影**：那是「时钟只显示闹钟」在尽职，关掉它人就回来了。
- **到点了啥也没干**：去应用里的脚本日志翻，脚本不见了或者标签打错字，那儿都会明说。

## 许可证

- 本仓库自己写的代码：**AGPL-3.0**（见 `../LICENSE`）
- 第三方组件和各自的许可证：见 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## 谢过这些人

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)：一切的起点，MIUIX 那套界面和设置框架大半是它给的
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix)：小米味儿 UI 组件
- [libsu](https://github.com/topjohnwu/libsu)、[libxposed](https://github.com/libxposed/api)：root 和 Xposed API
- 所有提过 issue、塞过日志的朋友
