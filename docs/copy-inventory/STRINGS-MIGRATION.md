# 文案集中化（12.4）：把散落的文案收进 `strings.xml`

> 目标：**以后改一句文案只改一处**。现在同一句话可能散在 `strings.xml`、Kotlin 硬编码、布局 XML 三处（见 `GUIDE.md` 第一节与 `DUPLICATES.md`），改一处漏两处。
>
> 本文件是这场迁移的**唯一台账**：批次进度、命名规范、不迁移清单都记在这里。改完一批就在这里打勾，别另开新文档。

---

## 0. 进度总表

| 批次 | 范围 | 新键数 | 状态 | 日期 |
| --- | --- | --- | --- | --- |
| B1 | 更新检查 / 下载（`UpdateActivity`、`activity_update.xml`、下载通知与完成提示） | 11 | ✅ 已完成 | 26.10.19 |
| B2 | 列表空态与翻页（基类 `RefreshListActivity` / `RefreshMainActivity` + 4 个布局） | 4 | ✅ 已完成 | 26.10.19 |
| B3 | 下载过程文案（`下载视频` / `下载音频` / `暂停` 等 `DownloadService.state`） | 22 | ✅ 已完成 | 26.10.19 |
| B4 | 下载完成提示 + 通用「加载失败」（`DownloadActivity`、3 个列表基类、`SearchFragment`、`TimelineActivity`、`DownloadNotifier`） | 7 | ✅ 已完成 | 26.10.19 |
| B5 | 敏感操作前的 Cookie 检查（`DUP-02`：5 处同一句话 + 3 个动词标题） | 4 | ✅ 已完成 | 26.10.19 |
| B6 | 通用「操作失败」系列（`DUP-03` / `DUP-04`：5 个 API + 2 个 Adapter + 1 Fragment） | 2（另 3 句进 `api/ApiErrorText.kt`，见 5.5） | ✅ 已完成 | 26.10.19 |
| B7 | 网络错误 / 未知错误（`DUP-05`~`DUP-08`，键在 B6 预置） | 0（复用 B6 键） | ✅ 已完成 | 26.10.19 |
| B8 | 更新下载异常 / 链接解析（`UpdateManager` 4 处、`LinkUrlUtil` 2 处） | 3 | ✅ 已完成 | 26.10.19 |
| B9 | 设置页（`04-settings.md` 的 `S-*`，386 条，**量最大的一批**；走 `SettingText` 映射，见 5.8） | 377 | ✅ 已完成 | 26.10.19 |
| B10 | 其余消息与 Toast（`06-messages-toasts.md` 的 `M-*`）——实际扩成了**全仓 Kotlin 界面文案批量收编**（`UiText.of` 方案，见 5.9） | 742 条映射 / 935 处调用 | ✅ 已完成 | 26.10.19 |
| B11 | 页面静态文案（`05-ui-pages.md` 的 `U-*`）——落成**布局 XML 332 处 + Manifest 103 个 `android:label`**（见 5.10） | 补充键见 5.10 | ✅ 已完成 | 26.10.19 |
| B12 | 教程正文（`Tutorials.kt`）——随 B10 一并收编（`UiText.of` 138 处），正文已落 `strings.xml` | 并入 B10 | ✅ 已完成 | 26.10.19 |
| B13 | 更新日志 / 关于 / 隐私（`01`、`02` 的 `A-*`、`L-*`）——**本就在 `strings.xml`**（`update_log_current`、`update_history_log`、`about_*`、`privacy_*`），无需迁移 | 0（无需） | ✅ 已完成 | 26.10.19 |
| B14 | 彩蛋（`07-eggs.md` 的 `E-*`）——`eggs` 字符串数组本就在 `strings.xml`，代码只按下标取 | 0（无需） | ✅ 已完成 | 26.10.19 |

顺序理由：先做**屏幕小、句子少、入口单一**的（更新/下载），把规范与验证流程跑顺；再按「字符串条数 × 位置数」从少到多做；教程正文风险最高（内有排版约定与「已阅」判定，改动要顺带加 `version`），放最后。

---

## 1. 命名规范

沿用仓库已有的前缀，**不新造体系**：

| 前缀 | 用途 | 例子 |
| --- | --- | --- |
| `pagename_` | 页面标题（顶栏那一行） | `pagename_update_check` |
| `btn_` | 按钮上的字 | `btn_cancel`、`btn_download_update` |
| `text_` | 界面上显示的文本（标签、提示、状态） | `text_download_video` |
| `desc_` | 设置项的说明小字 | `desc_setting_ui` |
| `hint_` | `EditText` 的 hint | `hint_search` |
| `err_` | 错误提示 | `err_crash_upload_json` |
| `a11y_` | 无障碍朗读（`contentDescription`） | 新增，如 `a11y_qr_login` |
| `msg_` | 只在 Toast / 通知里出现的一句话 | 新增，如 `msg_download_complete` |
| `tutorial_` | 教程正文 | `tutorial_cover_play_enabled` |
| `about_` / `feedback_` / `privacy_` | 关于页 / 反馈页 / 隐私 | `about_to_uncle` |

规则：

1. **一个键 = 一句话**。同一句话要显示在两个地方，就两处都用同一个键，**不要为了「位置不同」再开一个键**——那等于把双真相换个地方藏。
2. 键名用**英文短语义**，不用拼音。`text_downloading_video` 而不是 `text_xiazai_shipin`。
3. 同族用序号在值里区分、不用在键名里堆形容词。如清晰度选项：`text_qn_4k` / `text_qn_1080p`。
4. 键名一旦定下就别再改——`strings.xml` 里的键名是唯一索引，改名要全仓 grep。

