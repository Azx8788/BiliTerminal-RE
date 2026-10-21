<!-- markdownlint-disable MD004 MD032 -->
<!-- 本文件逐字照抄用户可见文案（素材而非正文）：行首的 `*`、`+`、`##` 与引用块内的列表都是内容，不是 Markdown 格式，故豁免上面这几条结构规则。 -->

# 05 · 页面与布局静态文案

> 收录范围：所有 Activity / Fragment 的**常驻界面文案**——页面标题、按钮、栏目标签、小节标题、输入框提示语、列表条与空态、列表项布局里的静态标签。来源是 `app/src/main/res/layout/*.xml` 的 `android:text` / `android:hint` / `android:contentDescription`，以及 `app/src/main/java/**` 里**硬编码**的 `setPageName(...)`。
> 编号前缀：`U`
> 生成：2026-10-19 · 基于当前工作区
> 计数：共 260 条（U-001 ~ U-260）
> **改写入口**：每条最后的 `- **改写**：` 里**已经预填了原文**（2026-10-19 起），你直接在原地改成新文案，再把该条标题加上 `〔待落地〕`；怎么配合见 [GUIDE.md](GUIDE.md) 第十一节。

---

## 〇、这份收什么、不收什么

### 0.1 与其它几份的分工

本次文案清单共 7 份，编号前缀互不冲突。**05 与 04 的碰撞最多**，改写前先看这张表，避免同一句话改两遍、或者以为漏了其实在别处。

| 文件 | 前缀 | 管什么 | 与 05 的边界 |
| --- | --- | --- | --- |
| `01-app-identity.md` | `A` | 应用身份 / 关于页 / 开源信息 / 免责声明 / 首次启动引导 / 隐私与统计 / 反馈 / 捐赠 / 特殊登录 / 调试彩蛋功能 | 05 **不再收** `activity_setting_about.xml`、`activity_setting_opensource.xml`、`activity_setup_introduction.xml`、`activity_feedback*.xml`、`activity_login_special.xml` 的文案，遇到时只写一句交叉引用 |
| `02-update-log.md` | `L` | 更新日志正文 / 检查更新页 / 强制更新 | 05 **不再收** `activity_update.xml`、`activity_update_history.xml` |
| `03-tutorials.md` | `T` | 教程正文与教程页全套 | 05 **不再收** `activity_tutorial.xml`、`activity_tutorial_pager.xml`、`activity_tutorial_manager.xml` |
| `04-settings.md` | `S` | 设置主页与全部设置子页面：一级/二级分组名与说明、每个设置条目的名称与说明、候选值、设置搜索词、**菜单设置页的 14 个菜单项名**、**我的页面设置页的 11 个入口名** | 05 **不再收**设置子页面标题、设置条目名、菜单项名、我的页面入口名——这些都在 04 的具体编号里（见下） |
| `06-messages-toasts.md` | `M` | Toast / `TerminalDialog` 弹窗 / 整页提示 / 错误翻译 / 通知栏 | 05 只收**常驻**文案；一次性的提示、弹窗按钮、通知标题都归 06 |
| `07-eggs.md` | `E` | 彩蛋（设置页长按「关于」、关于页连点、搜索关键词） | 无重叠 |

**04 已经收过的、05 不要重复写的具体编号**（本节所有交叉引用都指向这一批）：

- 设置子页面标题：`S-021`「选择播放器」、`S-033`「请选择清晰度」、`S-035`「内置播放器设置」、`S-139`「外观设置」、`S-158` 界面预览页标题、`S-171`「菜单设置」、`S-191`「我的页面设置」、`S-206`「详情页设置」、`S-221`「评论区设置」、`S-228`「偏好设置」、`S-001` 设置页标题、`S-002` 设置搜索框提示语。
- 菜单设置页：`S-172`/`S-173` 分区标题「已启用」「未启用」、`S-174` 角标「固定」、`S-176` 页面底部操作说明。
- **`S-177`~`S-190`＝14 个菜单项名**（推荐 / 短视频 / 热门 / 入站必刷 / 全站排行榜 / 热搜 / 直播 / 时间线 / 搜索 / 动态 / 我的 / 消息 / 缓存 / 设置）。这批文案的**唯一来源是 `activity/MenuActivity.kt:46-59` 的 `btnNames`**，05 在第四节点名位置、不复述文字。
- 我的页面设置页：`S-192`/`S-193` 分区标题、`S-194` 页面底部说明。
- **`S-195`~`S-205`＝11 个我的页面入口名**（关注 / 稍后再看 / 收藏 / 追番列表 / 历史记录 / 创作中心 / 大会员 / 登录记录 / 硬币变化记录 / 经验变化记录 / 编辑个人资料）。唯一来源是 `activity/user/MySpaceMenu.kt:19-31` 的 `ITEMS`。

### 0.2 三种「看着像文案、其实不该改」的文本

1. **`tools:` 命名空间属性**：`tools:text` / `tools:visibility` 只在 Android Studio 布局预览里生效，**不进 APK**。本文件不收。例如 `activity_simple_refresh.xml:249` 的 `tools:text="正在加载…"`——真正在用的「正在加载…」在代码里（见第三节）。
2. **运行时必然被 adapter 覆盖的示例文本**：`cell_*.xml` 是列表项的**布局模板**，绝大多数 `android:text` 只是给预览看的示例值，真值由 adapter 填。这些统一收在第十四节并标注「不是文案，别改」。
3. **设计占位符**：`文本文本文本`、`标题标题标题…`、`蔡徐坤`、`只因你太美！`、`114514条`、`P1 标题`、`用户名`、`分区标题`、`选项文字`……这是作者写布局时随手填的梗或占位，同样在第十四节单列。

### 0.3 页面标题有两个来源，改的时候别只改一处

1. **布局里**已经写了一行标题（`android:text="@string/pagename_*"` 或硬编码），如 `activity_message.xml:24` 写了 `@string/pagename_message`；
2. **代码里**再调一次 `BaseActivity.setPageName("…")` 覆盖它，如 `MessageActivity` 走的是同一句 `pagename_message`，而 `OpusInfoActivity:41` 是硬编码 `"文章详情"`。

`setPageName` 定义在 `activity/base/BaseActivity.kt:199`。**只改布局不改代码 = 白改**（代码后执行）；**只改代码不改布局 = 顶栏闪一下**。第一节收的全部是代码侧硬编码的那一半。

### 0.4 只有 4 处中文 `contentDescription`

无障碍朗读用的描述文本全工程只有 4 处是中文，其余都是 `@string` 或英文类名：`activity_loading.xml:46`「加载中」、`cell_loading.xml:9`「加载中」、`cell_follow_group.xml:46`「展开/折叠」、`fragment_qr_login.xml:57`「登录二维码」。它们分别在第 二、十二、十五节。

### 0.5 本文件里表格与条目的区别

- **表格**用于成组、彼此平行的短标签（页面标题、`hint_*`、`btn_*`、列表项静态标签）。表格里的「改写」列也是留给你的空位。
- **`###` 条目**用于需要交代上下文的长文案（整句说明、底部说明文字、按钮组）。格式与其它几份一致：位置 / 出现在哪 / 用途 / 原文 / 改写。

---

## 一、页面标题（代码里硬编码 `setPageName`）

页面标题是用户在手表上**唯一稳定的方位感来源**——手表顶栏空间极小，标题一旦含糊（如「详情」），用户退出后完全想不起自己在哪。以下 58 处全部是 `setPageName("硬编码中文")`，与 `strings.xml` 无关，**改文案要动代码**。

