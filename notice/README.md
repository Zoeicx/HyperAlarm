# notice/app.json — 应用内公告

`NoticeProcessor` 启动时会拉取本文件：

```
https://raw.githubusercontent.com/Zoeicx/HyperAlarm/main/notice/app.json
```

文件不存在或请求失败时，应用静默不弹公告。仓库默认提交的是一份「永不展示」的模板（`endTime: 0`），
要发公告时改成真实值即可，无需发版。

## 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | int | 公告 id。用户确认后记录 `prefs_key_notice_id`，同一 id 不再弹出（`alwaysShow` 例外） |
| `alwaysShow` | bool | true 时忽略 `id` 去重，每次都弹 |
| `title` | string | 标题 |
| `content` | string | 正文纯文本；公告弹窗里「协议 / 隐私」链接来自字符串资源 `tos_update_desc` 的 annotation，不由本文件控制 |
| `confirmDelaySeconds` | int | 确认按钮倒计时秒数，0 表示无倒计时 |
| `startTime` / `endTime` | long | 生效时间窗（毫秒时间戳）。`endTime` 为 0 或早于当前时间则不展示 |
| `versionMin` / `versionMax` | int | 匹配的 `versionCode` 区间，-1 表示不限 |
| `signCheckPassNeed` | bool | true 时只对签名校验通过的构建展示 |
| `buildType` | string[] | 构建类型白名单（如 `["release"]`），空数组表示不限 |
| `androidVersion` | int[] | Android SDK 版本白名单，空数组表示不限 |
| `miuiBigVersion` / `miuiSmallVersion` | number[] | HyperOS 版本白名单，空数组表示不限 |
| `lang` | string[] | 语言白名单（BCP-47，如 `["zh-CN"]`），空数组表示不限 |
| `protocolVersion` / `privacyVersion` | int | 协议版本号。大于用户已确认版本时触发协议更新提示，-1 表示不检查 |

## 示例：给 release 版推一条公告

```json
{
  "id": 20261101,
  "alwaysShow": false,
  "title": "1.0.1 已发布",
  "content": "本次修复了脚本标签路径校验：标签只接受脚本目录下的纯文件名，绝对路径与子目录会被拒绝。",
  "confirmDelaySeconds": 0,
  "startTime": 1761955200000,
  "endTime": 1764547200000,
  "versionMin": -1,
  "versionMax": -1,
  "signCheckPassNeed": true,
  "buildType": ["release"],
  "androidVersion": [],
  "miuiBigVersion": [],
  "miuiSmallVersion": [],
  "lang": [],
  "protocolVersion": -1,
  "privacyVersion": -1
}
```