---

## 2. 占位符规范

- **多参数一律用位置参数**：`%1$s`、`%2$d`。裸 `%s` 在参数顺序变化时会静默错位。
- 单参数可以留 `%s`（既有 `text_update_download_failed` = `下载失败：%s` 就是这样，不必回改）。
- 百分号要写 `%%`：`<string name="text_update_progress">%1$d%%  %2$dMB / %3$dMB</string>`。
- 数量带单位的，单位写进值里，不要拼在代码里：`已下载 %1$dMB`，而不是 `"已下载 " + n + "MB"`。
- `assembleDebug` 时 aapt2 会校验格式串；**参数个数对不上不会报错，运行时才会**——所以改带占位符的句子时要数一遍参数。

---

## 3. 迁移写法

### 3.1 Kotlin（有 Context 的地方，绝大多数）

```kotlin
textView.text = getString(R.string.text_update_checking)
MsgUtil.showMsg(getString(R.string.text_update_latest))
progressText.text = getString(R.string.text_update_progress, percent, downloadedMB, totalMB)
```

- `getString(Int, vararg)` 会自动做 `String.format`，**不要再套一层 `String.format(...)`**。
- 在 `Service` / `Adapter` / `Fragment` 里同样能直接写 `getString(...)`（它们都有 Context）。
- 在 `object` / 伴生对象 / 纯函数里没有 Context：写成 `BiliTerminal.context.getString(...)`。

### 3.2 布局 XML

```xml
android:text="@string/btn_cancel"
```

带参数的句子（进度、计数）**不要放布局**，留空在代码里 `setText`。

### 3.3 迁移后顺手做的事

- 删掉不再需要的 `@SuppressLint("SetTextI18n")`（如果这个函数里已经没有任何字符串拼接）。
- 如果这个键在别处已有同义文案，**把别处也换成同一个键**，并在 `DUPLICATES.md` 对应行标「已收敛」。

---

## 4. 不迁移的字符串（重要）

下面这些**看起来是文案，其实是协议**，改了会坏功能，一律留在代码里：

| 类别 | 例子 | 原因 |
| --- | --- | --- |
| 设置索引的 `name` | `SettingsIndex.Entry(name = "播放设置", ...)` | 既是显示文本又是**跨文件匹配键**，`SettingsIndex` 用它反查分组。改文案必须同时改匹配逻辑，不是纯文案。 |
| 菜单 key | `MenuConfig.ALL_ITEMS` 里的 `"dynamic"` 等 | 存进 SharedPreferences，改了老用户的菜单配置会失效。 |
| SP 键 / DB 列名 / 接口参数 | `"update_last_new_version"`、`msg_type` | 不是文案。 |
| 日志与异常文本 | `Logu.e("xxx失败")`、`throw Exception("...")` | 不显示给用户；只有落盘日志导出器要展示时才考虑。 |
| 布局预览 | `tools:text="xxx"` | 只在 Android Studio 里可见。 |
| 设计占位符 | `05-ui-pages.md` 第十四节那 39 行 | 是版式占位，不是文案。 |

判断口诀：**这句话会进 SharedPreferences / 数据库 / 网络参数 / 用作 key 吗？会，就别动。**

---

## 5. B1 明细（已完成，可作为后续批次的样板）

改动 6 个文件：

| 文件 | 改动 |
| --- | --- |
| `app/src/main/res/values/strings.xml` | **新增 11 个键**（见下），全部追加在 `</resources>` 前，带 `<!-- ===== 更新与下载 ===== -->` 分组注释 |
| `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/UpdateActivity.kt` | 12 处硬编码 → `getString(...)` |
| `app/src/main/res/layout/activity_update.xml` | 6 处 `android:text="…"` → `@string/...` |
| `app/src/main/java/com/RobinNotBad/BiliClient/service/download/DownloadNotifier.kt` | 通知标题「下载完成」「下载视频中」 |
| `app/src/main/java/com/RobinNotBad/BiliClient/service/DownloadService.kt` | 2 处 `1.0f, "下载完成"` |
| `app/src/main/java/com/RobinNotBad/BiliClient/activity/DownloadActivity.kt` | 完成 Toast |

新增的 11 个键：

```xml
<string name="pagename_update_check">检查更新</string>
<string name="text_update_version">版本 %1$s (%2$d)</string>
<string name="text_update_check_failed">检查更新失败：%1$s</string>
<string name="text_update_progress">%1$d%%  %2$dMB / %3$dMB</string>
<string name="text_update_downloaded_mb">已下载 %1$dMB</string>
<string name="text_downloading_video">下载视频中</string>
<string name="btn_cancel">取消</string>
<string name="btn_exit_app">退出应用</string>
<string name="btn_download_update">下载更新</string>
<string name="btn_cancel_download">取消下载</string>
<string name="btn_retry_download">重试下载</string>
```

**复用了既有键**（原来定义了却没人用，正好收口）：

- `text_update_checking`（正在检查更新...）— 布局与代码共用一处
- `text_update_latest`（当前已是最新版）
- `text_update_download_preparing`（准备下载...）
- `text_update_download_complete`（下载完成）— 4 个调用点收敛到 1 个键
- `text_update_download_failed`（下载失败：%s）
- `text_new_version_released`（发现新版本）— 布局 :58 与代码 `tipTitle` 收敛

**顺带发现的死键**（B7 批次处理）：`text_update`（`:49`）、`text_update_new_found`（`:60`，与 `text_new_version_released` 重复）、`text_update_success`（`:50`）、`text_update_network_error`（`:53`）、`text_update_config_error`（`:54`）、`pagename_update`（`:266`）。已登记进 `05-ui-pages.md` 的「死字符串」小节。