| 编号 | 位置 | 原文 | 出现在哪 / 用途 | 改写 |
| --- | --- | --- | --- | --- |
| U-001 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/article/OpusInfoActivity.kt:41` | 文章详情 | 从动态、收藏、搜索进入图文（专栏）详情页时的顶栏标题。**类名与文案不一致**：B 站已把专栏统一叫「图文」，这里仍写「文章详情」 | 文章详情 |
| U-002 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/InteractionDebugActivity.kt:28` | 互动视频变量调试 | 互动视频播放中长按调试按钮进入；仅调试包可见 | 互动视频变量调试 |
| U-003 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/dynamic/DynamicActivity.kt:226` | 动态 | 底部菜单进动态页 | 动态 |
| U-004 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/feedback/FeedbackDetailActivity.kt:73` | 反馈详情 | 我的反馈列表点进某条反馈（文案归属 01） | 反馈详情 |
| U-005 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/feedback/FeedbackListActivity.kt:50` | 我的反馈 | 反馈页进入历史反馈列表（文案归属 01） | 我的反馈 |
| U-006 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/dynamic/DynamicTopicActivity.kt:41` | 话题 | 从动态里的话题标签点进；写的是 `topicName.ifEmpty { "话题" }`，即**话题名为空时的兜底文案** | 话题 |
| U-007 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/dynamic/DynamicTopicActivity.kt:44` | 话题广场 | 发动态页的「话题广场」入口 | 话题广场 |
| U-008 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/live/RecommendLiveActivity.kt:20` | 推荐直播 | 菜单点「直播」进的是推荐直播 | 推荐直播 |
| U-009 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/live/FollowLiveActivity.kt:20` | 我关注的直播 | 直播页切到我关注的 | 我关注的直播 |
| U-010 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/vote/VoteInfoActivity.kt:68` | 投票详情 | 动态里的投票卡点进 | 投票详情 |
| U-011 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/note/NoteActivity.kt:45` | 笔记 | 视频详情页底部「笔记」入口 | 笔记 |
| U-012 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/search/SearchActivity.kt:326` | 搜索-$sub | 搜索页切到「专栏 / 用户 / 直播 / 番剧」等类别时，`$sub` 是类别名 | 搜索-$sub |
| U-013 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/CoinLogActivity.kt:29` | 硬币变化记录 | 我的页面 → 硬币变化记录（**入口名**在 04 的 `S-203`） | 硬币变化记录 |
| U-014 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/message/NoticeActivity.kt:22` | 详情 | 消息页点系统通知/回复进详情。**只写「详情」是全文案里最模糊的一处标题**，用户不知道自己在看什么的详情 | 详情 |
| U-015 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/message/MessageSettingsActivity.kt:35` | 消息设置 | 消息页右上角齿轮 | 消息设置 |
| U-016 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/reply/ReplyInfoActivity.kt:65` | 评论详情 | 点某条评论进详情（含楼中楼） | 评论详情 |
| U-017 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/WatchLaterActivity.kt:37` | 稍后再看 | 我的页面 → 稍后再看 | 稍后再看 |
| U-018 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/TimelineActivity.kt:29` | 时间线 | 菜单 → 时间线 | 时间线 |
| U-019 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/OpenSourceActivity.kt:24` | 开源信息 | 关于页 → 开源信息（文案归属 01） | 开源信息 |
| U-020 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/AnnouncementsActivity.kt:16` | 公告列表 | 设置 → 关于与帮助 → 公告列表 | 公告列表 |
| U-021 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/MySpaceMoreActivity.kt:31` | 更多 | 我的页面点「更多」进的列表页 | 更多 |
| U-022 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/VideoInfoActivity.kt:63` | 番剧详情 | 番剧（bangumi）分支 | 番剧详情 |
| U-023 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/VideoInfoActivity.kt:86` | 视频详情 | 普通视频分支 | 视频详情 |
| U-024 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/VideoInfoActivity.kt:141` | $mainTitle | 拿到视频主标题后用它替换标题 | $mainTitle |
| U-025 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/VideoInfoActivity.kt:149` | $mainTitle-$sub | 切到某个分P后 | $mainTitle-$sub |
| U-026 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/UpdateHistoryActivity.kt:131` | 历史更新日志 | 关于页 → 历史更新日志（文案归属 02） | 历史更新日志 |
| U-027 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/QRLoginHelpActivity.kt:15` | 登录方式说明 | 扫码登录页的「登录方式说明」 | 登录方式说明 |
| U-028 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/CaptchaWebViewActivity.kt:65` | 人机验证 | 登录触发滑块/点选验证时拉起 | 人机验证 |
| U-029 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/LoginRecordActivity.kt:30` | 登录记录 | 我的页面 → 登录记录（**入口名**在 04 的 `S-202`） | 登录记录 |
| U-030 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/series/UserSeriesActivity.kt:22` | 投稿的系列 | 用户主页 → 投稿的系列 | 投稿的系列 |
| U-031 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SponsorActivity.kt:20` | 捐赠列表 | 关于页 → 捐赠列表（文案归属 01） | 捐赠列表 |
| U-032 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/HotSearchActivity.kt:20` | 热搜 | 菜单 → 热搜 | 热搜 |
| U-033 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/TutorialManagerActivity.kt:39` | 教程管理 | 关于页 → 教程管理（文案归属 03） | 教程管理 |
| U-034 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/AddFavoriteActivity.kt:26` | 添加收藏 | 视频详情页「收藏」后选收藏夹 | 添加收藏 |
| U-035 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/series/SeriesInfoActivity.kt:51` | $seriesName | 合集/系列详情，标题直接是系列名 | $seriesName |
| U-036 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/HistoryActivity.kt:25` | 历史记录 | 我的页面 → 历史记录（**入口名**在 04 的 `S-199`） | 历史记录 |
| U-037 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/FollowUsersActivity.kt:57` | 关注列表 / 粉丝列表 | 同一个页面按 `mode` 二选一：`if (mode == 0) "关注列表" else "粉丝列表"` | 关注列表 / 粉丝列表 |
| U-038 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/RecommendActivity.kt:33` | 推荐 | 菜单首页。注意菜单点进来时 `MenuActivity:79` 已用 `btnNames` 的「推荐」设过一次，这里是**同一句话的第二个定义点** | 推荐 |
| U-039 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/ExpLogActivity.kt:29` | 经验变化记录 | 我的页面 → 经验变化记录（**入口名**在 04 的 `S-204`） | 经验变化记录 |
| U-040 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/FollowingBangumisActivity.kt:20` | 追番列表 | 我的页面 → 追番列表（**入口名**在 04 的 `S-198`） | 追番列表 |
| U-041 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/local/DownloadListActivity.kt:53` | 下载列表 | 下载管理页（缓存页的「下载中」入口） | 下载列表 |
| U-042 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/video/collection/CollectionInfoActivity.kt:38` | 合集详情 | 视频卡片的合集标记点进 | 合集详情 |
| U-043 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/favorite/FavoriteFolderCreateActivity.kt:32` | 创建收藏夹 | 收藏列表页「+ 创建收藏夹」 | 创建收藏夹 |
| U-044 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/info/UserInfoActivity.kt:30` | 用户信息 | 我的页面顶部用户卡片点进自己 | 用户信息 |
| U-045 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/info/UserInfoActivity.kt:77` | 用户信息 / 用户信息-$sub | 看别人的主页写「用户信息」；切换子页（动态/投稿等）时写「用户信息-$sub」 | 用户信息 / 用户信息-$sub |
| U-046 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/favorite/FavoriteFolderListActivity.kt:25` | 收藏 | 我的页面 → 收藏 | 收藏 |
| U-047 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/favorite/FavoriteVideoListActivity.kt:68` | $folderName | 收藏夹内容页，标题就是收藏夹名 | $folderName |
| U-048 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/favorite/FavouriteOpusListActivity.kt:18` | 图文收藏夹 | 收藏的图文（专栏）列表 | 图文收藏夹 |
| U-049 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/MenuActivity.kt:79` | $btnNames[from] | 从子页面返回菜单时，用菜单项名恢复标题；文字本体在 `MenuActivity.kt:46-59`（04 的 `S-177`~`S-190`） | $btnNames[from] |
| U-050 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingQualityActivity.kt:36` | 请选择清晰度 | 设置 → 播放与播放器 → 清晰度（04 的 `S-033`） | 请选择清晰度 |
| U-051 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SearchSortActivity.kt:43` | 搜索类别排序 | 搜索设置页 → 调整搜索类别排序 | 搜索类别排序 |
| U-052 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingInfoActivity.kt:17` | 详情页设置 | 设置 → 内容与浏览 → 详情页设置（04 的 `S-206`） | 详情页设置 |
| U-053 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingTerminalPlayerActivity.kt:50` | 内置播放器设置 | 长按播放器卡片进入（04 的 `S-035`） | 内置播放器设置 |
| U-054 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingPrefActivity.kt:20` | 偏好设置 | 设置 → 内容与浏览 → 通用偏好（04 的 `S-228`） | 偏好设置 |
| U-055 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingMySpaceActivity.kt:28` | 我的页面设置 | 设置 → 内容与浏览 → 我的页面设置（04 的 `S-191`） | 我的页面设置 |
| U-056 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/SettingRepliesActivity.kt:17` | 评论区设置 | 设置 → 内容与浏览 → 评论区设置（04 的 `S-221`） | 评论区设置 |
| U-057 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/LoginActivity.kt:19` | 登录 | 未登录时点菜单顶部「登录」按钮进的页 | 登录 |
| U-058 | `app/src/main/java/com/RobinNotBad/BiliClient/activity/user/MedalWallActivity.kt:33` | 粉丝勋章 | 用户主页 → 查看粉丝勋章 | 粉丝勋章 |

**改写这批标题时的约束**：手表顶栏一行大约放得下 6~8 个汉字，超过就会被截断成「…」。「全站排行榜」「硬币变化记录」「互动视频变量调试」都已在临界线附近，**改写只能更短不能更长**。

---

## 二、页面标题（`strings.xml` 的 `pagename_*`）

这一组是上表那批的「另一条路」：定义在 `app/src/main/res/values/strings.xml`，由布局文件引用。全部 36 条列在下面，其中 **3 条已经没有任何引用**（第十五节会再点一次名）。「引用位置」列就是**唯一的投放点**，改完不需要动代码。

| 编号 | 定义位置 | 原文 | 引用位置 | 备注 | 改写 |
| --- | --- | --- | --- | --- | --- |
| U-059 | `app/src/main/res/values/strings.xml:10` | 开源信息 | `app/src/main/res/layout/activity_setting_opensource.xml:28` | 文案归属 01 | 开源信息 |
| U-060 | `app/src/main/res/values/strings.xml:229` | 页面标题 | `activity_simple_viewpager.xml:24`、`activity_simple_text.xml:24`、`activity_simple_refresh.xml:25`、`activity_simple_main_refresh.xml:24`、`activity_simple_list.xml:24` | **纯占位**：5 个通用列表基类布局都写它，代码必然调 `setPageName` 覆盖 | 页面标题 |
| U-061 | `app/src/main/res/values/strings.xml:230` | 加载中…… | `app/src/main/res/layout/activity_loading.xml:24` | 通用加载页 | 加载中…… |
| U-062 | `app/src/main/res/values/strings.xml:231` | 哔哩终端崩溃了 | `app/src/main/res/layout/activity_catch.xml:18` | 崩溃页（跑在 `:error_activity` 独立进程） | 哔哩终端崩溃了 |
| U-063 | `app/src/main/res/values/strings.xml:232` | 直播详情 | `app/src/main/res/layout/activity_live_info.xml:26` | | 直播详情 |
| U-064 | `app/src/main/res/values/strings.xml:233` | 复制 | `app/src/main/res/layout/activity_copy.xml:24` | | 复制 |
| U-065 | `app/src/main/res/values/strings.xml:234` | 创作中心 | `app/src/main/res/layout/activity_creative_center.xml:26` | | 创作中心 |
| U-066 | `app/src/main/res/values/strings.xml:235` | 提示 | `app/src/main/res/layout/activity_dialog.xml:20` | 通用整页提示（`MsgUtil.showDialog`），文案归 06 | 提示 |
| U-067 | `app/src/main/res/values/strings.xml:236` | 表情 | `app/src/main/res/layout/activity_emote.xml:27` | 表情选择页 | 表情 |
| U-068 | `app/src/main/res/values/strings.xml:237` | 图片查看 | `app/src/main/res/layout/activity_image_viewer.xml:26` | | 图片查看 |
| U-069 | `app/src/main/res/values/strings.xml:238` | 特殊登录 | `app/src/main/res/layout/activity_login_special.xml:26` | 文案归属 01 | 特殊登录 |
| U-070 | `app/src/main/res/values/strings.xml:239` | 账号切换 | `app/src/main/res/layout/activity_account_switch.xml:26` | | 账号切换 |
| U-071 | `app/src/main/res/values/strings.xml:240` | 菜单 | `app/src/main/res/layout/activity_menu.xml:26` | | 菜单 |
| U-072 | `app/src/main/res/values/strings.xml:241` | 消息 | `app/src/main/res/layout/activity_message.xml:24` | | 消息 |
| U-073 | `app/src/main/res/values/strings.xml:242` | 我的 | `app/src/main/res/layout/activity_myspace.xml:26` | 菜单项名同字，但那是 04 的 `S-187`（来源 `MenuActivity.btnNames`），两处独立 | 我的 |
| U-074 | `app/src/main/res/values/strings.xml:243` | 原小电视播放器 | `app/src/main/res/layout/activity_player.xml:158` | **在播放器画面里的顶栏**，不是系统顶栏；「原」字是相对内置播放器说的 | 原小电视播放器 |
| U-075 | `app/src/main/res/values/strings.xml:244` | 私信 | `app/src/main/res/layout/activity_private_msg.xml:32` | | 私信 |
| U-076 | `app/src/main/res/values/strings.xml:245` | 写评论 | `app/src/main/res/layout/activity_write_reply.xml:26` | | 写评论 |
| U-077 | `app/src/main/res/values/strings.xml:246` | 搜索 | `app/src/main/res/layout/activity_search.xml:105` | 注意这个 `TextView` 在 `:105`，而代码里 `SearchActivity:326` 会把它改成「搜索-$sub」 | 搜索 |
| U-078 | `app/src/main/res/values/strings.xml:247` | 发动态 | `app/src/main/res/layout/activity_send_dynamic.xml:26` | | 发动态 |
| U-079 | `app/src/main/res/values/strings.xml:248` | 关于 | `app/src/main/res/layout/activity_setting_about.xml:24` | 文案归属 01 | 关于 |
| U-080 | `app/src/main/res/values/strings.xml:249` | 设置 | `app/src/main/res/layout/activity_setting_main.xml:26` | 04 的 `S-001` | 设置 |
| U-081 | `app/src/main/res/values/strings.xml:250` | 菜单设置 | `app/src/main/res/layout/activity_setting_menu.xml:24` | 04 的 `S-171` | 菜单设置 |
| U-082 | `app/src/main/res/values/strings.xml:251` | 搜索设置 | `app/src/main/res/layout/activity_setting_search.xml:24` | 04 收录 | 搜索设置 |
| U-083 | `app/src/main/res/values/strings.xml:252` | 搜索类别排序 | **无人引用** | **死字符串**，见第十五节 | 搜索类别排序 |
| U-084 | `app/src/main/res/values/strings.xml:253` | 选择播放器 | `app/src/main/res/layout/activity_setting_player_choose.xml:26` | 04 的 `S-021` | 选择播放器 |
| U-085 | `app/src/main/res/values/strings.xml:254` | 界面设置 | **无人引用** | **死字符串**，见第十五节 | 界面设置 |
| U-086 | `app/src/main/res/values/strings.xml:255` | 界面大小预览 | `app/src/main/res/layout/activity_setting_ui_preview.xml:24` | 04 的 `S-158` | 界面大小预览 |
| U-087 | `app/src/main/res/values/strings.xml:256` | 初次见面…… | `app/src/main/res/layout/activity_setup_introduction.xml:25` | 文案归属 01 | 初次见面…… |
| U-088 | `app/src/main/res/values/strings.xml:257` | 调整界面…… | `app/src/main/res/layout/activity_setup_ui.xml:26` | 首次启动第二步 | 调整界面…… |
| U-089 | `app/src/main/res/values/strings.xml:258` | 正在连接…… | `app/src/main/res/layout/activity_splash.xml:17` | 开屏页；用户在设置里可以自定义开屏文字覆盖它（04 的 `S-131`） | 正在连接…… |
| U-090 | `app/src/main/res/values/strings.xml:259` | 测试页面 | `app/src/main/res/layout/activity_test.xml:24` | 仅调试包 | 测试页面 |
| U-091 | `app/src/main/res/values/strings.xml:260` | 教程 | `app/src/main/res/layout/activity_tutorial.xml:26` | 文案归属 03；该页属**待清理的旧链路** | 教程 |
| U-092 | `app/src/main/res/values/strings.xml:261` | 教程管理 | `app/src/main/res/layout/activity_tutorial_manager.xml:26` | 文案归属 03 | 教程管理 |
| U-093 | `app/src/main/res/values/strings.xml:266` | 版本更新 | **无人引用** | **死字符串**（检查更新页标题实际硬编码为「检查更新」，见 02） | 版本更新 |
| U-094 | `app/src/main/res/values/strings.xml:559` | 反馈与建议 | `app/src/main/res/layout/activity_feedback.xml:36`、`activity/FeedbackActivity.kt:63` | 文案归属 01 | 反馈与建议 |

---

## 三、列表底部状态条与空态

通用列表基类 `RefreshListActivity` / `RefreshMainActivity` 承担了全工程**几乎所有列表页**的空态与翻页提示。这四句话的曝光量是全应用最高的——每一个空列表都会看到它，改一句等于改遍全应用。

### U-095 空态：啥都木有~

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshListActivity.kt:25`（常量 `EMPTY_TEXT`）
- **出现在哪**：所有继承 `RefreshListActivity` / `RefreshMainActivity` 的列表页，在「加载成功但列表为空」时占据整个内容区中央
- **用途**：区分「没有内容」与「加载失败」——`:341` 的注释专门写明了这个动机：以前空态只有一句话，用户分不清是加载完了没东西还是网络挂了，所以失败时要换成下面那条带重试的
- **原文**：

> 啥都木有~

- **改写**：啥都木有~

### U-096 空态（可重试）：啥都木有~\n点我重试

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshListActivity.kt:26`（常量 `EMPTY_TEXT_WITH_RETRY`）
- **出现在哪**：列表**加载失败**时的空态（`:347` 赋值）；`RefreshMainActivity.kt:120` 也用它
- **用途**：把「失败」和「空」用文案分开，并给出唯一可行的动作（点一下重试）
- **原文**（`\n` 是真换行，两行居中显示）：

> 啥都木有~
> 点我重试

- **改写**：

> 啥都木有~
> 点我重试

### U-097 翻页到底：没有更多了

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshListActivity.kt:307`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshMainActivity.kt:80`
- **出现在哪**：列表底部 `loadMoreTip`（布局 `activity_simple_refresh.xml:241`，11sp 灰色居中，**在列表外**，不随列表滚走）
- **用途**：告诉用户「不用再往下滑了」
- **原文**：

> 没有更多了

- **改写**：没有更多了

### U-098 翻页中：正在加载…

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshListActivity.kt:308`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/base/RefreshMainActivity.kt:81`
- **出现在哪**：同上，列表底部状态条；两个基类各写了一份**同样的字符串**（改的时候别只改一处）
- **用途**：翻页时的进度反馈。**注意用的是省略号 `…` 而不是三个点**
- **原文**：

> 正在加载…

- **改写**：正在加载…

### U-099 布局里硬编码的同一句空态

- **位置**：`app/src/main/res/layout/activity_simple_main_refresh.xml:88`、`app/src/main/res/layout/activity_simple_refresh.xml:261`、`app/src/main/res/layout/fragment_simple_list.xml:25`、`app/src/main/res/layout/fragment_simple_refresh.xml:26`
- **出现在哪**：4 个基类布局里 `emptyTip` / `emptyView` 的初始 `android:text`，运行时会被 U-095/U-096 覆盖
- **用途**：布局预览与「代码还没来得及设值」时的兜底。**改 U-095 时必须同步改这 4 处**，否则极端时机会闪出旧文案
- **原文**：

> 啥都木有~

- **改写**：啥都木有~

---

## 四、菜单页与「我的」页面

菜单页（`activity/MenuActivity.kt`）和「我的」页面（`activity/user/MySpaceActivity.kt`）是手表上唯一两个**按钮墙**页面——它们不是一个布局写死的，而是代码按配置动态 `addView` 出来的。文案因此全在 Kotlin 里。

### U-100 菜单最顶部的「登录」按钮

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/MenuActivity.kt:99`（插入位置在 `:87` `btnList.add(0, "login")`）
- **出现在哪**：未登录时（`SharedPreferencesUtil.getLong(mid, 0) == 0L`）菜单列表的**第一项**，同时代码把「动态」「消息」「我的」三项从列表里摘掉
- **用途**：未登录时唯一可用的正向动作，用最显眼的位置引导登录
- **原文**：

> 登录

- **改写**：登录

### U-101 菜单按钮的未读角标格式

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/MenuActivity.kt:106`、`:116`（首次构建）、`:141`、`:149`（`onResume` 刷新）
- **出现在哪**：「动态」「消息」两个菜单按钮上，仅在 `DYNAMIC_UPDATE_NUM` / `MESSAGE_UPDATE_NUM` 大于 0 时
- **用途**：不做红点，直接把未读数拼进按钮文字
- **原文**（`$btnText` 是菜单项名，`$updateNum` 是未读数）：

> $btnText ($updateNum)

- **改写**（注意括号是**半角括号 + 一个空格**，改格式时两处 `onResume` 也要一起改）：

### U-102 14 个菜单项名

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/MenuActivity.kt:46-59`（`btnNames: Map<String, Pair<String, Class<out InstanceActivity>>>`）
- **出现在哪**：菜单页的全部按钮；同时是 `MenuActivity:79` 恢复顶栏标题、以及「我的页面设置」里入口名的来源
- **用途**：全应用最核心的导航文案
- **原文**（key → 文案，顺序即默认展示顺序）：

