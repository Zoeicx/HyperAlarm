<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | **繁體中文（香港）** | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm 係一個 Xposed 模組，**唯一作用域係小米時鐘**。佢將小米時鐘嘅鬧鐘變成 shell script 嘅觸發器。

## 模組宿主

只喺以下版本驗證過。其他版本請自己試，或者提供安裝包等我哋適配，又或者升級/降級返去以下版本：

- 小米時鐘 `com.android.deskclock`
- 版本 `18.28.0`，版本號 `1306002800`

## 安裝

1. 準備 root（KernelSU / Magisk）同 LSPosed（Xposed API 101 以上）。
2. 裝咗本模組，喺 LSPosed 度啟用 HyperAlarm。
3. 作用域只剔「小米時鐘」`com.android.deskclock`，唔好加其他包。
4. 強制停止小米時鐘，再重新啟動佢個進程，模組先會生效。

## 用法

- 新增任務：主頁**頂部操作列右邊**嘅 `+`。
- 任務標籤就係 script 名，一定要以 `.sh` 結尾：
  - `alert.sh`：夠鐘靜默執行，唔響鈴、唔彈鬧鐘介面
  - `@alert.sh`：執行 script，同時保留鬧鐘介面同鈴聲
- 清單入面個 `@` 會自動收埋；開返「鈴聲」開關就可以喺兩種模式之間切換。
- 「立即執行」可以自己試跑 script，唔使等夠鐘。
- 輪班制任務按班次觸發，班次由開始日期同週期推算。

## Script 規矩

Script 放喺 `/data/adb/alarm_shell/scripts/`，標籤入面個名一定要同檔名一致。

```text
/data/adb/alarm_shell/scripts/      # script 目錄
/data/adb/alarm_shell/password      # 鎖屏密碼
/data/adb/alarm_shell/unlock_phone  # 預設解鎖 script
```

- 一定要用 LF 換行，Windows 嘅 CRLF 會令 script 直接死。
- Script 以 root 身分執行，跑喺系統主命名空間，睇得到其他應用嘅資料目錄。
- 每次執行都會留日誌：應用入面嘅「Script 日誌」睇得到，同時鏡像去
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<script 名>/`。
- 靜默任務執行之前，只有部機重啟後仍然鎖住嘅時候，先會喚醒同解鎖螢幕；平時熄咗芒就唔會著返。

## 設定

- **時鐘只顯示鬧鐘**：小米時鐘清單只留真正嘅鬧鐘，script 任務唔顯示。
- **隱藏響鈴任務**：連開咗鈴聲嘅任務都一齊收埋。
- 日誌輸出等級 / 應用日誌 / Script 日誌：查問題時先將等級調高。
- **鎖屏密碼設定**：只會喺部機重啟後仲鎖住嘅時候，幫靜默任務解鎖；留空即係唔改。
- 重啟教學、複製 script 目錄、備份同還原模組設定。

## 已知限制

- 只喺 Android 16 / HyperOS 3 / 小米時鐘 18.28.0 驗證過，其他組合請自己試。
- 輪班制靠時鐘自己嘅內部實作，升級時鐘之後要重新驗證。
- 冇 root 嘅話模組行唔到，設定頁會提示你。
- Script 日誌會鏡像去外部儲存空間，root 同 adb shell 都讀得到，唔好將密鑰寫落 script 輸出。

## 常見問題

- **加咗任務但時鐘冇反應**：確認 LSPosed 入面模組已啟用、作用域只有小米時鐘，並且重啟過時鐘進程。
- **任務唔出現喺時鐘清單**：呢個係「時鐘只顯示鬧鐘」做緊嘢，熄咗佢就得。
- **夠鐘冇執行**：睇應用入面嘅 script 日誌，script 唔見咗或者標籤打錯字都會有清楚報錯。

## 授權

- 本倉庫自有程式碼：**AGPL-3.0**（見 `../LICENSE`）
- 第三方組件同各自嘅授權見 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## 致謝

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)：本項目嘅起點，MIUIX 介面同設定框架大部分來自佢
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix)：小米風格 UI 組件
- [libsu](https://github.com/topjohnwu/libsu)、[libxposed](https://github.com/libxposed/api)：root 同 Xposed API
- 所有提交 issue、畀日誌嘅用戶