---

## 5.1 B2 明细（已完成：列表空态与翻页）

这一批的价值不在「几个键」，而在**一处改动同时修掉 11 个副本**。原来「啥都木有~」有 **5 个副本**（代码常量 1 + 4 个布局），「没有更多了」「正在加载…」各有 **2 个副本**（两个基类各一份）。

| 文件 | 改动 |
| --- | --- |
| `app/src/main/res/values/strings.xml` | 新增 4 个键 |
| `app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshListActivity.kt` | 删掉 `EMPTY_TEXT` / `EMPTY_TEXT_WITH_RETRY` 两个 `const val`（**`const` 没法调 `getString`，所以不能留常量**），`updateLoadMoreTip` 两句、`setOnEmptyRetry` 一句改用 `getString` |
| `app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshMainActivity.kt` | 同样是翻页两句 + 空态一句，`RefreshListActivity.EMPTY_TEXT_WITH_RETRY` 改 `getString` |
| `activity_simple_main_refresh.xml`、`activity_simple_refresh.xml`、`fragment_simple_list.xml`、`fragment_simple_refresh.xml` | `android:text="啥都木有~"` → `@string/text_list_empty` |

```xml
<string name="text_list_empty">啥都木有~</string>
<string name="text_list_empty_retry">啥都木有~\n点我重试</string>
<string name="text_list_loading">正在加载…</string>
<string name="text_list_no_more">没有更多了</string>
```

注意 `text_list_loading` 用的是**省略号字符 `…`**（U+2026），不是三个点。改的时候别被编辑器自动纠正掉。

---

## 5.2 B3 明细（已完成：下载过程）

`DownloadService` 里 `state` 这个变量贯穿整条下载链路，它的值会直接显示在通知栏和下载列表里（「下载视频」「下载音频」「下载弹幕」…）。原来这些字面量在 `DownloadService.kt` 里出现 **25 次**（`state = "x"` 与 `setDownloadProgress(..., "x")` 成对写，同一句话两处），通知标题另有两处。

| 文件 | 改动 |
| --- | --- |
| `app/src/main/res/values/strings.xml` | 新增 22 个键 |
| `app/src/main/java/com/RobinNotBad/BiliClient/service/DownloadService.kt` | 6 类进度状态（视频/音频/封面/字幕/弹幕 × 8+8+2+2+3 处）、准备中、开始下载、下载中兜底 7 处、6 条 Toast、3 条完成通知、退出语 |
| `app/src/main/java/com/RobinNotBad/BiliClient/service/download/DownloadNotifier.kt` | 通知通道名与描述、`总进度 45% · 标题` 这一行的拼接、完成通知标题「下载结束」 |

关键键：

```xml
<string name="text_download_video">下载视频</string>
<string name="text_download_audio">下载音频</string>
<string name="text_download_danmaku">下载弹幕</string>
<string name="text_download_in_progress">下载中</string>
<string name="text_download_some_failed">%1$d 个任务下载失败，请重试</string>
<string name="text_download_link_failed">下载链接获取失败：\n%1$s</string>
<string name="text_download_total_progress">总进度 %1$d%% · %2$s</string>
<string name="channel_download">哔哩终端下载服务</string>
```

这一批顺手把「拼接式文案」收进了格式串：原来通知栏那行是 `"总进度 " + n + "% · " + title`，现在是一条带 `%1$d` / `%2$s` 的完整句子，改文案的人不用再读拼接代码。

---

## 5.3 B4 明细（已完成：下载完成提示 + 通用「加载失败」）

| 文件 | 改动 |
| --- | --- |
| `app/src/main/res/values/strings.xml` | 新增 7 个键（`text_downloading_file`、`text_download_failed`、`text_download_complete_install_tried`、`text_download_complete_install_failed`、`text_download_complete_rename_failed`、`text_danmaku_download_failed`、`text_load_failed`） |
| `app/src/main/java/com/RobinNotBad/BiliClient/activity/DownloadActivity.kt` | 8 处：下载进度前缀 3 条（文件/封面/视频，**后两条复用 B3 的 `text_download_cover` / `text_download_video`**）、下载失败、安装成功/失败/重命名失败、弹幕下载失败 |
| `activity/base/RefreshListActivity.kt`、`activity/base/RefreshMainActivity.kt`、`activity/base/RefreshListFragment.kt`、`activity/video/TimelineActivity.kt`、`activity/search/SearchFragment.kt` | 各 1 处 `MsgUtil.showMsgLong("加载失败")` → `getString(R.string.text_load_failed)` |
| `service/download/DownloadNotifier.kt` | 1 处（下载中通知标题）——**本次修掉一个真实 bug**：这里引用了 `text_downloading_video`，而 `strings.xml` 里没有这个键（文档 §1 的举例也误写成了它），编译直接失败；改用既有 `text_download_video` |

**为什么只加 7 个键**：同一句话已经在别的批次里成键的（下载封面 / 下载视频），直接复用，不新开。这就是「只改一处」的落点——以后改「下载视频」这四个字，改 `strings.xml` 一行，代码里 6 处调用点全跟着变。

---

## 5.4 B5 明细（已完成：Cookie 刷新失败提示）

同一句话在 5 个「敏感操作」入口重复了 5 遍（`DUPLICATES.md` 的 `DUP-02`），只差弹窗标题里的动词：

```java
MsgUtil.showDialog("无法发送", "上一次的Cookie刷新失败了，\n您可能需要重新登录以进行敏感操作", -1)
```