> `recommend` 推荐 ·
> `short_video` 短视频 ·
> `popular` 热门 ·
> `precious` 入站必刷 ·
> `ranking` 全站排行榜 ·
> `hotsearch` 热搜 ·
> `live` 直播 ·
> `timeline` 时间线 ·
> `search` 搜索 ·
> `dynamic` 动态 ·
> `myspace` 我的 ·
> `message` 消息 ·
> `local` 缓存 ·
> `settings` 设置

- **改写**：**这 14 条已在 `04-settings.md` 的 `S-177`~`S-190` 逐条收录，请改那一份**；本处只登记来源，避免两处不一致。需要注意 `util/MenuConfig.kt:14-24` 的 `ALL_ITEMS` / `FIXED_ITEMS` / `SWITCHABLE_KEYS` 只存 key 不存文案，改文案不涉及它。

### U-103 11 个「我的」页面入口名

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/user/MySpaceMenu.kt:19-31`（`object MySpaceMenu` 的 `val ITEMS: List<Item>`，`Item(key, iconRes, label)`）
- **出现在哪**：我的页面主列表 + 「更多」列表页，两处共用同一份定义
- **用途**：「我的」页面的全部功能入口文案
- **原文**：

> `follow` 关注 ·
> `watch_later` 稍后再看 ·
> `favorite` 收藏 ·
> `bangumi` 追番列表 ·
> `history` 历史记录 ·
> `creative` 创作中心 ·
> `vip` 大会员 ·
> `login_record` 登录记录 ·
> `coin_log` 硬币变化记录 ·
> `exp_log` 经验变化记录 ·
> `edit_profile` 编辑个人资料

- **改写**：**已在 `04-settings.md` 的 `S-195`~`S-205` 逐条收录，请改那一份**。约束：key 集合必须与 `util/MySpaceConfig.kt:18` 的 `ALL_ITEMS` 一致（`MySpaceConfigTest` 有守卫用例），**只能改 label，不能改 key**。

### U-104 「更多」与「退出登录」

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/user/MySpaceActivity.kt:91`（更多）、`:97`（退出登录）
- **出现在哪**：我的页面入口列表的最下方两条；「更多」只在真的有功能被移进更多列表时才渲染（`:74` 判 `layout.more.isNotEmpty()`），「退出登录」永远最后一位（`:75`）
- **用途**：「更多」是二级入口，「退出登录」是账号操作。退出登录那条**额外染成 `0xFFF44336` 红色**（`:98`），是全文案里唯一靠颜色表意的入口
- **原文**：

> 更多
> 退出登录

- **改写**：

> 更多
> 退出登录

### U-105 未登录时的用户卡片

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/user/MySpaceActivity.kt:166`
- **出现在哪**：我的页面顶部用户卡片的昵称位置，未登录时
- **用途**：把整张卡片变成一个登录入口
- **原文**：

> 点击登录

- **改写**：点击登录

### U-106 我的页面加载占位

- **位置**：`app/src/main/res/layout/activity_myspace.xml:89`
- **出现在哪**：我的页面数据未回来时
- **用途**：占位
- **原文**：

> 加载中...

- **改写**（**注意这是三个英文句点 `.`，不是 `…`**；全工程两种写法混用，见文末「覆盖性自查」第 5 条）：

---

## 五、消息页与私信页

### U-107 消息页四个分类名

- **位置**：`app/src/main/res/layout/activity_message.xml:105`（回复我的）、`:158`（`\@我`）、`:211`（收到的赞）、`:265`（系统通知）
- **出现在哪**：消息页顶部的四个分类按钮
- **用途**：消息分类导航。注意 `\@我` 里的反斜杠是**转义 `@`**，XML 里写的就是 `\@我`，Android 会把 `\@` 渲染成 `@`——**改写时不要把这个反斜杠去掉**（去掉后 aapt 可能报错或行为变化）
- **原文**：

> 回复我的
> \@我
> 收到的赞
> 系统通知

- **改写**：

> 回复我的
> \@我
> 收到的赞
> 系统通知

### U-108 消息页右侧两个入口

- **位置**：`app/src/main/res/layout/activity_message.xml:318`（消息设置）、`:333`（私信列表）
- **出现在哪**：消息页右上角/顶部动作位
- **用途**：进消息设置、进私信列表
- **原文**：

> 消息设置
> 私信列表

- **改写**：

> 消息设置
> 私信列表

### U-109 消息页分类名在代码里被二次赋值

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/message/MessageActivity.kt:68`、`:77`、`:142`
- **出现在哪**：同 U-107 的两个分类，`:68`/`:77` 直接赋同样的字，`:142` 则在文字后**拼接未读数**
- **用途**：与布局里的默认值形成「双真相」。**改 U-107 必须同步改这里**
- **原文**（`:142` 是拼接形式）：

> 回复我的
> 收到的赞
> 回复我的 + （未读数）

- **改写**：

> 回复我的
> 收到的赞
> 回复我的 + （未读数）

### U-110 消息设置页加载失败空态

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/message/MessageSettingsActivity.kt:64`、`:72`
- **出现在哪**：消息设置页列表加载失败时
- **用途**：失败提示（两条分支写了同一句话）
- **原文**：

> 加载失败，请重试

- **改写**：加载失败，请重试

### U-111 私信页的发图按钮

- **位置**：`app/src/main/res/layout/activity_private_msg.xml:83`
- **出现在哪**：私信页底部一个方形按钮，用来发图
- **用途**：控件太小放不下「图片」两个字，所以只写一个字
- **原文**：

> 图

- **改写**（**这一个字是全文案里信息量最低的**，但换成两个字会挤：要么保留单字、要么换成图标）：

### U-112 私信列表项占位

- **位置**：`app/src/main/res/layout/cell_private_msg.xml:15`
- **出现在哪**：私信列表项的用户名位置
- **用途**：布局预览占位，运行时被 adapter 覆盖。**不是文案**
- **原文**：

> 用户名

- **改写**：不需要改（占位）。若要改，见第十四节说明。

### U-113 消息回复卡片的示例正文

- **位置**：`app/src/main/res/layout/cell_message_reply.xml:26`
- **出现在哪**：消息页「回复我的」卡片里的回复正文位置
- **用途**：占位。但**这句是作者写的梗**（「全体目光向我看齐！看我看我，我宣布个事！」），运行时会被真实回复内容覆盖
- **原文**：

> 全体目光向我看齐！看我看我，我宣布个事！

- **改写**：不需要改（占位）

### U-114 消息卡片的跳转提示

- **位置**：`app/src/main/res/layout/cell_message_reply.xml:44`
- **出现在哪**：回复卡片右下角
- **用途**：提示这张卡可以点开。**这是真文案**，不会被覆盖
- **原文**：

> 点击跳转

- **改写**：点击跳转

---

## 六、视频详情、播放器、直播、番剧、短视频

### U-115 视频详情页「本软件疑似为AI创作」提示

- **位置**：`app/src/main/res/layout/fragment_video_info.xml:78`
- **出现在哪**：视频详情页顶部、UP 主信息之上的一条提示
- **用途**：**免责**——本项目代码大量由 AI 生成，这条提醒用户甄别视频内容本身。属于「幽默式免责」，改写时别把它改成严肃的法律声明（那是 01 的《免责声明》的职责）
- **原文**：

> 本软件疑似为AI创作，请注意甄别

- **改写**：本软件疑似为AI创作，请注意甄别

### U-116 视频详情页的加载中

- **位置**：`app/src/main/res/layout/fragment_video_info.xml:56`
- **出现在哪**：视频详情页数据未回来时
- **用途**：占位
- **原文**：

> 正在加载中...

- **改写**（同样是 `.` 而非 `…`）

### U-117 视频详情页合集入口

- **位置**：`app/src/main/res/layout/fragment_video_info.xml:113`
- **出现在哪**：视频详情页合集卡片
- **用途**：占位示例（「合集 · 纳西妲」），运行时换成真实合集名
- **原文**：

> 合集 · 纳西妲

- **改写**：不需要改（占位）

### U-118 视频详情页标签与简介占位

- **位置**：`app/src/main/res/layout/fragment_video_info.xml:178`（标签：???）、`:188`（简介……）
- **出现在哪**：视频详情页标签行与简介区
- **用途**：布局预览占位
- **原文**：

> 标签：???
> 简介简介简介简介简介简介简介

- **改写**：不需要改（占位）

### U-119 视频详情页底部动作按钮组

- **位置**：`app/src/main/res/layout/fragment_video_info.xml:199`（播放）、`:236`（点赞）、`:260`（投币）、`:284`（收藏）、`:303`（稍后再看）、`:311`（缓存）、`:328`（转发）、`:337`（视频摘要）、`:347`（笔记）
- **出现在哪**：视频详情页底部的九个动作按钮，手表上是一排小字按钮
- **用途**：详情页最主要的交互入口。这些按钮**文字会被代码按状态改**（如点赞后变色、`S-219` 的一键三连），但基础文案在这里
- **原文**：

> 播放 / 点赞 / 投币 / 收藏 / 稍后再看 / 缓存 / 转发 / 视频摘要 / 笔记

- **改写**（**每个按钮只有两三个字的宽度**，改写基本只能在这些词之间换）：

### U-120 番剧媒体信息区的标题

- **位置**：`app/src/main/res/layout/fragment_media_info.xml:143`（简介）、`:194`（制作人员）、`:273`（快速选择）、`:312`（播放）、`:320`（追番）、`:54`（加载中）
- **出现在哪**：番剧详情页（`fragment_media_info.xml`）的各区块
- **用途**：番剧页分区标题与动作按钮
- **原文**：

> 简介
> 制作人员
> 快速选择
> 播放
> 追番
> 加载中

- **改写**：

> 简介
> 制作人员
> 快速选择
> 播放
> 追番
> 加载中

### U-121 番剧备案号

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/BangumiInfoFragment.kt:294`
- **出现在哪**：番剧详情页底部
- **用途**：国产动画的备案号前缀。`"备案号：" + bangumi!!.info.record!!.trim()`
- **原文**：

> 备案号：

- **改写**：备案号：

### U-122 番剧未开播时的按钮

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/info/BangumiInfoFragment.kt:321`
- **出现在哪**：番剧详情页在未开播/无正片时，播放按钮位置
- **用途**：替代「播放」，避免点了没反应
- **原文**：

> 敬请期待

- **改写**：敬请期待

### U-123 播放器：听视频模式开关

- **位置**：`app/src/main/res/layout/activity_player.xml:48`（按钮文字）、`:60`（无障碍描述）
- **出现在哪**：播放器控制面板
- **用途**：只播放音频以省电。`:60` 那行是给读屏用的完整说明
- **原文**：

> 听视频模式
> 听视频模式，仅播放音频

- **改写**：

> 听视频模式
> 听视频模式，仅播放音频

### U-124 播放器：控制面板各按钮

- **位置**：`app/src/main/res/layout/activity_player.xml:517`（倍速调节）、`:596`（选择字幕）、`:672`（选择分P）、`:715`（选择清晰度）、`:745`（视频分段）、`:370`（视频详情）、`:476`（加载中）
- **出现在哪**：播放器内嵌的各选择面板标题
- **用途**：面板标题。`util/MsgUtil` 的提示与它们无关
- **原文**：

> 倍速调节 / 选择字幕 / 选择分P / 选择清晰度 / 视频分段 / 视频详情 / 加载中

- **改写**：倍速调节 / 选择字幕 / 选择分P / 选择清晰度 / 视频分段 / 视频详情 / 加载中

### U-125 播放器：发送弹幕区

- **位置**：`app/src/main/res/layout/activity_player.xml:627`（发送弹幕）、`:635`（`android:hint`）、`:650`（发送）
- **出现在哪**：播放器底部弹幕输入条
- **用途**：弹幕输入。`:635` 的提示语是全工程**最像社区氛围的一句话**，与 U-196「写一条友善的评论……」、U-197「爱国诚信友善……」同族
- **原文**：

> 发送弹幕
> 发个友善的弹幕见证当下
> 发送

- **改写**：

> 发送弹幕
> 发个友善的弹幕见证当下
> 发送

### U-126 播放器：音量显示格式

- **位置**：`app/src/main/res/layout/activity_player.xml:566`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/player/PlayerActivity.kt:1987`
- **出现在哪**：播放器音量条上方
- **用途**：`text_volume.text = "音量：" + show + "%"`，布局里的 `100%` 是初始占位
- **原文**：

> 音量：100%

- **改写**（改的是「音量：」这个前缀 + `%` 位置；`fragment_short_video_page.xml:199` 与 `ShortVideoPlayerActivity.kt:576` 有**同一句话的另外两个副本**）：

### U-127 播放器中央浮层的加载文字（`loading_text0`）

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/player/PlayerActivity.kt:676`、`:1024`、`:1108`、`:1111`、`:1199`、`:1952`、`:3324`、`:3357`、`:3464`、`:3850`、`:3878`
- **出现在哪**：播放器画面正中央转圈动画下面那一行（布局里的 `loading_text0`），**第二行 `loading_text1` 放速度或颜文字**
- **用途**：播放器是**没有进度条**的（手表屏太小），用户唯一的「没卡死」证据就是这行字在变。所以它的颗粒度做得非常细：初始化、载入视频/直播、缓冲、装填弹幕、切清晰度、加载分P、重新载入各有各的词
- **原文**（按触发时机）：

> 初始化播放（`:1024`）
> 载入视频中（`:1111`）
> 载入直播中（`:1108`）
> 正在缓冲（`:1199`）
> 装填弹幕中（`:676`、`:3357`、`:3878`）
> 切换清晰度中（`:3464`）
> 加载P{n}（`:3324`，`n` 是分P序号）
> 加载互动分P（`:3850`）
> 重新载入（`:1952`）

- **改写**（**每个词都必须在 4~6 字内**，因为它是叠在画面上的浮层；`加载P3` 这种带数字的要保持可拼接）：

### U-128 播放器面板：更新弹幕与切分辨率

- **位置**：`app/src/main/res/layout/panel_video_settings.xml:16`（更新弹幕）、`:43`（切换分辨率）、`:51`（当前）、`:68`（移动）、`:87`（查看详情）、`:106`（取消）
- **出现在哪**：播放器长按画面弹出的设置面板
- **用途**：面板里的动作与状态标记（「当前」标记选中的那档）
- **原文**：

> 更新弹幕 / 切换分辨率 / 当前 / 移动 / 查看详情 / 取消

- **改写**：更新弹幕 / 切换分辨率 / 当前 / 移动 / 查看详情 / 取消

### U-129 直播详情页：标签栏各字段名

- **位置**：`app/src/main/res/layout/activity_live_info.xml:253`（直播间ID）、`:269`（标签：???）、`:283`（分区）、`:297`（关注数）、`:311`（直播状态）、`:324`（描述）
- **出现在哪**：直播详情页（`activity_live_info.xml`）的信息区
- **用途**：字段名。**注意高度不一致**：布局里是「直播间ID」「分区」这种**不带冒号**的写法，而代码 `LiveInfoActivity.kt:111` `"房间号: "`、`:120` `"分区: "`、`:121` `"关注数: "`、`:130` `"直播状态: "` 会**带半角冒号 + 空格**再赋一遍，且字段名从「直播间ID」变成了「房间号」。改动前先确认哪一份真正显示
- **原文**：

> 直播间ID / 标签：??? / 分区 / 关注数 / 直播状态 / 描述

- **改写**：直播间ID / 标签：??? / 分区 / 关注数 / 直播状态 / 描述

### U-130 直播详情页：加载与状态文案

- **位置**：`app/src/main/res/layout/activity_live_info.xml:73`（正在加载中...）、`:200`（114人在线）、`:226`（已直播 00:00:00）、`:121`（选择清晰度）、`:162`（选择播放路线）、`:188`（播放）
- **出现在哪**：直播详情页
- **用途**：`:200`/`:226` 是初始占位（真实人数与时长由代码填），`:121`/`:162`/`:188` 是真按钮
- **原文**：

> 正在加载中... / 114人在线 / 已直播 00:00:00 / 选择清晰度 / 选择播放路线 / 播放

- **改写**：正在加载中... / 114人在线 / 已直播 00:00:00 / 选择清晰度 / 选择播放路线 / 播放

### U-131 直播详情页代码侧的状态拼接

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/live/LiveInfoActivity.kt:109`、`:111`、`:112`、`:120`、`:121`、`:130`
- **出现在哪**：直播详情页信息区，数据回来后覆盖布局里的占位
- **用途**：把「字段名 + 值」拼成一行
- **原文**：

