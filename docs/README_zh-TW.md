<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | **繁體中文** | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm 是一個 Xposed 模組，**唯一作用域是小米時鐘**。它把小米時鐘的鬧鐘變成 shell 腳本的觸發器。

## 模組宿主

僅在以下版本驗證。其餘版本請自測，或提供安裝包以便適配，或升級/降級到以下版本：

- 小米時鐘 `com.android.deskclock`
- 版本 `18.28.0`，版本號 `1306002800`

## 安裝

1. 準備 root（KernelSU / Magisk）與 LSPosed（Xposed API 101 以上）。
2. 安裝本模組，在 LSPosed 裡啟用 HyperAlarm。
3. 作用域只勾「小米時鐘」`com.android.deskclock`，不要加別的套件。
4. 強制停止小米時鐘，再重新啟動它的進程，模組才會生效。

## 用法

- 新增任務：主頁**頂部操作列右側**的 `+`。
- 任務標籤就是腳本名，必須以 `.sh` 結尾：
  - `alert.sh`：時間到靜默執行，不響鈴、不亮鬧鐘介面
  - `@alert.sh`：執行腳本，同時保留鬧鐘介面與鈴聲
- 清單裡的 `@` 會自動隱藏；打開「鈴聲」開關就能在兩種模式之間來回切。
- 「立即執行」可以單獨試跑腳本，不用等到時間到。
- 輪班制任務依班次觸發，班次由開始日期與週期推算。

## 腳本約定

腳本放在 `/data/adb/alarm_shell/scripts/`，標籤裡的名字必須和檔名一致。

```text
/data/adb/alarm_shell/scripts/      # 腳本目錄
/data/adb/alarm_shell/password      # 鎖屏密碼
/data/adb/alarm_shell/unlock_phone  # 預設解鎖腳本
```

- 必須是 LF 換行，Windows 的 CRLF 會讓腳本直接失敗。
- 腳本以 root 身分執行，跑在系統主命名空間裡，看得到其他應用程式的資料目錄。
- 每次執行都會留日誌：應用程式內的「腳本日誌」看得到，同時鏡像到
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<腳本名>/`。
- 靜默任務執行前，如果設過鎖屏密碼，會先用它解鎖螢幕。

## 設定

- **時鐘只顯示鬧鐘**：小米時鐘清單裡只留真正的鬧鐘，腳本任務不顯示。
- **隱藏響鈴任務**：連開了鈴聲的任務一起藏起來。
- 日誌輸出等級 / 應用程式日誌 / 腳本日誌：排查問題時先把等級調高。
- **鎖屏密碼設定**：給靜默任務解鎖用，留空表示不修改。
- 重啟教學、複製腳本目錄、備份與還原模組設定。

## 已知限制

- 只在 Android 16 / HyperOS 3 / 小米時鐘 18.28.0 上驗證過，其他組合請自測。
- 輪班制依賴時鐘自己的內部實作，升級時鐘後需要重新驗證。
- 沒有 root 時模組無法運作，設定頁會給出提示。
- 腳本日誌會鏡像到外部儲存空間，root 與 adb shell 都讀得到，別把密鑰回顯到腳本輸出裡。

## 常見問題

- **加了任務但時鐘沒反應**：確認 LSPosed 裡模組已啟用、作用域只有小米時鐘，並且重啟過時鐘進程。
- **任務不出現在時鐘清單裡**：這是「時鐘只顯示鬧鐘」在作用，關掉即可。
- **時間到沒執行**：看應用程式內的腳本日誌，腳本缺失或標籤拼錯都會有明確報錯。

## 授權條款

- 本倉庫自有程式碼：**AGPL-3.0**（見 `../LICENSE`）
- 第三方元件與各自的授權條款見 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## 致謝

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)：本專案的起點，MIUIX 介面與設定框架大多來自它
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix)：小米風格 UI 元件
- [libsu](https://github.com/topjohnwu/libsu)、[libxposed](https://github.com/libxposed/api)：root 與 Xposed API
- 所有提交 issue、回報日誌的使用者