| 键 | 值 | 用处 |
| --- | --- | --- |
| `msg_cookie_refresh_failed` | `上一次的Cookie刷新失败了，\n您可能需要重新登录以进行敏感操作` | 5 处共用 |
| `text_cannot_send` | `无法发送` | 发动态、写回复 |
| `text_cannot_upload` | `无法上传` | 编辑个人资料 |
| `text_cannot_submit` | `无法提交` | 改签名、改昵称头像 |

改动文件：`activity/dynamic/send/SendDynamicActivity.kt`、`activity/reply/WriteReplyActivity.kt`、`activity/user/EditProfileActivity.kt`、`activity/user/EditSignActivity.kt`、`activity/user/EditUserInfoActivity.kt`。

---

## 5.5 B6 明细（已完成：通用「操作失败」）

`DUPLICATES.md` 的 `DUP-03`（错误码兜底）与 `DUP-04`（`操作失败：` + 服务端 message），共 12 处。

**这批最终没有进 `strings.xml`，而是收进了 `api/ApiErrorText.kt`**——原因见下面的「为什么改方向」。

| 共享常量 | 值 | 落点 |
| --- | --- | --- |
| `ApiErrorText.opFailed(code)` | `操作失败（错误码 xxx）` | 5 个 Java API 的 `xxxErrorMsg` 兜底：`DynamicApi.java:263`、`FavoriteApi.java:505`、`FollowApi.java:245`、`PrivateMsgApi.java:406`、`ReplyApi.java:458` |
| `ApiErrorText.opFailedBrief(code)` | `操作失败（xxx）` | Kotlin 侧不带「错误码」三字：`DynamicHolder.kt:302`、`ReplyAdapter.kt:590` |
| `ApiErrorText.opFailedWithDetail(result)` | `操作失败：xxx` | `DynamicHolder.kt:327`、`ReplyAdapter.kt:635`、`OpusContentAdapter.kt:257`、`UserDynamicAdapter.kt:341`、`VideoInfoFragment.kt:461` |
| `msg_network_error_retry` | `网络错误，请重试` | **留在 `strings.xml`**，B7 用 |
| `msg_unknown_error` | `未知错误` | **留在 `strings.xml`**，B7 用 |

### 为什么改方向（重要，别改回去）

B6 最初是照 B1~B5 的老样子，把这三句加进 `strings.xml`，用例里用 `BiliTerminal.context.getString(...)`。结果 **6 个 JVM 单测直接挂**：

```
DynamicApiTest > topErrorMsg_unknownCodeStillShowsTheCode FAILED
FavoriteApiTest > resourceErrorMsg_mapsKnownCodes FAILED
FollowApiMoveUsersTest > 未知码兜底带上错误码本身 FAILED
FollowApiTest > tagErrorMsg_unknownCodeKeepsTheCode FAILED
PrivateMsgApiTest > sessionErrorMsg_成功为空串_其余给出可读提示 FAILED
ReplyApiTest > actionErrorMsg_unknownCodeKeepsTheNumber FAILED
601 tests completed, 6 failed
```

根因：`api/` 下这些 `xxxErrorMsg(code)` 是**纯函数**，单测在**没有 Context** 的 JVM 里跑（本仓库不用 Robolectric），`context!!` 必然空指针；就算不为空，`getString` 也拿不到资源。

所以规则定为：**需要被 JVM 单测直接调用的函数，文案不能依赖 Android Resources**。这类"码 → 人话"的映射进 `api/ApiErrorText.kt`（纯 Kotlin `object` + `@JvmStatic`，Java 在同一个包里直接调，不用 import）；界面层的文案照旧进 `strings.xml`。

改文案时：这三句去 `api/ApiErrorText.kt` 改，`strings.xml` 里搜不到它们（那儿留了一行注释指路）。

---

## 5.6 B7 明细（已完成：网络错误 / 未知错误）

不新增键，直接复用 B6 预置的两条：

| 键 | 值 | 落点 |
| --- | --- | --- |
| `msg_unknown_error` | `未知错误` | 6 处 `optString("message", "未知错误")` 兜底：`api/InteractionVideoApi.java`、`activity/message/MessageSettingsActivity.kt`（2 处）、`activity/settings/login/PasswordLoginFragment.kt`（2 处）、`SMSLoginFragment.kt` |
| `msg_network_error_retry` | `网络错误，请重试` | 5 处 `setStatusText("网络错误，请重试")`（两个登录 Fragment 的验证码/登录失败分支） |

**为什么 `optString(..., 兜底)` 也要迁**：那个兜底值在接口没返回 `message` 时会**真的显示给用户**，它和 Toast 里的句子一样是文案，不是注释。

---

## 5.7 B8 明细（已完成：更新下载异常 / 链接解析）

| 键 | 值 | 落点 |
| --- | --- | --- |
| `text_update_download_failed`（B1 既有） | `下载失败：%s` | `util/UpdateManager.kt:215`（HTTP 失败重试仍失败）、`:239`（`IOException`） |
| `text_update_verify_failed` | `安装包校验失败：%1$s` | `util/UpdateManager.kt:232`（`ApkVerifier` 不通过） |
| `text_update_download_error` | `下载出错：%1$s` | `util/UpdateManager.kt:241`（其它异常） |
| `text_parse_failed` | `解析失败！` | `util/LinkUrlUtil.java:163` |
| `text_list_empty`（B2 既有） | `啥都木有~` | `util/LinkUrlUtil.java:159`——**`DUP-01` 的第 8 处**，这条句子的 8 个位置至此全部指向同一个键 |