> 直播开始于{时间}
> 房间号: {id}（短号: {short_id}）
> 标签：{tags}
> 分区: {父分区 > 子分区}
> 关注数: {x.x万}
> 直播状态: {状态}

- **改写**：

> 直播开始于{时间}
> 房间号: {id}（短号: {short_id}）
> 标签：{tags}
> 分区: {父分区 > 子分区}
> 关注数: {x.x万}
> 直播状态: {状态}

### U-132 短视频播放页

- **位置**：`app/src/main/res/layout/activity_short_video_player.xml:33`（加载中...）、`:50`（加载失败）、`:61`（重试）、`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/ShortVideoPlayerActivity.kt:202`（加载短视频...）、`app/src/main/res/layout/fragment_short_video_page.xml:57`（短视频）、`:199`（音量：100%）
- **出现在哪**：短视频页（独立 Activity 与 ViewPager 两种形态都在用）
- **用途**：加载、失败、重试三态 + 页内标签
- **原文**：

> 加载中... / 加载失败 / 重试 / 加载短视频... / 短视频 / 音量：100%

- **改写**：加载中... / 加载失败 / 重试 / 加载短视频... / 短视频 / 音量：100%

### U-133 播放器中央浮层的第二行（`loading_text1`）

- **位置**：`app/src/main/res/layout/activity_player.xml:490`（布局默认值 `OwO`）、`app/src/main/java/com/RobinNotBad/BiliClient/activity/player/PlayerActivity.kt:677`、`:3358`、`:3879`（`(≧∇≦)`）、`:1225-1226`（速度，`showLoadingSpeed()` 每 500ms 刷新一次）
- **出现在哪**：播放器画面正中央的转圈动画下方第二行字。第一行是 `loading_text0`（见 U-127），这行紧随其后
- **用途**：**同一个位置被三种内容轮流占用**——布局里写死的颜文字 `OwO`、代码里三处写死的 `(≧∇≦)`、以及起播时由 `showLoadingSpeed()` 每 500 毫秒刷新的实时下载速度 `%.1fKB/s`。都是颜文字/数字，**不是需要重写的说明文案**，但改写范围若含「播放器加载提示」，这三处副本都要一起看
- **原文**：

> OwO
>
> (≧∇≦)
>
> %.1fKB/s（格式化模板，实际显示形如 `128.6KB/s`）

- **改写**（颜文字部分可换，速度格式 `%.1fKB/s` 不要动单位）：

### U-134 视频卡片列表项

- **位置**：`app/src/main/res/layout/cell_video_list.xml:44`（标题）、`:66`（11.4万）、`:76`（UP主）、`app/src/main/res/layout/cell_video_local.xml:60`（标题）、`app/src/main/res/layout/cell_page_item.xml:17`（P1 标题）、`app/src/main/res/layout/cell_article_head.xml:41`（UP主名字）、`:79`、`app/src/main/res/layout/cell_article_image.xml:30`（共1张图片）、`app/src/main/res/layout/cell_dynamic_image.xml:28`（共1张图片）、`app/src/main/res/layout/cell_reply_list.xml:124`（共1张图片）
- **出现在哪**：各类视频/图文卡片布局
- **用途**：**除「共1张图片」外全是占位**。「共N张图片」由代码按图片数拼接（`cell_article_head.xml` :79 与 `cell_article_image.xml:30` 是同一句话的两个副本）
- **原文**：

> 标题 / 11.4万 / UP主 / P1 标题 / UP主名字 / 共1张图片

- **改写**（只需考虑「共N张图片」这一句）：

### U-135 视频详情页跳转过渡

- **位置**：`app/src/main/res/layout/activity_player_jump.xml:7`，字符串定义在 `app/src/main/res/values/strings.xml` 的 `text_getting_video`
- **出现在哪**：点视频卡片后、播放器还没起来时的一页过渡
- **用途**：解析视频地址期间的等待提示
- **原文**：

> 解析视频地址……

- **改写**：解析视频地址……

---

## 七、动态、评论、回复与发动态

### U-136 动态页顶部动作卡片

- **位置**：`app/src/main/res/layout/cell_dynamic_action.xml:13`（直播列表）、`:24`（发动态）、`:34`（类型）、`:44`（话题广场）
- **出现在哪**：动态页最上方的四个动作入口
- **用途**：动态页的功能导航。**这块卡片在 26.10 之后是新功能的推荐落点**（AGENTS.md 明确写过：想让老用户立刻看到入口就挂这里，别加菜单项）
- **原文**：

> 直播列表 / 发动态 / 类型 / 话题广场

- **改写**：直播列表 / 发动态 / 类型 / 话题广场

### U-137 动态卡片的转发与管理

- **位置**：`app/src/main/res/layout/cell_dynamic.xml:155`（转发）、`:170`（管理）
- **出现在哪**：动态卡片右下角
- **用途**：转发；「管理」只在看自己的动态时出现（删除等）
- **原文**：

> 转发
> 管理

- **改写**：

> 转发
> 管理

### U-138 动态详情页标题

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/dynamic/DynamicInfoActivity.kt:38`
- **出现在哪**：动态详情页顶栏
- **用途**：**这是第三套设置标题的写法**——直接 `pageName.text = "动态详情"`，不走 `setPageName()`。全文案里只有 4 处这么写（见 §0.3）
- **原文**：

> 动态详情

- **改写**：动态详情

### U-139 话题广场空态

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/dynamic/DynamicTopicActivity.kt:77`
- **出现在哪**：话题广场列表取不到数据时
- **用途**：**这是全工程写得最好的一条空态**——三行讲清了「空了 + 为什么 + 怎么办」。改写时保持这个三段式
- **原文**：

> 话题广场没有取到数据
> 可能是服务端暂时没有下发
> 点我重试

- **改写**：

> 话题广场没有取到数据
> 可能是服务端暂时没有下发
> 点我重试

### U-140 动态里的投票卡片

- **位置**：`app/src/main/res/layout/cell_dynamic_vote.xml:24`（投票标题）、`:50`（0人参与）、`:58`（投票）
- **出现在哪**：动态流里嵌的投票卡片
- **用途**：标题与参与人数是占位（真实值由代码填）；「投票」是按钮
- **原文**：

> 投票标题 / 0人参与 / 投票

- **改写**：投票标题 / 0人参与 / 投票

### U-141 投票详情页的状态与按钮

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/vote/VoteInfoActivity.kt:160`、`:161`、`:162`（状态）、`:269`、`:274`、`:279`（按钮）
- **出现在哪**：投票详情页
- **用途**：三个状态（投票中/已投票/已结束）在状态标签与提交按钮上**各写一套**，所以同一个状态有两种措辞：状态区写「已投票」，按钮写「已投票」（一致）；状态区写「已结束」，按钮也写「已结束」（一致）。**唯一不一致的是进行中**：状态区「投票中」、按钮「确认投票」
- **原文**：

> 投票中 / 已投票 / 已结束
> 确认投票 / 已投票 / 已结束

- **改写**：

> 投票中 / 已投票 / 已结束
> 确认投票 / 已投票 / 已结束

### U-142 评论操作条

- **位置**：`app/src/main/res/layout/cell_reply_action.xml:15`（写评论）、`:25`（回复排序）、`:38`（共114514条）
- **出现在哪**：评论区的操作条
- **用途**：写评论、切排序、评论总数。`:38` 的「114514」是**作者玩的梗**（恶臭数字），但它是**会被真实评论数覆盖的占位**，不必改
- **原文**：

> 写评论 / 回复排序 / 共114514条

- **改写**：写评论 / 回复排序 / 共114514条

### U-143 评论列表项

- **位置**：`app/src/main/res/layout/cell_reply_list.xml:94`（删除）、`:141`（ UP主觉得很赞）、`:172`（共114条回复）、`:185`（回复）
- **出现在哪**：评论列表项
- **用途**：`:94`「删除」只在自己发的评论上出现；`:141`「UP主觉得很赞」是**UP 主点赞标记**（注意开头有**一个前导空格**，是排版用的）；`:172` 楼中楼展开；`:185` 回复按钮
- **原文**：

> 删除
> 「 UP主觉得很赞」（注意前导空格）
> 共114条回复
> 回复

- **改写**：

> 删除
> 「 UP主觉得很赞」（注意前导空格）
> 共114条回复
> 回复

### U-144 写评论页

- **位置**：`app/src/main/res/layout/activity_write_reply.xml:77`（表情，`@string/btn_emote`）、`:96`（图片，`@string/btn_image`）、`:114`（发送，`@string/btn_send`）
- **出现在哪**：写评论页底部工具栏
- **用途**：输入区附件与发送。文字定义在 `strings.xml`（见第十二节 `btn_*` 表）
- **原文**：

> 表情 / 图片 / 发送

- **改写**：见 U-197~U-201（`btn_*` 表）

### U-145 回复评论时的输入框预填

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/reply/WriteReplyActivity.kt:129`
- **出现在哪**：从某条评论点「回复」进写评论页时，输入框里**已经填好**的内容
- **用途**：让用户直接接着写。格式是**半角冒号 + 一个空格**，`@` 后面是对方的昵称
- **原文**：

> 回复 @{昵称} :

- **改写**：回复 @{昵称} :

### U-146 发动态页

- **位置**：`app/src/main/res/layout/activity_send_dynamic.xml:77`（表情）、`:96`（添加图片）、`:122`（添加投票）、`:142`（定时发布）、`:163`（`android:hint` 投票标题）、`:189`（+ 添加选项）、`:198`（移除投票）、`:216`（完成）
- **出现在哪**：发动态页
- **用途**：发动态的全部动作与投票子表单。`:216`「完成」是发布按钮——**这个词偏弱**，用户不一定知道它是「发出去」
- **原文**：

> 表情 / 添加图片 / 添加投票 / 定时发布 / 投票标题 / + 添加选项 / 移除投票 / 完成

- **改写**：表情 / 添加图片 / 添加投票 / 定时发布 / 投票标题 / + 添加选项 / 移除投票 / 完成

### U-147 动态与评论的图片数标记

- **位置**：`app/src/main/res/layout/cell_reply_list.xml:124`（共1张图片）、`app/src/main/res/layout/cell_article_head.xml:79`（共1张图片）
- **出现在哪**：评论里的图片集、图文的头部
- **用途**：与 U-134 同一句话的副本
- **原文**：

> 共1张图片

- **改写**：见 U-134

### U-148 图文/动态列表项的标题占位

- **位置**：`app/src/main/res/layout/cell_article_list.xml:107`、`app/src/main/res/layout/cell_dynamic_article.xml:101`、`app/src/main/res/layout/cell_opus.xml:85`、`app/src/main/res/layout/cell_dynamic_video.xml:102`
- **出现在哪**：各类图文/动态卡片
- **用途**：**纯占位**（「标题标题标题…」重复 12~16 遍是为了测省略号），运行时被真实标题覆盖。**不是文案**
- **原文**：

> 标题标题标题标题标题标题标题标题标题标题标题标题

- **改写**：不需要改（占位）

---

## 八、收藏夹、系列、笔记、创作中心、大会员、资料编辑

### U-149 收藏夹编辑页

- **位置**：`app/src/main/res/layout/activity_favorite_folder_edit.xml:26`（编辑收藏夹）、`:60`（收藏夹名称）、`:68`（`android:hint`）、`:80`（收藏夹简介）、`:88`（`android:hint`）、`:108`（保存）、`:126`（删除收藏夹）
- **出现在哪**：收藏列表页长按收藏夹 → 编辑
- **用途**：一个页面同时承担「新建」与「编辑」，所以标题写「编辑收藏夹」而字段名与删除按钮都是编辑语义
- **原文**：

> 编辑收藏夹
> 收藏夹名称 / 请输入收藏夹名称
> 收藏夹简介 / 请输入收藏夹简介（可选）
> 保存 / 删除收藏夹

- **改写**（注意「保存」与「删除收藏夹」是**同一屏内的主/破坏性两个动作**，改写不要让它们视觉权重接近——删除已按规范走 `danger`）：

### U-150 收藏夹列表项与新建入口

- **位置**：`app/src/main/res/layout/cell_favorite_folder_list.xml:58`（收藏夹名称）、`app/src/main/res/layout/cell_create_folder_button.xml:15`（+ 创建收藏夹）
- **出现在哪**：收藏列表页
- **用途**：`:58` 是占位；「+ 创建收藏夹」是列表最后一项的**真按钮文案**（注意 `+` 后面有一个**半角空格**）
- **原文**：

> 收藏夹名称
> + 创建收藏夹

- **改写**（只为「+ 创建收藏夹」写）：

### U-151 本地文件夹项与长按菜单

- **位置**：`app/src/main/res/layout/cell_video_folder.xml:35`（文件夹名称）、`:44`（0个视频）、`:77`（重命名）、`:96`（拆散）、`:115`（取消）
- **出现在哪**：缓存页的文件夹卡片；`:77`/`:96`/`:115` 是**卡片内展开的操作区**（不是弹窗）
- **用途**：`:35`/`:44` 是占位；「重命名」「拆散」「取消」是真文案。**「拆散」是全工程最特别的一个动词**——把打包成一个文件夹的本地视频拆回独立条目，官方客户端没有对应概念，所以这是自造词
- **原文**：

