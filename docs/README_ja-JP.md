<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | **日本語** | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm は Xposed モジュールで、**スコープは小米時計だけ**です。小米時計のアラームを shell スクリプトの起動条件に変えます。

## 対象アプリ

検証済みは下記のバージョンのみです。それ以外は自己責任で試すか、適応のために APK を提供するか、下記へ更新／ダウングレードしてください:

- 小米時計 `com.android.deskclock`
- バージョン `18.28.0`、バージョンコード `1306002800`

## インストール

1. root（KernelSU / Magisk）と LSPosed（Xposed API 101 以上）を用意します。
2. このモジュールをインストールし、LSPosed で HyperAlarm を有効にします。
3. スコープは「小米時計」`com.android.deskclock` だけにチェックを入れてください。他のパッケージは追加しないでください。
4. 小米時計を強制停止し、プロセスを起動し直すとモジュールが有効になります。

## 使い方

- 新規作成: ホーム画面**上部アクションバーの右端**にある `+`。
- ラベルがそのままスクリプト名で、末尾は `.sh` である必要があります:
  - `alert.sh`: 時刻になると静かに実行。鳴らず、アラーム画面も出ません
  - `@alert.sh`: スクリプトを実行しつつ、アラーム画面と着信音も残します
- 一覧では `@` は自動的に隠れます。「着信音」スイッチで 2 つのモードを行き来できます。
- 「今すぐ実行」で、時刻を待たずにスクリプトを試せます。
- シフト制のタスクは開始日と周期から算出したシフトごとに発火します。

## スクリプトの決まり

スクリプトは `/data/adb/alarm_shell/scripts/` に置き、ラベルの名前はファイル名と一致させてください。

```text
/data/adb/alarm_shell/scripts/      # スクリプト置き場
/data/adb/alarm_shell/password      # ロック画面のパスワード
/data/adb/alarm_shell/unlock_phone  # 既定のロック解除スクリプト
```

- 改行は LF のみ。Windows の CRLF を持ち込むとスクリプトは即失敗します。
- スクリプトは root 権限で、システムのメインネームスペース上で動くため、他アプリのデータディレクトリも見えます。
- 実行のたびにログが残ります。アプリ内の「スクリプトログ」で見られ、
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<スクリプト名>/` にも複製されます。
- 静かなタスクの実行前、ロック画面のパスワードを設定してあれば、それで画面を解除します。

## 設定

- **時計はアラームだけ表示**: 小米時計の一覧には本物のアラームだけが残り、スクリプトのタスクは表示されません。
- **鳴るタスクも隠す**: 着信音を有効にしたタスクも一緒に隠します。
- ログ出力レベル / アプリログ / スクリプトログ: 不具合を調べるときはまずレベルを上げてください。
- **ロック画面のパスワード**: 静かなタスクの解除に使います。空欄なら変更しません。
- 再起動チュートリアル、スクリプトディレクトリのコピー、設定のバックアップと復元。

## 既知の制限

- Android 16 / HyperOS 3 / 小米時計 18.28.0 でしか検証していません。他の組み合わせは自己責任で。
- シフト制は時計側の内部実装に依存するため、時計を更新したら再検証が必要です。
- root がないと動作しません。設定画面にもその旨が表示されます。
- スクリプトのログは外部ストレージに複製され、root と adb shell から読めます。秘密情報をスクリプトの出力に出さないでください。

## よくある質問

- **タスクを追加したのに時計が反応しない**: LSPosed でモジュールが有効か、スコープが小米時計だけか、時計のプロセスを再起動したかを確認してください。
- **タスクが時計の一覧に出てこない**: 「時計はアラームだけ表示」が効いています。オフにすれば戻ります。
- **時刻になっても実行されない**: アプリ内のスクリプトログを見てください。スクリプトが見つからない、ラベルの綴りが違う、といった理由が明示されます。

## ライセンス

- このリポジトリ固有のコード: **AGPL-3.0**（`../LICENSE`）
- サードパーティ製コンポーネントと各ライセンス: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## 謝辞

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): このプロジェクトの出発点。MIUIX の UI と設定基盤の多くはここ由来です
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): 小米風の UI コンポーネント
- [libsu](https://github.com/topjohnwu/libsu)、[libxposed](https://github.com/libxposed/api): root と Xposed の API
- issue を立て、ログを送ってくれたすべての人