顺带把 `:215` 的半角冒号 `下载失败: 500` 统一成了既有键里的全角冒号（唯一一处**刻意**的措辞统一，因为两条分支本来就是同一件事）。

---

## 5.8 B9 明细（已完成：设置页 —— 最大一批）

设置页的难处不是句子多，而是**设置项的名字同时是跨文件匹配协议**。

### 5.8.1 为什么不能直接把名字搬进 strings.xml

`SettingsIndex.Entry.name` 与各设置页 `SettingSection.name` 靠**逐字比对**定位：搜索命中的条目 → `openGroup(activity, type, title, highlight = "圆屏适配")` → 目标页拿 `highlight` 滚动并高亮那一行。把中文字面量从代码里删掉、改成 `R.string.x`，比对两侧类型就不一致了（一侧是资源 id，另一侧是字符串），搜索跳转会**静默失灵**（不报错，只是点了没反应）。

采用的方案是 **「字面量当键、显示走资源」**：

- 新建 `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingText.kt`（395 行，367 条映射）：`object SettingText { fun resolve(context: Context?, raw: String?): String }`。表里查得到就 `context.getString(res)`，查不到原样返回（兼容以后新加、还没登记的条目）。
- 代码里的中文字面量**保持原样不动**，只当 key 用。
- **所有显示点统一过一层 `resolve`**。

于是「改一句只改一处」照样成立：改 `strings.xml` 的值，界面立刻变；键（也就是那些字面量）不用动，匹配也不会断。

### 5.8.2 改了哪几个显示点（只有 4 个文件、26 处）

| 文件 | 改动 |
| --- | --- |
| `adapter/SettingsAdapter.kt` | 9 个 Holder 里 `settingSection.name`（8 处）、`settingSection.desc`（9 处）、`chocola/vanilla.text = strings[0/1]`、`value.text = extra.displayNames[...]`、`putExtra("title", …)` 全部套 `SettingText.resolve(itemView.context, …)` |
| `activity/settings/SettingMainActivity.kt` | 搜索结果卡片 `entry.name` / `entry.desc`、分组卡片 `title`、`putExtra("group_title", …)` |
| `activity/settings/SettingGroupActivity.kt` | `setPageName(groupTitle)` |
| `activity/settings/SettingsIndex.kt` | `openGroup()` 里的 `putExtra("group_title", title)` |

因为显示集中在这 4 处，**一张映射表就覆盖了 386 条设置文案**，不必去改 350 多个 `SettingSection(...)` 调用点。

### 5.8.3 顺带修掉「绕过映射」的直接显示位（36 处）

映射只管「名字 / 说明」，另有一批直接显示的 Toast、状态文字不走它，逐个改掉：

| 文件 | 内容 |
| --- | --- |
| `activity/settings/AboutActivity.kt` | `作者的话`、`给叔叔`、`已关闭开发者模式！`、`已启用开发者模式！`、`本次更新 ($versionName)`（新增 `text_update_current_version`） |
| `activity/settings/SettingGroupActivity.kt` | `登录凭证状态`、`回声洞`、`字体未应用：$error`（新增 `msg_font_apply_failed`） |
| `activity/settings/SettingPlayerChooseActivity.kt` | 两条播放器提醒弹窗（标题 + 正文都要过） |
| `activity/settings/TutorialManagerActivity.kt` | `已完成` / `未完成` / `历史记录：已完成过（版本 N）` |
| `activity/settings/login/AccountSwitchActivity.kt` | 空态两行、`添加新账号`、`已切换至 $displayName` |
| `activity/settings/login/QRLoginFragment.kt` | **24 处**：二维码状态机全部文案，含 3 个 `contentDescription`（`QrScale` 枚举的 `tip: String` 改成 `@StringRes tipRes: Int`，调用点 `MsgUtil.showMsg(getString(qrScale.tipRes))`） |
| `activity/settings/login/SMSLoginFragment.kt` | **8 处**：验证码 / 登录失败提示、`%1$ds后重发`、`获取验证码`，以及两处 `optString("message", 兜底)` 的兜底值 |
| `activity/settings/TestActivity.kt` | 8 处（Debug 专用调试页，一样进资源） |

新增键合计 **377 个**（`settingtext_*` 337 个自动生成 + 手写 40 个：36 个二维码/短信/教程/账号 + 4 个带占位符的 `msg_*`）。

### 5.8.4 验证方式（没有真机时的三条静态判据）

1. `assembleDebug` 通过；
2. 脚本扫 `activity/settings` 下**所有显示位**（`.text =` / `MsgUtil.*` / `setPageName(` / `Toast.makeText` / `TerminalDialog.*`）中仍未用 `getString(` 或 `SettingText.resolve(` 包裹的中文字面量 → 结果 **0 处**；
3. `SettingText.kt` 引用的 367 个 `R.string.*` 全部存在于 `strings.xml`，且 `strings.xml` 728 个 `<string>` 无重名。

---

## 5.9 B10 明细（已完成：全仓 Kotlin 界面文案 —— 用 `UiText` 映射收编）

这一批原本只是「其余消息与 Toast」，实际做成了**整个 Kotlin 界面层的收编**，因为散落的字面量远超清单统计。

**做法**：新增 `app/src/main/java/com/RobinNotBad/BiliClient/util/UiText.kt`，思路与设置页的 `SettingText` 一致——**字面量当键、显示走资源**：

```kotlin
UiText.of("啥都木有~")                       // 命中映射 → strings.xml；未命中/无 Context → 原样返回
UiText.fmt("共%1\$s张图片", count)            // 带占位符：原先是 "共" + count + "张图片"
```