> 文件夹名称 / 0个视频
> 重命名 / 拆散 / 取消

- **改写**（「拆散」如果换词，`cell_video_folder` 的宽度只够两个字）：

### U-152 用户主页顶部的动作按钮

- **位置**：`app/src/main/res/layout/cell_user_info.xml:287`（关注）、`:301`（私信）、`:315`（加入老粉计划）、`:66`（查看粉丝勋章）、`:203`（充电公示）
- **出现在哪**：用户主页（`cell_user_info.xml`，被 `UserInfoActivity` 当头部用）
- **用途**：主页的主要动作。多数字段（`:27` 蔡徐坤、`:53` 2.5万粉丝、`:92` 91关注、`:106` 只因你太美！、`:121` 震惊!）都是**占位**
- **原文**：

> 关注 / 私信 / 加入老粉计划 / 查看粉丝勋章 / 充电公示

- **改写**：关注 / 私信 / 加入老粉计划 / 查看粉丝勋章 / 充电公示

### U-153 用户主页的争议账号提示

- **位置**：`app/src/main/res/layout/cell_user_info.xml:174`
- **出现在哪**：被平台冻结功能的用户主页
- **用途**：**平台下发的状态说明**，虽然写在布局里，但它是哔哩哔哩的官方措辞，属于「引用而非创作」——改写前要确认能不能改（改错了会与 App 内其它位置不一致）
- **原文**：

> 该用户存在争议行为，已冻结其帐号功能的使用

- **改写**：该用户存在争议行为，已冻结其帐号功能的使用

### U-154 用户主页的直播中标记

- **位置**：`app/src/main/res/layout/cell_user_info.xml:148`
- **出现在哪**：正在直播的用户主页
- **用途**：占位（`xxxx` 是直播间标题）
- **原文**：

> 直播中：xxxx

- **改写**：不需要改（占位）

### U-155 充电用户列表项

- **位置**：`app/src/main/res/layout/cell_electric_user.xml:28`（用户名）、`:40`（充电留言）
- **出现在哪**：充电公示页
- **用途**：占位
- **原文**：

> 用户名 / 充电留言

- **改写**：不需要改（占位）

### U-156 创作中心数据项

- **位置**：`app/src/main/res/layout/activity_creative_center.xml:59`（投稿总数据）、`:96`（粉丝数）、`:137`（播放量）、`:178`（总点赞）、`:219`（总投币）、`:260`（总收藏）、`:301`（总分享）、`:342`（评论数）、`:383`（弹幕数）、`:423`（成为UP主时间）
- **出现在哪**：我的页面 → 创作中心
- **用途**：数据卡片标签。**这块布局是靠复制的**：每项的 id 与结构都不同但排版完全一致（粉丝数/播放量/总点赞…），改文案时逐项改，别只改第一个
- **原文**：

> 投稿总数据 / 粉丝数 / 播放量 / 总点赞 / 总投币 / 总收藏 / 总分享 / 评论数 / 弹幕数 / 成为UP主时间

- **改写**（「成为UP主时间」是全场最长的一个标签，卡片未必放得下）：

### U-157 大会员页字段名

- **位置**：`app/src/main/res/layout/activity_vip.xml:26`（大会员）、`:66`（领取每日经验）、`:73`（大会员信息）、`:108`（大会员状态）、`:147`（会员类型）、`:186`（过期时间）、`:225`（当前等级）、`:264`（经验值）、`:303`（绑定手机）、`:319`（卡券信息）
- **出现在哪**：我的页面 → 大会员
- **用途**：会员信息字段名与两个动作（领经验、卡券）。**「大会员信息」是分节标题，其余是字段名**
- **原文**：

> 大会员 / 领取每日经验 / 大会员信息 / 大会员状态 / 会员类型 / 过期时间 / 当前等级 / 经验值 / 绑定手机 / 卡券信息

- **改写**：大会员 / 领取每日经验 / 大会员信息 / 大会员状态 / 会员类型 / 过期时间 / 当前等级 / 经验值 / 绑定手机 / 卡券信息

### U-158 大会员页的重复「加载中...」

- **位置**：`app/src/main/res/layout/activity_vip.xml:93`、`:132`、`:171`、`:210`、`:249`、`:288`
- **出现在哪**：大会员页六个字段的值位置
- **用途**：数据未回来时的占位。**同一句话在一个布局里写了 6 遍**
- **原文**：

> 加载中...

- **改写**（改的话 6 处一起改）

### U-159 大会员页的值文案

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/user/VipActivity.kt:42`、`:44`、`:46`、`:48`、`:53`、`:56`、`:57`、`:58`、`:71`
- **出现在哪**：大会员页字段值
- **用途**：把接口返回的状态翻译成人话。注意 **`会员类型` 有三档：「年度大会员」「月大会员」「大会员」**，而**未开通时类型写「无」、状态写「否」、到期时间也写「无」**——三个字段在失败态下全写「无/否」，用户很难分清哪个是哪个
- **原文**：

> 是 / 否
> 年度大会员 / 月大会员 / 大会员 / 无
> 未知
> 未绑定

- **改写**：

> 是 / 否
> 年度大会员 / 月大会员 / 大会员 / 无
> 未知
> 未绑定

### U-160 编辑资料页（入口页）

- **位置**：`app/src/main/res/layout/activity_edit_profile.xml:26`（编辑个人资料）、`:77`（点击上传头像 / 支持从相册选择图片，审核通过后生效）、`:111`（修改个人描述）、`:145`（修改昵称/生日/性别）
- **出现在哪**：我的页面 → 编辑个人资料（04 的 `S-205` 收录的是**入口名**，这里是**页面内**文案）
- **用途**：三个可编辑项的入口。`:77` 是**全工程少见的双行说明**（动作 + 规则），且它解释了「审核」这个不直观的机制
- **原文**：

> 编辑个人资料
> 点击上传头像
> 支持从相册选择图片，审核通过后生效
> 修改个人描述
> 修改昵称/生日/性别

- **改写**：

> 编辑个人资料
> 点击上传头像
> 支持从相册选择图片，审核通过后生效
> 修改个人描述
> 修改昵称/生日/性别

### U-161 修改签名页

- **位置**：`app/src/main/res/layout/activity_edit_sign.xml:26`（修改个人描述）、`:60`（修改签名不会立即生效，会等待审核队列稍后生效）、`:69`（`android:hint`）、`:99`（提交）
- **出现在哪**：编辑资料 → 修改个人描述
- **用途**：签名编辑。`:60` 的说明文案与标题**用了两个不同的词**（标题「个人描述」、说明「签名」）——同一个东西两种叫法，改写时应当统一
- **原文**：

> 修改个人描述
> 修改签名不会立即生效，会等待审核队列稍后生效
> 请输入个人描述（最多70个字符）
> 提交

- **改写**（**注意 `（最多70个字符）` 是一个硬约束**，若改写后长度限制变了，`EditSignActivity` 的校验也要同步）：

### U-162 修改昵称/生日/性别页

- **位置**：`app/src/main/res/layout/activity_edit_user_info.xml:26`（编辑资料）、`:58`（留空表示不修改对应项）、`:67`（`android:hint` 昵称）、`:79`（`android:hint` 生日…）、`:90`（性别）、`:106`（男）、`:114`（女）、`:129`（提交）
- **出现在哪**：编辑资料 → 修改昵称/生日/性别
- **用途**：三个字段一次性提交。`:58` 是关键规则说明——**只有「留空=不改」这一条**，决定了用户敢不敢只填一项
- **原文**：

> 编辑资料
> 留空表示不修改对应项
> 昵称
> 生日（YYYY-MM-DD，如 2000-01-01）
> 性别 / 男 / 女 / 提交

- **改写**（`:79` 的 `YYYY-MM-DD` 与示例日期是**格式约定**，改成别的格式必须同步改解析代码）：

### U-163 笔记页

- **位置**：`app/src/main/res/layout/activity_note.xml:26`（笔记）、`:82`（这个视频还没有笔记）
- **出现在哪**：视频详情页 → 笔记
- **用途**：标题与空态。空态口吻比列表页的「啥都木有~」（U-095）具体得多——它说清了「这是这个视频的问题，不是你的问题」
- **原文**：

> 笔记
> 这个视频还没有笔记

- **改写**：

> 笔记
> 这个视频还没有笔记

### U-164 图文收藏夹（`FavouriteOpusListActivity`）

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/user/favorite/FavouriteOpusListActivity.kt:18`
- **出现在哪**：收藏列表里的图文收藏夹
- **用途**：标题，已在 U-048 登记；此处的**类名拼写是 `Favourite`（英式）而 `FavoriteFolderListActivity` 是美式**，改名时不要顺手统一（会打破引用）
- **原文**：

> 图文收藏夹

- **改写**：图文收藏夹

### U-165 系列/合集详情

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/series/SeriesInfoActivity.kt:144`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/collection/CollectionInfoActivity.kt:121`、`:226`
- **出现在哪**：系列/合集详情页的播放量位置
- **用途**：`holder.playTimes.text = "共" + {播放量}`，**只有「共」一个字是自己写的**，后面接的是接口给的数（`collection.view` 是字符串）
- **原文**：

> 共{播放量}

- **改写**（注意后面直接拼数字/数字+单位，不加空格）：

### U-166 时间线剧集项

- **位置**：`app/src/main/res/layout/cell_timeline_episode.xml:32`（标题）、`:42`（第1话）
- **出现在哪**：时间线页的剧集行
- **用途**：占位
- **原文**：

> 标题 / 第1话

- **改写**：不需要改（占位）

### U-167 分组与分区相关列表项

- **位置**：`app/src/main/res/layout/cell_follow_group.xml:21`（分组名称）、`:46`（`android:contentDescription` 展开/折叠）、`app/src/main/res/layout/cell_group_header.xml:34`（分组）、`:49`（分组描述）、`app/src/main/res/layout/cell_section_header.xml:16`（分区标题）、`:25`（0项）
- **出现在哪**：关注列表的分组头、通用分区头
- **用途**：除 `contentDescription`「展开/折叠」外都是占位。**「展开/折叠」是 4 处中文无障碍描述之一**（见第〇节 0.4），读屏用户靠它知道这个图标能点
- **原文**：

> 分组名称 / 展开/折叠 / 分组 / 分组描述 / 分区标题 / 0项

- **改写**（只需考虑「展开/折叠」与「0项」）：

### U-168 登录记录项

- **位置**：`app/src/main/res/layout/cell_login_record.xml:20`（设备名称）、`:29`（登录方式）、`:37`（登录时间）、`:52`（位置）
- **出现在哪**：我的页面 → 登录记录
- **用途**：字段名。**这块是安全相关页面**（用户靠它判断账号有没有被别人登录），字段名不能含糊
- **原文**：

> 设备名称 / 登录方式 / 登录时间 / 位置

- **改写**：设备名称 / 登录方式 / 登录时间 / 位置

### U-169 粉丝勋章项

- **位置**：`app/src/main/res/layout/cell_medal_list.xml:40`（勋章名称）、`:57`（主播名称）、`:71`（亲密度: 0）
- **出现在哪**：用户主页 → 查看粉丝勋章
- **用途**：`:40`/`:57` 是占位；**「亲密度」后接半角冒号 + 空格 + 数字**，是真格式
- **原文**：

> 勋章名称 / 主播名称 / 亲密度: 0

- **改写**：勋章名称 / 主播名称 / 亲密度: 0

### U-170 话题项

- **位置**：`app/src/main/res/layout/cell_topic.xml:24`（话题）、`:37`（0 动态 · 0 浏览）
- **出现在哪**：话题广场列表项
- **用途**：占位。「0 动态 · 0 浏览」里的 **`·` 前后各有一个半角空格**，是拼接格式
- **原文**：

> 话题
> 0 动态 · 0 浏览

- **改写**：不需要改（占位）

### U-171 UP 主列表项与用户列表项

- **位置**：`app/src/main/res/layout/cell_up_list.xml:53`（UP主）、`app/src/main/res/layout/cell_user_list.xml:55`（年少时的怦然心动，送给了永远无法企及的人。）
- **出现在哪**：搜索/关注列表的用户项
- **用途**：都是占位。`:55` 那句是**签名示例**（用户签名位置），作者随手写了一句
- **原文**：

> UP主
> 年少时的怦然心动，送给了永远无法企及的人。

- **改写**：不需要改（占位）

### U-172 变动记录项

- **位置**：`app/src/main/res/layout/cell_log.xml:29`（变化说明）、`:37`（时间）
- **出现在哪**：硬币变化记录 / 经验变化记录页
- **用途**：列头。
- **原文**：

> 变化说明 / 时间

- **改写**：变化说明 / 时间

### U-173 交互变量项

- **位置**：`app/src/main/res/layout/cell_interaction_debug_var.xml:20`（变量名）
- **出现在哪**：互动视频变量调试页（仅调试包）
- **用途**：占位
- **原文**：

> 变量名

- **改写**：不需要改（占位）

---

## 九、登录流程

登录链路有**三条并存的路径**（二维码 / 账号密码 / 短信验证码），文案分散在三个 Fragment 布局与三个 Kotlin 文件里。这里是**新用户第一眼看到的界面**，也是投诉最集中的地方（手表上不好输密码）。

### U-174 二维码登录页

- **位置**：`app/src/main/res/layout/fragment_qr_login.xml:57`（`android:contentDescription`）、`:71`（正在获取二维码）、`:100`（WEB端扫码登录）、`:135`（切换账号）、`:157`（但是我拒绝）、`:179`（其他设备导入）
- **出现在哪**：登录页默认 tab
- **用途**：`WEB端扫码登录` 是模式切换按钮；`其他设备导入` 是**引导用户从手机端把登录信息导过来**的入口（配套 U-175）；`但是我拒绝` 是返回/放弃（**与 01 的 `A-076` 是同一句话在两个地方各写一遍**：那里是 `@string/btn_login_refuse` 用在特殊登录页，这里是硬编码在扫码页）
- **原文**：

> 登录二维码（无障碍）
> 正在获取二维码
> WEB端扫码登录
> 切换账号
> 但是我拒绝
> 其他设备导入

- **改写**：

> 登录二维码（无障碍）
> 正在获取二维码
> WEB端扫码登录
> 切换账号
> 但是我拒绝
> 其他设备导入