- `of(raw)`：`MAP[raw]` 命中且 `BiliTerminal.context != null` 时 `context.getString(key)`，否则**原样返回**。所以「还没登记的文案」不会崩，只是暂时没进资源。
- `fmt(template, vararg args)`：先 `of(template)` 取资源值，再 `String.format`（资源值里没有 `%` 时原样返回）。**为什么单独起名 `fmt` 而不是重载 `of`**：两个 `of` 重载会让 `UiText.of("x")` 出现重载歧义（坑 22）。

**工具**（都在 `build/`，可重跑）：

| 脚本 | 作用 |
| --- | --- |
| `ui-migrate3.ps1` | 按调用形态替换：`MsgUtil.showMsg` / `showMsgLong` / `setPageName` / `setStatusText` |
| `wrap-rest.ps1` | 兜底：把其余中文字面量包成 `UiText.of(...)`（跳过 `const val`、比较、`when` 键、日志） |
| `wrap3.ps1` | 同上，但允许 `\n` 等转义 |
| `register.ps1` / `register2.ps1` | 扫 `UiText.of("…")` 的实参，登记键与映射（`register2` 会先解码再编码，见坑 24） |
| `shape-fix.ps1` | 处理 `MsgUtil.err("标题", e)`、`.add("文案" to { … })`、`arrayOf/listOf` 等特殊形态 |

**结果**：`strings.xml` 从 1125 条涨到 **1795** 条；`UiText` 映射 **815** 条；`UiText.of/fmt` 调用 **935** 处。`assembleDebug` 通过、两个 flavor 单测各 601 条全过。

**有意没动的**（详见 §4）：`const val`（编译期常量）、`==` / `!=` 这类**当键用**的比较、`api/` 的错误码→人话映射（`ApiErrorText.kt`）、`model/` 的数据字段、`activity/settings`（走 `SettingText`）。

## 5.10 B11 明细（已完成：布局 XML 与 Manifest）

**布局**：93 个布局里 **332 处** `android:text` / `android:hint` / `android:contentDescription` 的中文改为 `@string/*`，新增 **218** 个键，命名 `text_<布局名去下划线>_NN` / `hint_…` / `a11y_…`；已存在的同值自动复用旧键。5 条带 `\n` 的手工处理（`text_crash_reason`、`text_upload_avatar_hint`、`text_about_test_version`、`text_net_tool_intro`、`text_announcement_poem_placeholder`）。

**Manifest**：103 个 `android:label="中文"` 改为 `@string/*`，其中 **79 个复用既有键**（绝大多数本来就是 `pagename_*`），新增 24 个 `label_activity_no_NN`。

**有意不迁**：`res/xml/tutorial_*.xml`（旧教程链路，正文已在 `Tutorials.kt`，见 §4）、`res/drawable/*.xml` 里 `<path>` 附近的注释、所有 `tools:text`（设计占位符）。

## 6. 每批的标准动作（照做即可）


1. **挑一批**：从上表的表里选，范围控制在「一个屏幕 / 一个模块」，改动文件 ≤ 8 个。
2. **先查重**：拿句子去 `DUPLICATES.md` 搜一遍，有同义的就复用键，别新开。
3. **加键**：追加在 `strings.xml` 末尾，带分组注释，键名按第 1 节规范。
4. **改调用点**：Kotlin 用 `getString(...)`，布局用 `@string/...`。
5. **编译**：`./gradlew.bat :app:assembleStandardDebug --offline`。**这一步必做**——布局引用写错、`getString` 放错作用域都只有编译能发现。
6. **补单测**（可选）：如果迁的是纯函数里的格式串，补一个断言文案的用例。
7. **记台账**：本文件第 0 节打勾、第 5 节样式加一节明细；`AGENTS.md` 的约定已于 26.10.19 改成「用户可见文案一律进 `strings.xml`」，无需再动。
8. **单元测试**：`:app:testStandardDebugUnitTest`（改了纯函数才需要）。

---

## 7. 已知坑

1. **键名撞车**：追加前先 grep 键名。`strings.xml` 里重名会让 aapt2 直接报错（`Duplicate resources`），这个不危险，危险的是**值重复但键不同**——那正是我们要消灭的。
2. **`strings.xml` 里的裸文本不是注释**：`:16` 的 `# 旧的免责声明`、`:349` 的 `#本次版本更新日志…` 都是**会显示给用户的裸文本**（AAPT 容忍它在 `<resources>` 里）。往文件里加东西时别以为它们是注释，也不要顺手删。
3. **转义**：`'` 要写 `\'`（如 `Don\'t`）；`@` 开头的值要写 `\@`；`<` `>` 用 CDATA 包起来；`&` 写 `&amp;`。
4. **`\n` 是换行**：多行文案写一个 `\n`，不要真的折行——折行会带进缩进空格。
5. **一次别改太多**：一批不超过 ~40 处替换。改多了编译报一堆错时很难定位是哪个键。
6. **改教程正文要加版本号**：`Tutorials.kt` 里每篇有 `version`，`TutorialStore.readVersion(id) >= tutorial.version` 才算已读。改正文不加 `version`，老用户永远看不到新版本。
7. **涉及隐私承诺的三处要同步**：`about_to_uncle`、`text_setup_introduction`、`privacy_consent_message`，改一处必须三处一起改，且 `api/TerminalApi.java:85-86` 的 `PRIVACY_VERSION` 要 +1（见 `GUIDE.md` 红线 5）。
8. **本仓库的文件行尾不统一**：`strings.xml` 是 **CRLF**，而 Kotlin 与布局文件是 **LF**。用脚本做「整块删行」的替换时，`\r\n` 写死会让匹配直接 0 命中（本次踩过：删 `RefreshListActivity` 的两个 `const val` 时先报 `OCC=0`）。要么只用单行匹配，要么先 `[System.IO.File]::ReadAllText` 判一下该文件是 CRLF 还是 LF。
9. **别用 `Select-String -Path 'app\src\main\java\**\*.kt'` 搜代码**：PowerShell 5.1 的 `-Path` 不展开 `**`，会静默返回 0 命中，让你误判「这个键没人用」。用 `grep` 工具，或 `Get-ChildItem -Recurse | Select-String`。（本次因此差点把「没人引用」的结论写错。）
10. **`companion object` / `@JvmStatic` 里没有 `getString`**：`DownloadService` 的 `start(Long)` 系列门面都在伴生对象里，直接写 `getString(...)` 会报 `Unresolved reference 'getString'`。改成 `BiliTerminal.context!!.getString(...)`——注意 `BiliTerminal.context` 的类型是 **`Context?`**，不加 `!!` 会报 `Only safe (?.) or non-null asserted (!!.) calls are allowed`。同理适用于任何 `object` 与静态工具函数。
11. **改了 Kotlin 却忘了 `import`**：`R` 在 `com.RobinNotBad.BiliClient` 包下，而 `service/` 子包不会自动可见，得手动 `import com.RobinNotBad.BiliClient.R`。同理 `DownloadService.kt` 原本一句 `R.string` 都没有，所以根本没有这条 import。
12. **改键名 / 删键前必须全仓 grep 引用**：本次 B4 编译失败就是因为 `.setContentTitle(service.getString(R.string.text_downloading_video))` 引用了一个不存在的键（`text_downloading_video`），而正确键是 `text_download_video`。`strings.xml` 只保证「键不重名」，**不保证「没人引用已删的键」**——删/改名前用 grep 工具搜 `R.string.<键名>` 与 `@string/<键名>`，确认 0 命中。
13. **文档里的示例键名也会被照抄**：§1 规范表原来拿 `text_downloading_video` 当例子，代码里就真的出现了这个不存在的键。写文档时的举例一律用**真实存在的键**，否则下一个人（或下一个 agent）会照着抄进代码。
14. **Kotlin 文件的 `package` 行没有分号，Java 有**：用一条正则同时给两种文件插 import 会静默失败。Kotlin 用 `(?m)^(package [^\r\n]+)\r?$`，Java 用 `(?m)^(package [^\r\n]+;\s*)$`。`[regex]::Replace` 没匹配到**不会报错**，只会原样返回——所以「插 import」之后一定要回读前几行确认，否则要等编译报 `Unresolved reference 'R'` 才发现（本次 B8 就这么踩了一次）。
15. **Kotlin 块注释会嵌套，注释里别写 `/*`**：`ApiErrorText.kt` 的 KDoc 里写了 `app/src/test/.../api/*Test.kt`，其中 `/*` 开启了**内层注释**，于是后面的 `*/` 只关掉了内层、外层一直没闭合，编译器报的是 `ApiErrorText.kt:27:1 Syntax error: Unclosed comment.`，而**连带**报了十几条 `Unresolved reference 'ApiErrorText'`（因为整个 object 都没解析出来）。排查时先看**第一条** `e:`，别被连带错误带偏；写文档注释时通配符写成 `xxx Test.kt` 或去掉斜杠。
16. **PowerShell 字符串 `.Replace()` 会把 `System.Char` 当字符替换**：想批量替换句子时若把 `@('旧','新')` 解构成了两个字符串再取 `$pair[0]`，拿到的是**首字符**（`[char]`），于是 `.Replace('c', '长长的一整句')` 会把文件里每个 `c` 都换掉。本次因此把 `OpusContentAdapter.kt` / `VideoInfoFragment.kt` / `UserDynamicAdapter.kt` 三个文件写坏（出现 `oontext`、`aar msg`），最后用 `git checkout --` 还原这三个文件再逐条手改。**教训：批量替换用 `[string]` 变量前先 `.GetType().Name` 确认；改坏了立刻 `git checkout -- <file>`（这些文件当时只改了一行，还原成本极低）。**
17. **行尾归一化会被当成整文件重写**：本次 `api/*.java` 四个文件在**工作区是 CRLF、索引里是 LF**，且被 git 判成 `w/-text`（二进制），于是 `git diff` 把 1133 行全算成改动。合并前务必确认 diff 干净：`git diff --numstat` 与 `git diff -w --numstat` 数量差得远，就是行尾抖动；用 `[System.IO.File]::WriteAllText` 写 UTF-8 无 BOM 并把 `\r\n` 统一成 `\n` 即可。判断当前状态：`git ls-files --eol -- <file>`。
18. **中文 `.ps1` 脚本必须存成「UTF-8 带 BOM」**：Windows PowerShell 5.1 读**没有 BOM** 的 `.ps1` 时按 GBK 解码，注释里的中文会变成乱码字节，进而把 `'` 和 `"` 的配对搞坏，报一堆 `必须在"*"运算符后面提供一个值表达式` / `方法调用中缺少")"`。解法：写完后 `[System.IO.File]::WriteAllText($p, $t, (New-Object System.Text.UTF8Encoding($true)))` 补 BOM 再执行。（本次 B9b 第一次跑就整篇解析失败。）
19. **设置页这种「名字即协议」的模块，用「字面量当键 + 显示层 resolve」而不是改调用点**：`SettingSection` / `SettingsIndex.Entry` 的 name 靠逐字比对定位，把字面量换成 `R.string.*` 会让搜索跳转静默失灵。正确做法见 5.8：一张 `SettingText` 映射表 + 4 个显示点，覆盖全部 386 条。
21. **含 `$` / `${…}` 的模板字符串不能整段包 `UiText.of`**：正则一旦把 `"合集 · ${collectionTitle ?: "本地视频"}（…）"` 里的第一个引号配对成一段，就会把内部插值的引号吃掉，编译报一堆 `Syntax error` / `Unresolved reference`（本次一次弄坏 83 处）。批量工具的字面量字符集里**必须排除 `$`**；确需迁移拼接句时改写成 `UiText.fmt("…%1$s…", 变量)`。
22. **`of` 不要做重载**：`of(raw)` 和 `of(raw, vararg args)` 同时存在时，`UiText.of("x")` 会报 `Overload resolution ambiguity`（本次引发 2000+ 个连带错误）。带占位符的那个叫 `fmt`。
23. **映射表的键必须是「运行时字符串」**：`strings.xml` 里 `\n` 写单反斜杠（Android 转义），Kotlin 映射表的键也要写单反斜杠（那是真的换行符）——写成 `\\n` 就永远匹配不上；`$` 在 Kotlin 源里要写 `\$`。
24. **登记键要走「解码→再编码」，不能直接搬源码字面量**：直接把源码里的 `"登录信息\\n缺失"` 抄进映射表会得到字面反斜杠 n，编译期还会因为 `%1\$s` 变成 `%1\\$s` 报 `Unresolved reference 's'`（本次 39 条）。用 `build/register2.ps1`。
25. **给 `.java` 批量插 import 要带分号**：`import com.RobinNotBad.BiliClient.util.UiText`（Kotlin 写法）进 Java 会报 `错误: 需要';'`。Kotlin 与 Java 用两条正则，别复用。
26. **统计「还剩多少文案」时别把行尾注释算进去**：`x = "" // 中文说明` 这种行会被 `"…中文"` 正则误命中，把剩余量估高数倍。判据是「中文出现在字符串字面量里」，不是「这行有中文」。
27. **改完必须回读 diff 的行数**：`git diff --numstat` 出现某文件几百行改动而 `git diff -w --numstat` 只有几行，就是行尾抖动（见坑 17）。本次布局/Manifest 批量改完后仍保持「每文件 1~N 行」，没有整文件重写。