### U-175 二维码状态文字（`scanStat`）

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/QRLoginFragment.kt:168`、`:171`、`:184`、`:187`、`:211`、`:218`、`:225`、`:257`、`:284`、`:287`、`:291`、`:299`、`:383`、`:384`、`:387`、`:395`、`:427`
- **出现在哪**：二维码下方那块状态文字，**扫码登录的每一步都靠它告知进度**
- **用途**：这是全工程**状态文案最细的地方**——二维码获取（TV端/普通两种）、已扫描待确认、已失效、轮询错误码（86090/86101）、接口变动全都各有措辞。`TV端模式` 指的是拿电视端 API 出码（能用但可能失效）
- **原文**：

> 正在获取TV端二维码
> 正在获取二维码
> 获取二维码失败，网络错误
> 登录接口可能失效，请找开发者
> 遇到其他错误：\n{e.message}
> 无法获取二维码信息，点击上方重试\n{e.message}
> 请使用手机端哔哩哔哩扫码登录（TV端模式）\n点击二维码可以进行放大和缩小
> 请使用官方手机端哔哩哔哩扫码登录\n点击二维码可以进行放大和缩小
> 已扫描，请在手机上点击确认登录
> 已扫描，请在手机上点击登录
> 二维码已失效，点击上方重新获取
> 正在处理TV端登录……
> 正在处理登录……
> 二维码登录API可能变动，\n但你仍然可以尝试扫码登录。\n建议反馈给开发者

- **改写**（**这块文案的密度极高、信息量大，改写时最忌讳「简化」成一句「登录失败」**——用户在这块文字上找的就是下一步该干什么）：

### U-176 账号密码登录页

- **位置**：`app/src/main/res/layout/fragment_password_login.xml:28`（账号）、`:37`（`android:hint`）、`:47`（密码）、`:56`（`android:hint`）、`:83`（登录）、`:101`（短信验证码登录）
- **出现在哪**：登录页第二个 tab
- **用途**：账号密码表单
- **原文**：

> 账号 / 手机号/邮箱 / 密码 / 输入密码 / 登录 / 短信验证码登录

- **改写**（`:101` 的按钮文字是**这个 tab 的出口**，写「短信验证码登录」比「换个方式」明确）：

### U-177 短信验证码登录页

- **位置**：`app/src/main/res/layout/fragment_sms_login.xml:28`（手机号）、`:37`（`android:hint`）、`:47`（验证码）、`:62`（`android:hint`）、`:80`（获取验证码）、`:106`（登录）、`:124`（账号密码登录）
- **出现在哪**：登录页第三个 tab
- **用途**：验证码表单
- **原文**：

> 手机号 / 输入手机号 / 验证码 / 短信验证码 / 获取验证码 / 登录 / 账号密码登录

- **改写**：手机号 / 输入手机号 / 验证码 / 短信验证码 / 获取验证码 / 登录 / 账号密码登录

### U-178 「获取验证码」的倒计时

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/SMSLoginFragment.kt:232`
- **出现在哪**：验证码页的「获取验证码」按钮，发送成功后进入倒计时，倒计时结束后恢复
- **用途**：`sendSmsText.text = "获取验证码"` 是**倒计时结束那一刻恢复的文字**；倒计时中的文字由同一个文件另外的定时器写（形如 `重新获取(Ns)`），**改这个按钮的措辞要同时处理两处**
- **原文**：

> 获取验证码

- **改写**：获取验证码

### U-179 登录方式说明页

- **位置**：`app/src/main/res/layout/activity_qr_login_help.xml:25`（登录方式说明）、`:43`（知道了）
- **出现在哪**：登录页 → 登录方式说明
- **用途**：`:25` 与 `QRLoginHelpActivity.kt:15` 的标题同名（见 U-027）；`:43`「知道了」是全文案里出现频率最高的按钮词之一，与 `TerminalDialog.alert` 的默认按钮（06）同字
- **原文**：

> 登录方式说明
> 知道了

- **改写**：

> 登录方式说明
> 知道了

### U-180 账号切换页

- **位置**：`app/src/main/res/layout/activity_account_switch.xml:26`（`@string/pagename_account_switch`，见 U-070）、`app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/login/AccountSwitchActivity.kt:143`（暂无已保存的账号…）、`:263`（添加新账号）
- **出现在哪**：设置 → 账号与登录 → 账号切换
- **用途**：多账号管理。`:143` 的空态**同时承担教学职责**（告诉用户登录后会自动保存），比通用空态信息量大
- **原文**：

> 暂无已保存的账号
> 登录后会自动保存账号凭证
> 添加新账号

- **改写**：

> 暂无已保存的账号
> 登录后会自动保存账号凭证
> 添加新账号

---

## 十、其余独立页面

### U-181 崩溃页（`activity_catch.xml`）

- **位置**：`app/src/main/res/layout/activity_catch.xml:34`（可能的崩溃原因：\n？？？）、`:55`（崩溃堆栈：）、`:77`（点击上方展开错误堆栈）、`:117`（附带应用日志）、`:128`（崩溃前最近的应用运行日志，已脱敏；不勾选就不会发送）、`:151`（上传堆栈）、`:159`（重启应用）、`:165`（退出捕捉）、`:175`（“哔哩终端坏掉了，这不是终端的错，绝对不是”）
- **出现在哪**：**崩溃页跑在 `:error_activity` 独立进程**（26.10.04 起），是用户遇到闪退后唯一看到的界面。文案全部硬编码在这个布局里，没有任何 `strings.xml`
- **用途**：崩溃页的设计意图很清楚——**先安抚（`:175` 那句带引号的自嘲），再解释（`:34`/`:55`），最后给出口（`:151`/`:159`/`:165`）**。`:128` 是隐私承诺（日志已脱敏、不勾选不发送），**不要把「已脱敏」删掉**
- **原文**：

> 可能的崩溃原因：
> ？？？
> 崩溃堆栈：
> 点击上方展开错误堆栈
> 附带应用日志
> 崩溃前最近的应用运行日志，已脱敏；不勾选就不会发送
> 上传堆栈
> 重启应用
> 退出捕捉
> “哔哩终端坏掉了，这不是终端的错，绝对不是”

- **改写**（`？？？` 会被真实原因覆盖；`:34` 的换行 `\n` 是真换行）：

### U-182 崩溃页的多态提示（代码侧）

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/CatchActivity.kt:72`、`:94`、`:144`、`:154`、`:168`
- **出现在哪**：崩溃页内的状态文字与上传按钮
- **用途**：`:144` 是**全文案里最长的一条**——上传成功后把报错 ID 直接写进按钮里，靠按钮承载一段说明（手表上按钮会变成多行）。`:154` 是「这类错误不许上传」的拒绝语
- **原文**：

> 本次没有可附带的日志（`:72`）
> 正在自动上报错误报告…（`:94`）
> 请带着你的报错ID：{code}\n和你崩溃前进行的操作\n去找开发者\n（提醒：开发者不保证会修好也不保证随时回复你）（`:144`）
> 此类型报错不可上传\n非特殊情况请勿打扰开发者谢谢喵（`:154`）
> 未知的崩溃原因（`:168`）

- **改写**（`:144` 里那句免责是**作者刻意的自嘲式预防**，改写时保留这个「不过度承诺」的态度）：

### U-183 下载页准备状态

- **位置**：`app/src/main/res/layout/activity_download.xml:13`
- **出现在哪**：点「缓存」后进入的下载页，任务创建但还没开始
- **用途**：占位与准备提示
- **原文**：

> 准备下载

- **改写**：准备下载

### U-184 下载列表空态

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/adapter/video/DownloadAdapter.kt:112`
- **出现在哪**：下载列表页在「没有下载中的项」时的提示（与 U-095 的通用空态不同，这条更具体）
- **用途**：区分「下载列表空」与「所有列表都空」
- **原文**：

> 没有下载中的项

- **改写**：没有下载中的项

### U-185 图片查看页计数

- **位置**：`app/src/main/res/layout/activity_image_viewer.xml:67`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/ImageViewerActivity.kt:84`
- **出现在哪**：图片查看页右下角
- **用途**：`textView.text = "第${position + 1}/${imageList.size}张"`，布局里的 `第0/0张` 是占位
- **原文**：

> 第0/0张

- **改写**（拼出来是「第3/9张」，**中英混排且没有空格**，改写时注意它读起来顺不顺）：

### U-186 复制页

- **位置**：`app/src/main/res/layout/activity_copy.xml:56`（`@string/hint_copy_empty`）、`:66`（`@string/text_select_range`）、`:79`（`@string/hint_start_position`）、`:103`（`@string/hint_end_position`）、`:169`（`@string/text_copy_longclick`）、`:175`（`@string/btn_copy_selected`）、`:181`（`@string/btn_copy_all`）、`:187`（`@string/text_copy_naughty_devices`）
- **出现在哪**：长按文本 → 复制
- **用途**：一个**带区间选择**的复制页（不是系统复制）。文案定义在 `strings.xml`（见第十一、十二节），此处登记布局落点
- **原文**：

> 啥都木有（剪贴板空）
> 选择范围
> 起始位置 / 结束位置
> *长按选择到开头/结尾*
> 复制选中 / 复制全部
> *部分设备可能阉割剪贴板导致无法复制，此处点名表扬小天才*

- **改写**：见 U-205、U-207、U-208、U-217、U-222、U-229

### U-187 公告列表项占位

- **位置**：`app/src/main/res/layout/cell_announcement_list.xml:29`（新春快乐！）、`:39`（春来早，瑞兽登高，祥风抬首云中道。\n似野鹤，彩鸢入霄，溪回谷转隐瑛瑶。）
- **出现在哪**：公告列表页
- **用途**：**占位**，真实公告由接口下发。作者写了一个春节对句当示例。**不是文案**
- **原文**：

> 新春快乐！
> 春来早，瑞兽登高，祥风抬首云中道。
> 似野鹤，彩鸢入霄，溪回谷转隐瑛瑶。

- **改写**：不需要改（占位）

### U-188 TO DO 清单页

- **位置**：`app/src/main/res/layout/activity_todo_list.xml:24`（TO DO清单）、`:57`（手机端适配）、`:65`（平板端适配）、`:73`（不保证会实现）
- **出现在哪**：一个隐藏的「开发计划」页面（**入口位置未能确认，推测是从关于页或调试入口进**）
- **用途**：**:24 的「TO DO」是英文 + 中文混排且中间没空格**；`:73` 是一句免责，说明这个列表不是承诺
- **原文**：

> TO DO清单
> 手机端适配
> 平板端适配
> 不保证会实现

- **改写**：

> TO DO清单
> 手机端适配
> 平板端适配
> 不保证会实现

### U-189 列表选择页标题

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/ListChooseActivity.kt:37`
- **出现在哪**：底部弹出的选择列表标题（`(findViewById<TextView>(R.id.pageName)).text = "请选择"`）
- **用途**：**直接改 `pageName` 而不走 `setPageName()`**，是第四种标题写法。它是通用组件，文案会出现在很多场景（选收藏夹、选清晰度等），所以只能写得非常泛
- **原文**：

> 请选择

- **改写**：请选择