---

## 8. 迁移完成后要改的文档

- ✅ `AGENTS.md`：「文案硬编码中文，只有设置页用 `strings.xml`」→ 已于 26.10.19 改成「用户可见文案一律进 `strings.xml`」，并补了「纯函数例外走 `api/ApiErrorText.kt`」。
- `GUIDE.md` 第一节（「同一句话可能有三份」）→ 改成「历史遗留已清理，新文案只进 `strings.xml`」，并把 `DUPLICATES.md` 标为归档。
- 七份清单里的「位置」列不必重写——它们记录的是**迁移前的快照**，已在 `README.md` 说明；改写时仍按 `GUIDE.md` 的流程走。

---

## 9. 迁移完成后的现状（26.10.19）

**一句话**：用户可见文案现在只有一个来源——`app/src/main/res/values/strings.xml`。

| 项 | 数值 |
| --- | --- |
| `strings.xml` 的 `<string>` | 1719 条（另有 5 个 `<string-array>`） |
| 其中 `uitext_*` 键 | 708 条 |
| `util/UiText.kt` 映射表 | 757 条（`字面量 → R.string.*`） |
| `UiText.of` / `UiText.fmt` 调用点 | 1028 处 |
| 布局 XML 走 `@string/*` | 332 处 |
| `AndroidManifest.xml` 的 `android:label` | 103 个全部走 `@string/*` |
| 本轮改动规模 | 271 个文件，+3487 / −1725 行 |
| 验证 | `:app:assembleDebug`、`:app:testStandardDebugUnitTest`、`:app:testCoexistDebugUnitTest` 全部通过 |

**以后改一句文案怎么做**：

1. 普通界面文字（Kotlin 里是 `UiText.of("…")` 或 `getString(R.string.…)`）→ 只改 `strings.xml` 里对应那条的值。
2. 设置页（`activity/settings/`）→ 代码里的中文是**键**，改 `strings.xml` 里 `settingtext_*` 的值即可，**别改代码里的键**（改名会破坏搜索跳转的逐字比对，见坑 19）。
3. 布局上的静态文字（`@string/text_*`、`@string/hint_*`、`@string/label_*`）→ 改 `strings.xml`。
4. 教程正文 → 改 `strings.xml` 里对应 `uitext_*` 的值，**并且把 `Tutorials.kt` 里那篇的 `version` 加 1**，否则老用户看不到更新（坑 6）。
5. 更新日志 / 关于 / 隐私 / 彩蛋 → 本来就在 `strings.xml`（`update_log_current`、`update_history_log`、`about_*`、`privacy_*`、`eggs`），按 `GUIDE.md` 的红线改（隐私三处要同步 + `PRIVACY_VERSION` +1）。

**刻意留在代码里的中文**（不是漏迁，见 §4）：

- `const val`、`when`/`==` 里**当键用**的比较、`Map` 的 key；
- `api/` 的「错误码 → 人话」映射（在 `api/ApiErrorText.kt`，因为要被 JVM 单测直接调用）；
- `Log.` / `Logu.` / 异常文本、注释、`tools:text`；
- `res/xml/tutorial_*.xml`（旧教程链路，正文已迁到 `Tutorials.kt` → `strings.xml`）。

**已知的小尾巴**：`UiText` 映射表里若某条实参在代码里已不存在，就是死条目；`build/register2.ps1` 重跑会重新对齐。本轮已清掉 58 条死映射与 76 个死键。