### U-190 分页选择页标题

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/MultiPageActivity.kt:34`、`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/local/LocalPageChooseActivity.kt:39`
- **出现在哪**：在线视频与本地视频两个分P选择页
- **用途**：两处写了**同一句话**（改要一起改）。注意它与 U-189 的「请选择」是**两个不同措辞**，同类操作两种叫法
- **原文**：

> 请选择分页

- **改写**：请选择分页

### U-191 清晰度选择页标题（播放器版）

- **位置**：`app/src/main/java/com/RobinNotBad/BiliClient/activity/video/QualityChooserActivity.kt:56`
- **出现在哪**：播放器里点「选择清晰度」弹出的页面
- **用途**：与设置里的 `S-033`「请选择清晰度」、`U-050` 同字，但**这是播放器内的那个**，代码位置不同
- **原文**：

> 请选择清晰度

- **改写**：请选择清晰度

### U-192 互动视频变量调试页

- **位置**：`app/src/main/res/layout/activity_interaction_debug.xml:24`
- **出现在哪**：互动视频播放中长按调试按钮
- **用途**：与 `InteractionDebugActivity.kt:28` 的 `setPageName` 同字（U-002），仅调试包
- **原文**：

> 互动视频变量调试

- **改写**：互动视频变量调试

### U-193 测试页（仅调试包）

- **位置**：`app/src/main/res/layout/activity_test.xml:65`（deepseek猫娘请求测试）、`:84`（opus请求测试）、`:94`（说明段）、`:103`（`android:hint`）、`:115`（`android:hint`）、`:127`（启用WBI签名）、`:134`（使用POST方式）、`:165`（查看Cookies）、`:178`（`android:hint`）、`app/src/main/java/com/RobinNotBad/BiliClient/activity/settings/TestActivity.kt:109`（使用R1）
- **出现在哪**：仅 Debug 包；入口由 `BuildConfig.DEBUG` 编译期包住，Release 下整类被 strip
- **用途**：`:94` 是**全工程唯一一条对开发者说话的文案**，态度很明确（「如果你不是开发者，你不需要知道这个页面是用来干什么的」）。改写它不影响正式用户
- **原文**：

> deepseek猫娘请求测试
> opus请求测试
> 如果你不是开发者，你不需要知道这个页面是用来干什么的。\n——当然，知道一点也无妨，这是一个简易的网络请求工具。
> 在这里输入请求链接
> 在这里输入POST数据
> 启用WBI签名 / 使用POST方式
> 查看Cookies
> 这里会输出请求结果
> 使用R1

- **改写**（可选，只影响调试包）：

### U-194 表情页与视频页的短标签

- **位置**：`app/src/main/res/layout/cell_subtitle.xml:14`（让我们说中文）、`app/src/main/res/layout/cell_action_button.xml:10`（操作）、`app/src/main/res/layout/item_vote_option.xml:31`（选项文字）、`app/src/main/res/layout/item_dialog_terminal.xml:42`（条目）
- **出现在哪**：子标题行、通用动作按钮、投票选项、弹窗列表项
- **用途**：**除 `cell_action_button`「操作」可能是默认文案外，其余都是占位**（占位说明见第十四节）
- **原文**：

> 让我们说中文 / 操作 / 选项文字 / 条目

- **改写**（只需考虑「操作」）：

### U-195 三个通用弹窗布局的按钮

- **位置**：`app/src/main/res/layout/activity_confirm_dialog.xml:38`（内容）、`:50`（取消）、`:58`（确定）、`app/src/main/res/layout/activity_input_dialog.xml:59`（取消）、`:67`（确定）、`app/src/main/res/layout/activity_dialog.xml:38`（内容）、`:44`（知道了）
- **出现在哪**：三类通用弹窗布局的默认按钮
- **用途**：**这三套布局定义的按钮文字与 `util/TerminalDialog.kt` 的默认值构成「双真相」**（`TerminalDialog.kt:148` 的 `confirmText = "确定"`、`:149` 的 `cancelText = "取消"`、`:259` 的 `buttonText = "知道了"`）。**改一处必须改另一处**，归属上以 06 为准（06 已收录 `TerminalDialog` 那侧）
- **原文**：

> 内容 / 取消 / 确定 / 知道了

- **改写**：改 `TerminalDialog` 的默认参数（见 06），并同步这三个布局

---

## 十一、输入框提示语汇总（`android:hint`）

手表屏幕小，`hint` 是**唯一在输入前能告诉用户该填什么的东西**——一旦用户开始打字它就消失。以下 8 条定义在 `strings.xml`，**改这里就改遍了所有引用它的地方**。

| 编号 | 定义位置 | 原文 | 引用位置 | 用途 | 改写 |
| --- | --- | --- | --- | --- | --- |
| U-196 | `app/src/main/res/values/strings.xml:193` | 写一条友善的评论…… | `app/src/main/res/layout/activity_write_reply.xml:60` | 评论输入框。**「友善」是刻意的社区引导** | 写一条友善的评论…… |
| U-197 | `app/src/main/res/values/strings.xml:194` | 搜点什么喵~ | `app/src/main/res/layout/activity_search.xml:71` | 搜索框。带项目惯用的猫娘后缀 | 搜点什么喵~ |
| U-198 | `app/src/main/res/values/strings.xml:195` | 爱国诚信友善…… | `app/src/main/res/layout/activity_private_msg.xml:63` | 私信输入框。**社会主义价值观接龙**，与 U-196 同一族但换了措辞 | 爱国诚信友善…… |
| U-199 | `app/src/main/res/values/strings.xml:196` | 起始位置 | `app/src/main/res/layout/activity_copy.xml:79` | 复制页选区起点 | 起始位置 |
| U-200 | `app/src/main/res/values/strings.xml:197` | 结束位置 | `app/src/main/res/layout/activity_copy.xml:103` | 复制页选区终点 | 结束位置 |
| U-201 | `app/src/main/res/values/strings.xml:198` | 在这里输入登录信息 | `app/src/main/res/layout/activity_login_special.xml:84` | 特殊登录粘贴框（文案归属 01） | 在这里输入登录信息 |
| U-202 | `app/src/main/res/values/strings.xml:199` | 让我喵两句…… | `app/src/main/res/layout/activity_send_dynamic.xml:60` | 发动态输入框 | 让我喵两句…… |
| U-203 | `app/src/main/res/values/strings.xml:200` | 啥都木有 | `app/src/main/res/layout/activity_copy.xml:56` | 复制页在剪贴板为空时。**注意没有末尾的波浪号 `~`，与 U-095 那句不是同一个词** | 啥都木有 |

### U-204 硬编码在各布局里的 16 条 hint

- **位置**：见下表
- **出现在哪**：这些提示语**没有进 `strings.xml`**，直接写在布局里，改起来要逐个布局文件改
- **用途**：表单与调试输入框的提示
- **原文与位置**：

| 原文 | 位置 | 用途 | 改写 |
| --- | --- | --- | --- |
| 请输入个人描述（最多70个字符） | `app/src/main/res/layout/activity_edit_sign.xml:69` | 签名输入框；**括号里是硬约束** | 请输入个人描述（最多70个字符） |
| 昵称 | `app/src/main/res/layout/activity_edit_user_info.xml:67` | 昵称输入框 | 昵称 |
| 生日（YYYY-MM-DD，如 2000-01-01） | `app/src/main/res/layout/activity_edit_user_info.xml:79` | 生日输入框；**格式约定，改则需同步解析代码** | 生日（YYYY-MM-DD，如 2000-01-01） |
| 请输入收藏夹名称 | `app/src/main/res/layout/activity_favorite_folder_edit.xml:68` | 新建/编辑收藏夹 | 请输入收藏夹名称 |
| 请输入收藏夹简介（可选） | `app/src/main/res/layout/activity_favorite_folder_edit.xml:88` | 同上；**「（可选）」不能删** | 请输入收藏夹简介（可选） |
| 手机号/邮箱 | `app/src/main/res/layout/fragment_password_login.xml:37` | 账号密码登录 | 手机号/邮箱 |
| 输入密码 | `app/src/main/res/layout/fragment_password_login.xml:56` | 账号密码登录 | 输入密码 |
| 输入手机号 | `app/src/main/res/layout/fragment_sms_login.xml:37` | 短信登录 | 输入手机号 |
| 短信验证码 | `app/src/main/res/layout/fragment_sms_login.xml:62` | 短信登录 | 短信验证码 |
| 搜索设置项 | `app/src/main/res/layout/activity_setting_main.xml:48` | 设置页搜索框（04 的 `S-002`） | 搜索设置项 |
| 请输入倍数 | `app/src/main/res/layout/activity_setup_ui.xml:120` | 首次启动调整界面时输入缩放倍数 | 请输入倍数 |
| 投票标题 | `app/src/main/res/layout/activity_send_dynamic.xml:163` | 发动态的投票子表单 | 投票标题 |
| 在这里输入请求链接 | `app/src/main/res/layout/activity_test.xml:103` | 测试页（仅调试包） | 在这里输入请求链接 |
| 在这里输入POST数据 | `app/src/main/res/layout/activity_test.xml:115` | 测试页（仅调试包） | 在这里输入POST数据 |
| 这里会输出请求结果 | `app/src/main/res/layout/activity_test.xml:178` | 测试页（仅调试包） | 这里会输出请求结果 |
| 发个友善的弹幕见证当下 | `app/src/main/res/layout/activity_player.xml:635` | 播放器弹幕输入框（见 U-125） | 发个友善的弹幕见证当下 |

---

## 十二、通用按钮与固定字符串（`strings.xml`）

这一批都在 `app/src/main/res/values/strings.xml`，是**跨页面复用的短词**。改一处会同时改变多个页面，所以是**全文案里风险最高的一组**。

### 12.1 `btn_*` 按钮组

| 编号 | 定义位置 | 原文 | 引用位置 | 归属与用途 | 改写 |
| --- | --- | --- | --- | --- | --- |
| U-205 | `strings.xml:202` | 登录 | `activity_login_special.xml:102` | 特殊登录（01 已收） | 登录 |
| U-206 | `strings.xml:203` | 粘贴并登录 | `activity_login_special.xml:128` | 特殊登录（01 已收） | 粘贴并登录 |
| U-207 | `strings.xml:205` | 但是我拒绝 | `activity_login_special.xml:146` | 特殊登录（01 的 `A-076`）；**与 U-174 扫码页那份是同一句话的两个副本** | 但是我拒绝 |
| U-208 | `strings.xml:206` | 表情 | `activity_write_reply.xml:77` | 评论工具栏 | 表情 |
| U-209 | `strings.xml:207` | 图片 | `activity_write_reply.xml:96` | 评论工具栏 | 图片 |
| U-210 | `strings.xml:208` | 发送 | `activity_write_reply.xml:114`、`activity_player.xml:650` | 评论发送 + 弹幕发送**共用同一个字符串**；改它两个地方都会变 | 发送 |
| U-211 | `strings.xml:209` | 哔哩终端，启动！ | `activity_setup_introduction.xml:70` | 首次启动引导（01 已收） | 哔哩终端，启动！ |
| U-212 | `strings.xml:210` | 你还木有登录哦！点击这里登录 | 代码侧使用（`btn_login_tip`） | 未登录提示按钮 | 你还木有登录哦！点击这里登录 |
| U-213 | `strings.xml:211` | 恢复默认 | `activity_setup_ui.xml:67` | 04 的 `S-115` | 恢复默认 |
| U-214 | `strings.xml:212` | 已阅 | `activity_tutorial.xml:49`、`activity_tutorial_pager.xml:55`、`TutorialActivity.kt:57` | 教程页（03 已收；旧链路的 `TutorialActivity.kt:57` 是待清理的副本） | 已阅 |
| U-215 | `strings.xml:213` | 复制选中 | `activity_copy.xml:175` | 复制页 | 复制选中 |
| U-216 | `strings.xml:214` | 复制全部 | `activity_copy.xml:181` | 复制页 | 复制全部 |
| U-217 | `strings.xml:619` | 确认设置 | `activity_setup_ui.xml:266` | 首次启动调整界面完成按钮 | 确认设置 |
| U-218 | `strings.xml:620` | 查看预览 | `activity_setup_ui.xml:259` | 04 的 `S-116` | 查看预览 |
| U-219 | `strings.xml:621` | 复制 | `activity_login_special.xml:72` | 特殊登录的复制按钮（01 已收）。**与 U-064 的 `pagename_copy`「复制」同字不同用途** | 复制 |

### 12.2 `section_ui_*` 与 `text_ui_padding_*` 界面设置小标题

| 编号 | 定义位置 | 原文 | 引用位置 | 归属 | 改写 |
| --- | --- | --- | --- | --- | --- |
| U-220 | `strings.xml:176` | 界面大小 | `activity_setup_ui.xml:111` | 04 的 `S-120`（首次启动也复用） | 界面大小 |
| U-221 | `strings.xml:178` | 横向 | 代码侧使用（`text_ui_padding_horizontal`） | 界面边距的方向标签 | 横向 |
| U-222 | `strings.xml:179` | 纵向 | 代码侧使用（`text_ui_padding_vertical`） | 同上 | 纵向 |
| U-223 | `strings.xml:216` | 圆屏适配 | `activity_setup_ui.xml:86` | 04 的 `S-118` | 圆屏适配 |
| U-224 | `strings.xml:217` | 界面边距 | `activity_setup_ui.xml:150` | 04 的 `S-123` | 界面边距 |

### 12.3 `text_*` 固定字符串组

| 编号 | 定义位置 | 原文 | 引用位置 | 归属与用途 | 改写 |
| --- | --- | --- | --- | --- | --- |
| U-225 | `strings.xml:219` | 当你的天空突然下起了大雨~\n那是我在为你炸乌云~ | `activity_player.xml:116`、`activity_tutorial.xml:36` | **播放器与教程页共用的占位字幕/副标题**（一句歌词）。运行时会被真实内容覆盖，但在视频没字幕时会露出来 | 当你的天空突然下起了大雨~\n那是我在为你炸乌云~ |
| U-226 | `strings.xml:220` | 00:00/00:00 | `activity_player.xml:126` | 播放器进度占位 | 00:00/00:00 |
| U-227 | `strings.xml:221` | 0人在看 | `activity_player.xml:139` | 实时在线人数占位（受 04 的 `S-065` 开关控制） | 0人在看 |
| U-228 | `strings.xml:223` | 解析视频地址…… | `activity_player_jump.xml:7` | 见 U-135 | 解析视频地址…… |
| U-229 | `strings.xml:224` | 发现新版本 | `activity_update.xml:58` | 检查更新页（02 已收） | 发现新版本 |
| U-230 | `strings.xml:225` | 选择范围 | `activity_copy.xml:66` | 复制页小节标题 | 选择范围 |
| U-231 | `strings.xml:226` | *长按选择到开头/结尾* | `activity_copy.xml:169` | 复制页操作提示。**首尾的 `*` 是真的星号**，是作者用来代替「斜体」的土办法（XML 里没写 `<i>`） | *长按选择到开头/结尾* |
| U-232 | `strings.xml:227` | *部分设备可能阉割剪贴板导致无法复制，此处点名表扬小天才* | `activity_copy.xml:187` | 复制页底部吐槽。**「小天才」指小天才手表**；`*…*` 同样是伪斜体 | *部分设备可能阉割剪贴板导致无法复制，此处点名表扬小天才* |

---

## 十三、列表项布局里的**真静态标签**

`cell_*.xml` 里绝大多数 `android:text` 是给预览看的占位（下一节统一处理），但有少数**不会被 runtime 覆盖**，它们就是最终文案。这批最容易漏——因为它们混在一堆占位符中间。

| 编号 | 位置 | 原文 | 出现在哪 | 改写 |
| --- | --- | --- | --- | --- |
| U-233 | `app/src/main/res/layout/cell_reply_action.xml:15` | 写评论 | 评论区操作条（见 U-142） | 写评论 |
| U-234 | `app/src/main/res/layout/cell_reply_action.xml:25` | 回复排序 | 评论区操作条 | 回复排序 |
| U-235 | `app/src/main/res/layout/cell_reply_list.xml:94` | 删除 | 自己评论的操作项 | 删除 |
| U-236 | `app/src/main/res/layout/cell_reply_list.xml:141` | 「 UP主觉得很赞」（前导空格） | UP 主点过赞的评论标记 | 「 UP主觉得很赞」（前导空格） |
| U-237 | `app/src/main/res/layout/cell_reply_list.xml:172` | 共114条回复 | 楼中楼展开（数字会被覆盖） | 共114条回复 |
| U-238 | `app/src/main/res/layout/cell_reply_list.xml:185` | 回复 | 回复按钮 | 回复 |
| U-239 | `app/src/main/res/layout/cell_article_end.xml:124`、`:148`、`:172` | 点赞 / 投币 / 收藏 | 文章（图文）末尾的三连按钮 | 点赞 / 投币 / 收藏 |
| U-240 | `app/src/main/res/layout/cell_dynamic.xml:155`、`:170` | 转发 / 管理 | 动态卡片操作 | 转发 / 管理 |
| U-241 | `app/src/main/res/layout/cell_dynamic_action.xml:13`、`:24`、`:34`、`:44` | 直播列表 / 发动态 / 类型 / 话题广场 | 动态页顶部动作卡片 | 直播列表 / 发动态 / 类型 / 话题广场 |
| U-242 | `app/src/main/res/layout/cell_message_reply.xml:44` | 点击跳转 | 消息卡片 | 点击跳转 |
| U-243 | `app/src/main/res/layout/cell_follow_group.xml:46` | 展开/折叠（`contentDescription`） | 关注列表分组头 | 展开/折叠（`contentDescription`） |
| U-244 | `app/src/main/res/layout/cell_section_header.xml:25` | 0项 | 通用分区头的计数（数字会被覆盖，「项」是固定字） | 0项 |
| U-245 | `app/src/main/res/layout/cell_video_folder.xml:77`、`:96`、`:115` | 重命名 / 拆散 / 取消 | 本地文件夹卡片内操作 | 重命名 / 拆散 / 取消 |
| U-246 | `app/src/main/res/layout/cell_create_folder_button.xml:15` | + 创建收藏夹 | 收藏列表末尾 | + 创建收藏夹 |
| U-247 | `app/src/main/res/layout/activity_simple_refresh.xml:72`、`:85`、`:100` | 全部 / 未看完 / 清除已看完 | **稍后再看页**的筛选条（默认 `visibility="gone"`，只有该页点亮）。文字不会被代码覆盖，见 `WatchLaterActivity.kt:44-63` | 全部 / 未看完 / 清除已看完 |
| U-248 | `app/src/main/res/layout/activity_simple_refresh.xml:129`、`:142`、`:155` | 收藏时间 / 播放量 / 投稿时间 | **收藏夹内容页**的排序条（默认隐藏，`FavoriteVideoListActivity.kt:88-95` 点亮；只改颜色不改文字） | 收藏时间 / 播放量 / 投稿时间 |
| U-249 | `app/src/main/res/layout/activity_simple_refresh.xml:182`、`:195` | 多选 / 删除 | 收藏夹内容页的多选条。「删除」在进入多选后由代码加上选中数 | 多选 / 删除 |
| U-250 | `app/src/main/res/layout/activity_simple_refresh.xml:221` | + 新建分组 | 关注列表的分组模式条（`FollowUsersActivity.kt:73` 点亮） | + 新建分组 |
| U-251 | `app/src/main/res/layout/activity_loading.xml:46`、`app/src/main/res/layout/cell_loading.xml:9` | 加载中（`contentDescription`） | 通用加载页与列表加载项的无障碍描述 | 加载中（`contentDescription`） |
| U-252 | `app/src/main/res/layout/activity_image_viewer.xml:67` | 第0/0张 | 图片查看页计数（见 U-185） | 第0/0张 |
| U-253 | `app/src/main/res/layout/cell_action_button.xml:10` | 操作 | 通用动作按钮的默认文字 | 操作 |

---

## 十四、列表项与控制台示例文本（**设计占位符，不是文案**）

**这一节的东西一律不用改。**列出来有两个目的：一是让你在逐条改写时**放心跳过**它们；二是提供一个判据——如果某条文本是**人名梗 / 重复词 / 明显假数据**，它大概率是占位。

判据（三条任一）：

1. **重复同一段词**（「标题标题标题…」重复 12~16 遍）——用来测省略号；
2. **网络梗或人名**（`蔡徐坤`、`只因你太美！`、`114514`、`你干嘛哈哈哎哟`）；
3. **显然要由接口填的值**（`标题`、`用户名`、`设备名称`、`UP主名字`、`0人参与`）。

| 位置 | 原文 | 为什么是占位 |
| --- | --- | --- |
| `app/src/main/res/layout/cell_article_list.xml:107`、`cell_dynamic_article.xml:101`、`cell_opus.xml:85`、`cell_dynamic_video.xml:102` | 标题标题标题标题标题标题标题标题标题标题标题标题 | 重复词，测省略号 |
| `app/src/main/res/layout/cell_choose.xml:20`、`cell_folder_choose.xml:29` | 文本文本文本 | 重复词 |
| `app/src/main/res/layout/cell_dynamic.xml:29`、`cell_dynamic_child.xml:33`、`cell_collection_info.xml:17`、`cell_reply_list.xml:27`、`cell_user_info.xml:27` | 蔡徐坤 | 人名梗（昵称位置） |
| `app/src/main/res/layout/cell_dynamic.xml:53`、`cell_dynamic_child.xml:47`、`cell_collection_info.xml:30`、`cell_reply_list.xml:51`、`cell_user_info.xml:106` | 只因你太美！ | 梗（签名/正文位置） |
| `app/src/main/res/layout/cell_dynamic.xml:67`、`cell_dynamic_child.xml:63` | 第一次压扁成这样的我~ | 梗（动态正文位置） |
| `app/src/main/res/layout/cell_reply_child.xml:10` | 你干嘛哈哈哎哟 | 梗（楼中楼正文位置） |
| `app/src/main/res/layout/cell_reply_action.xml:38` | 共114514条 | 梗数字（评论数） |
| `app/src/main/res/layout/cell_user_info.xml:121` | 震惊! | 梗（签名位置） |
| `app/src/main/res/layout/cell_setting_choose.xml:19`、`:30`、`:38`、`:49` | 单选类设置项 / 巧克力 / 香草 / 虽然但是，Robin只喜欢纳西妲嘿嘿嘿 | 设置项预览占位（`cell_setting_choose` 是设置项模板）；最后一句是作者私货 |
| `app/src/main/res/layout/cell_setting_input.xml:20`、`:37` | 输入类设置项 / 如花 如火 如诗 如梦 | 同上 |
| `app/src/main/res/layout/cell_setting_list_choose.xml:19`、`:28`、`:37` | 列表类设置项 / 点击选择 / 描述 | 同上 |
| `app/src/main/res/layout/cell_setting_nav.xml:33`、`:47` | 跳转项 / 跳转描述 | 同上 |
| `app/src/main/res/layout/cell_setting_switch.xml:19`、`:29` | 开关类设置项 / 你打开的不是开关，而是通往异世界的大门 | 同上；末句是作者写的段子 |
| `app/src/main/res/layout/cell_setting_title.xml:13` | 标题 | 同上 |
| `app/src/main/res/layout/cell_video_list.xml:44`、`cell_video_local.xml:60`、`cell_page_item.xml:17`、`cell_article_head.xml:41`、`cell_video_folder.xml:35` | 标题 / UP主名字 / P1 标题 / 文件夹名称 | 接口填值 |
| `app/src/main/res/layout/cell_video_list.xml:66` | 11.4万 | 播放量示例 |
| `app/src/main/res/layout/cell_video_folder.xml:44` | 0个视频 | 计数示例 |
| `app/src/main/res/layout/cell_article_head.xml:79`、`cell_article_image.xml:30`、`cell_dynamic_image.xml:28`、`cell_reply_list.xml:124` | 共1张图片 | 图片数示例（真实值拼接） |
| `app/src/main/res/layout/cell_dynamic_vote.xml:24`、`:50` | 投票标题 / 0人参与 | 投票数据 |
| `app/src/main/res/layout/cell_article_head.xml:90` | 正在加载中... | 加载态占位 |
| `app/src/main/res/layout/activity_vip.xml:93` 等 6 处 | 加载中... | 加载态占位（见 U-158） |
| `app/src/main/res/layout/item_vote_option.xml:31`、`item_dialog_terminal.xml:42` | 选项文字 / 条目 | 列表项模板 |
| `app/src/main/res/layout/cell_timeline_episode.xml:32`、`:42` | 标题 / 第1话 | 剧集数据 |
| `app/src/main/res/layout/cell_favorite_folder_list.xml:58` | 收藏夹名称 | 接口填值 |
| `app/src/main/res/layout/cell_follow_group.xml:21`、`cell_group_header.xml:34`、`:49`、`cell_section_header.xml:16` | 分组名称 / 分组 / 分组描述 / 分区标题 | 接口填值 |
| `app/src/main/res/layout/cell_login_record.xml:20`、`:29`、`:37`、`:52` | 设备名称 / 登录方式 / 登录时间 / 位置 | 接口填值（**但字段名本身见 U-168，那一条是要改的**） |
| `app/src/main/res/layout/cell_medal_list.xml:40`、`:57` | 勋章名称 / 主播名称 | 接口填值 |
| `app/src/main/res/layout/cell_topic.xml:24` | 话题 | 接口填值 |
| `app/src/main/res/layout/cell_up_list.xml:53` | UP主 | 接口填值 |
| `app/src/main/res/layout/cell_log.xml:29`、`:37` | 变化说明 / 时间 | 列头模板 |
| `app/src/main/res/layout/cell_private_msg.xml:15` | 用户名 | 接口填值 |
| `app/src/main/res/layout/cell_electric_user.xml:28`、`:40` | 用户名 / 充电留言 | 接口填值 |
| `app/src/main/res/layout/cell_user_info.xml:53`、`:92` | 2.5万粉丝 / 91关注 | 计数示例 |
| `app/src/main/res/layout/cell_subtitle.xml:14` | 让我们说中文 | 字幕示例（真实字幕覆盖） |
| `app/src/main/res/layout/cell_announcement_list.xml:29`、`:39` | 新春快乐！ / 春来早，瑞兽登高… | 公告示例 |
| `app/src/main/res/layout/cell_user_list.xml:55` | 年少时的怦然心动，送给了永远无法企及的人。 | 签名示例 |
| `app/src/main/res/layout/cell_interaction_debug_var.xml:20` | 变量名 | 调试数据 |
| `app/src/main/res/layout/fragment_video_info.xml:113`、`:178`、`:188` | 合集 · 纳西妲 / 标签：??? / 简介简介简介简介简介简介简介 | 详情数据 |
| `app/src/main/res/layout/activity_live_info.xml:200`、`:226`、`:269` | 114人在线 / 已直播 00:00:00 / 标签：??? | 直播数据 |

---

## 十五、死字符串与无引用资源

改文案时最浪费时间的不是漏改，而是**改了一个永远不会显示的东西**，或者反过来**以为改了但其实代码用的是另一个**。这一节把这类情况列全。

| 编号 | 位置 | 原文 | 状况 | 建议 |
| --- | --- | --- | --- | --- |
| U-254 | `app/src/main/res/values/strings.xml:252` | 搜索类别排序 | **无人引用**（`SearchSortActivity.kt:43` 自己硬编码了同字标题，见 U-051） | 要么删掉，要么改成从资源读 |
| U-255 | `app/src/main/res/values/strings.xml:254` | 界面设置 | **无人引用**。疑似旧版本的设置子页标题，后来该页被并入「界面与外观」分组（04 的 `S-007`） | 删除 |
| U-256 | `app/src/main/res/values/strings.xml:266` | 版本更新 | **无人引用**。检查更新页标题实际是布局里硬编码的「检查更新」（02 已收） | 删除 |
| U-257 | `app/src/main/res/values/strings.xml:229` | 页面标题 | 5 个通用列表布局的**占位**（U-060），运行时必被 `setPageName` 覆盖 | 保留（改了也无害） |
| U-258 | `app/src/main/res/layout/activity_simple_refresh.xml:249` 等 | 正在加载… | **`tools:text`，不进 APK** | 不需要改 |
| U-259 | `app/src/main/res/layout/activity_tutorial.xml` 全套 | — | 旧教程链路（`TutorialHelper.showTutorialList()` 已无调用者），`03-tutorials.md` 已判定为死链路 | 见 03 |
| U-260 | 全工程的 `android:text="…"` 与代码 `setText` 同字重复 | 见上文各处标注 | **「双真相」共 11 组**（同一句话有两处独立定义，改一处必漏）：① 推荐 → `U-038` / `U-049`；② 消息四个分类名 → `U-107` / `U-109`；③ 互动调试标题 → `U-002` / `U-192`；④ 空态「啥都木有~」 → `U-095` / `U-099`（代码一处 + 4 个布局）；⑤ 翻页「没有更多了」「正在加载…」 → `U-097` / `U-098`（两个基类各一份）；⑥ 音量显示（三副本） → `U-126`；⑦ 直播字段名（连字段名都变了） → `U-129` / `U-131`；⑧「知道了」 → `U-179`（`activity_qr_login_help.xml:43`）与 `TerminalDialog.kt:259`；⑨ 弹窗按钮 → `U-195` 与 `TerminalDialog.kt:148`/`:149`/`:232`/`:233`/`:259`；⑩「但是我拒绝」 → `U-207`（`strings.xml:205`，01 的 `A-076`）与 `U-174`（`fragment_qr_login.xml:157` 硬编码那份）；⑪「共N张图片」（4 副本） → `U-134` / `U-147` | **改一处必须全改**，否则同一句话在不同路径显示不同措辞 |

---

## 覆盖性自查

### 已扫描

- `app/src/main/res/layout/` 下**全部 144 个布局文件**的 `android:text` / `android:hint` / `android:contentDescription` 中含中文的项——grep 命中 **348 处**，全部过了一遍，按「真文案 / 占位符」分流到第十三、十四节。
- `app/src/main/java/com/RobinNotBad/BiliClient/activity/` 下全部硬编码页面标题：`setPageName(` **61 处命中**（含 `BaseActivity.kt:199` 的定义，故实际调用 60 处，其中 1 处是变量 `groupTitle`）。
- **另外三套设置标题的写法**（容易漏，特此单列）：
  1. `pageName.text = "…"`：`DynamicInfoActivity.kt:38`（U-138）、`LocalListActivity.kt:88`、`:293`、`:299`、`ListChooseActivity.kt:37`（U-189）——共 5 处；
  2. `title.text = "…"`：`RankingActivity.kt:41`（全站排行榜）、`PreciousActivity.kt:44`（入站必刷）、`PopularActivity.kt:42`（热门）——**这 3 处与 04 的 `S-180`/`S-181`/`S-179` 同字但代码位置不同**；
  3. `QualityChooserActivity.kt:56`、`MultiPageActivity.kt:34`、`LocalPageChooseActivity.kt:39`（U-190/U-191）。
- `strings.xml` 的 `pagename_*`（36 条）、`hint_*`（8 条）、`btn_*`（14 条）、`section_ui_*`（3 条）、`text_ui_padding_*`（2 条）、`text_default_*`（3 条）、`text_getting_video` / `text_new_version_released` / `text_select_range` / `text_copy_*`（5 条）——逐条核对了引用位置。
- `strings.xml` 里 `strings.xml:77-124` 的 `eggs` 数组、`tutorial_*` 数组、`update_*`、`privacy_*`、`about_*`、`desc_*`、`donate_*`、`egg_*`、`err_*`、`aria2_*`、`cache_*`、`icon_license`、`setting_lab_*` 等**已确认归属 01/02/03/04/06/07**，本文件未重复收录。

### 明确排除（归属其它文件，05 只做交叉引用）

| 排除对象 | 归属 |
| --- | --- |
| `activity_setting_about.xml`、`activity_setting_opensource.xml`、`activity_setup_introduction.xml`、`activity_feedback.xml`、`activity_feedback_detail.xml`、`activity_login_special.xml` 的正文 | 01 |
| `activity_update.xml`、`activity_update_history.xml` | 02 |
| `activity_tutorial.xml`、`activity_tutorial_pager.xml`、`activity_tutorial_manager.xml` | 03 |
| `activity_setting_*.xml` 与 `activity_setting_ui_preview.xml` 的设置条目 | 04 |
| `activity_dialog.xml` / `activity_confirm_dialog.xml` / `activity_input_dialog.xml` 的**按钮语义**（已在 U-195 登记位置，措辞归 06） | 06 |
| 全部 `Toast` / `TerminalDialog` / `MsgUtil.*` 调用点 | 06 |
| 搜索关键词彩蛋、设置页长按「关于」、关于页连点 | 07 |

### 存疑

1. **`activity_todo_list.xml`（U-188）的入口未确认**。全文案 grep 没找到 `TodoListActivity` 的启动点，推测是从关于页某个隐藏入口或调试入口进；若确认它已无入口，这条应当并入第十五节的死页面。
2. **`cell_user_info.xml:174`「该用户存在争议行为，已冻结其帐号功能的使用」（U-153）**：这是平台措辞还是本地写死的？未能从代码确认，**推测是接口字段的兜底文案**——若确实是本地写死，改写时要注意与官方 App 的说法保持一致。
3. **`cell_message_reply.xml:26`「全体目光向我看齐…」（U-113）**：运行时会被真实回复内容覆盖，但**不能 100% 排除**某些分支下会显示它，故保留在第八节。
4. **`activity_vip.xml:93` 等 6 处「加载中...」**（U-158）与 `activity_myspace.xml:89`（U-106）用的是 `...`，而 `RefreshListActivity` 的两个常量用的是 `…`。全文案里两种写法混用，本次未统一，**建议改写时顺手统一为 `…`**。
5. 布局里含中文的项共 **348 处**（grep 命中数），本文件正式编号 **260 条**（U-001~U-260），差额全部去了第十四节的占位符表与第十五节的死字符串——**没有未分类的残留**。
