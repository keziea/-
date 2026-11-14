# AGENTS.md — 逐日 / Day by Day · 总操作文档

> 本文件是**唯一入口**。任何 AI / 开发者接手本项目，只需读完本文件，就能知道：
> 这是什么、代码在哪、按什么顺序读分层文档、怎么把它编译成 APK。
> 最后更新：2026-11-14 · 版本 v3.1.1

---

## 1. 一句话定位

**「逐日」是一款纯本地、零广告、零账号的安卓待办与坚持记录 App。**
你可以提前给某一天安排任务（写名字、可选写详情、可选时间），给任务设置「不重复 / 每天 / 每周几 / 每月某日」，
打开 App 勾一下就算完成；也可以写两笔笔记，最后在「总结」页看到自己的连续天数、每日完成度、近 8 周热力与写作轨迹。

设计目标排序：**好看 > 顺手 > 功能多**。功能刻意克制，只做「安排 + 完成 + 复盘」这一条主链路。

## 2. 技术栈（版本已锁定，勿随手升级）

| 层 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 语言 | Kotlin | 2.2.20 | 无 KSP / kapt，编译快 |
| UI | Jetpack Compose + Material 3 | BOM 2025.09.01 | 全自绘界面，不引第三方 UI 库 |
| 构建 | Android Gradle Plugin | 8.12.3 | 需 Gradle 8.13 |
| 构建 | Gradle | 8.13 | 装在 `E:\AndroidDev\gradle-8.13` |
| JDK | Temurin JDK 17 | 17.0.20.1+1 | 装在 `E:\AndroidDev\jdk-17.0.20.1+1` |
| SDK | Android SDK Platform | compileSdk / targetSdk = 36 | `D:\Android\SDK` |
| 最低版本 | minSdk | 23（Android 6.0） | core library desugaring 提供 java.time，兼容 MuMu 6 等旧模拟器 |
| 存储 | 本地 JSON 文件 | org.json（系统内置） | 无数据库、无网络、无权限 |
| 架构 | 单 Activity + StateFlow + Compose State | — | 无 ViewModel 框架、无 DI 框架 |

**依赖清单（全部）**：`core-ktx`、`activity-compose`、`lifecycle-runtime-compose`、`compose.ui`、`compose.material3`，以及编译期 `desugar_jdk_libs`（用于 java.time 脱糖）。
没有 Room / Hilt / Navigation / Retrofit / Coil / 图标扩展包。APK 体积就是这个量级。

## 3. 顶层目录地图

```
工作安排软件/
├── AGENTS.md                    ← 你正在读的总文档（唯一入口）
├── docs/                        ← 分层操作文档（见第 4 节阅读顺序）
├── settings.gradle.kts          ← 仓库源、模块声明
├── build.gradle.kts             ← 根构建脚本（插件版本声明）
├── gradle.properties            ← JVM 参数、AndroidX 开关
├── local.properties             ← 本机 SDK 路径（不入版本库）
├── app/
│   ├── build.gradle.kts         ← app 模块：SDK 版本、依赖、签名、混淆
│   ├── proguard-rules.pro       ← R8 规则
│   └── src/main/
│       ├── AndroidManifest.xml  ← 无任何权限声明（离线 App）
│       ├── java/com/zhuri/daybyday/
│       │   ├── MainActivity.kt          ← 唯一 Activity，edge-to-edge 入口
│       │   ├── ZhuriApp.kt              ← Application，持有唯一 Repository
│       │   ├── model/Task.kt            ← 任务模型 + 重复规则判定
│       │   ├── model/Diary.kt           ← 笔记模型 + 写作统计（连续 / 热力）
│       │   ├── data/TaskRepository.kt   ← StateFlow 状态 + JSON 持久化
│       │   ├── util/Dates.kt            ← 日期工具、中文格式化
│       │   └── ui/
│       │       ├── theme/Theme.kt        ← 色板 / 字体 / 圆角 / 动效令牌
│       │       ├── Root.kt              ← 根容器：四个 Tab + 自适应内容列 + 导航 + FAB
│       │       ├── TodayScreen.kt       ← 今日（日期条 + 清单 + 当天笔记）
│       │       ├── CalendarScreen.kt    ← 日历（月视图 + 当天清单）
│       │       ├── DiaryScreen.kt       ← 笔记（时间轴 + 写作统计入口）
│       │       ├── DiaryEditor.kt       ← 笔记编辑器（沉浸式底部抽屉）
│       │       ├── StatsScreen.kt       ← 总结（任务统计 + 写作统计）
│       │       ├── EditorSheet.kt       ← 任务编辑器（底部抽屉）
│       │       └── components/          ← 复用组件（见 03 文档）
│       └── res/                         ← 主题、图标、字符串
├── artifacts/                   ← 构建产物（APK）与验收截图
│   ├── 逐日-v3.1.1.apk
│   └── screenshots/             ← 今日 / 日历 / 笔记 / 总结 / 编辑器 / 暗色 / 平板
├── scripts/
│   ├── seed_demo.py             ← 生成演示数据（任务 + 笔记），用于截图与统计校验
│   └── new_release.py           ← 新建版本说明并刷新 docs/releases 索引
├── signing/zhuri-release.jks    ← Release 签名密钥（密码见 docs/01 第 6 节）
└── .workbuddy/memory/           ← 会话记忆（AI 用，非代码）
```

## 4. 分层文档阅读顺序（按需读，不必全读）

| # | 文档 | 什么时候必须读 |
|---|---|---|
| ① | `docs/00-项目总览.md` | **任何人第一次接手必读**。产品定位、功能边界、页面结构与信息架构 |
| ② | `docs/01-目录结构与构建.md` | 要编译、要改环境、要发版时读。含完整文件职责表与可复制的构建命令 |
| ③ | `docs/02-功能规格.md` | 要改功能逻辑时读。重复规则算法、统计口径、边界情况全部在这里 |
| ④ | `docs/03-视觉与交互规范.md` | 要改界面时读。色板、字号、间距、圆角、动效、每个组件的尺寸规格 |
| ⑤ | `docs/04-数据与状态.md` | 要改数据结构 / 存储 / 迁移时读。JSON Schema 与读写时机 |
| ⑥ | `docs/05-验收与测试.md` | 交付前读。验收清单、性能指标、已知限制 |
| ⑦ | `docs/CHANGELOG.md` | 每次改动后追加。改了什么、为什么改 |
| ⑧ | `docs/releases/README.md` | 发版前必看。逐版本用户说明索引与写法 |
| ⑨ | `docs/06-发布流程.md` | 要发版时读。版本号规则、发布说明模板、双推步骤 |

**推荐路径**：
- 只想跑起来 → ① → ②
- 想加一个功能 → ① → ③ → ④ → ②
- 想改视觉 → ① → ③ → ②
- 想复查质量 → ⑤

## 5. 编译与安装（复制即可执行）

环境变量（每次新终端都要设，或写进系统环境）：

```bash
export JAVA_HOME="/e/AndroidDev/jdk-17.0.20.1+1"
export ANDROID_HOME="/d/Android/SDK"
export GRADLE_USER_HOME="/e/AndroidDev/gradle-home"
export PATH="$JAVA_HOME/bin:/e/AndroidDev/gradle-8.13/bin:$PATH"
```

构建 Debug 包：

```bash
cd "D:/vibeCoding/工作安排软件"
gradle assembleDebug --no-daemon
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

构建 Release 包（已内置签名配置，可直接安装）：

```bash
cd "D:/vibeCoding/工作安排软件"
gradle assembleRelease --no-daemon
# 产物：app/build/outputs/apk/release/app-release.apk → 复制到 artifacts/逐日-v1.3.apk
```

安装到已连接的设备 / 模拟器：

```bash
export MSYS_NO_PATHCONV=1   # Git Bash 下必须，否则 /data/... 会被转成 Windows 路径
/d/Android/SDK/platform-tools/adb.exe install -r artifacts/逐日-v1.3.apk
/d/Android/SDK/platform-tools/adb.exe shell am start -n com.zhuri.daybyday/.MainActivity
```

> 本机已配好一个 720×1600 的 AVD（名为 `zhuri`），启动命令：
> ```bash
> "$ANDROID_HOME/emulator/emulator.exe" -avd zhuri -no-snapshot -no-audio \
>   -no-boot-anim -gpu swiftshader_indirect -no-window -memory 3072 -cores 6
> ```
> 首次启动约 3 分钟，之后 30 秒内可用。AVD 是手写的配置文件（`~/.android/avd/zhuri.avd`），
> 原因见 `docs/01` 第 8.4 节。

**注意**：项目路径含中文，`gradle.properties` 里的 `android.overridePathCheck=true` 不能删，否则 AGP 会直接拒绝构建。

## 6. 硬性约定（写代码前必看）

1. **文档先行**：新增/删除功能、改交互、改视觉规范、改数据结构，必须**先改文档再改代码**，改完补 `CHANGELOG.md`。
2. **不引第三方 UI / 图标库**：所有图标用 `Path` 自绘或 Compose 内置图标，保持 APK 小、风格统一。
3. **颜色、字号、间距只能来自令牌**：`ui/theme/Theme.kt` 里的 `Zhuri.colors` / `Zhuri.type` 是唯一来源，禁止在页面里写死 `Color(0xFF...)`。
4. **不申请任何权限**：不联网、不通知、不读存储。数据只写自己沙盒的 `filesDir`。
5. **不阻塞主线程**：JSON 读写放在 `Dispatchers.IO`；列表全部用 `LazyColumn` + 稳定 `key`。
6. **暗色模式必须同步可用**：每个新颜色都要给出 light / dark 两套值。
7. **中文文案**：全部面向用户的字符串写中文，不出现「未命名项目」这类占位词。
8. **发版必须留说明**：每发一个版本，先运行 `python scripts/new_release.py <版本> "<标题>" --type <类型> --summary "<摘要>"`，
   补全「新增 / 改进 / 修复」，再改 `versionCode` / `versionName` 打包含。没有发布说明不许打包。
   一个版本一个文件，不按 bug 建文件夹；普通 bug 并入下一版，紧急修复发补丁版本（如 v3.0.1）。

## 7. 当前状态（v3.1.1）

| 项 | 状态 |
|---|---|
| 今日清单（日期条 + 勾选 + 详情展示 + 当天笔记） | ✅ |
| 任务编辑（标题 / 详情 / 日期 / 重复 / 时间） | ✅ |
| 重复规则（不重复 / 每天 / 每周几 / 每月某日） | ✅ |
| 日历月视图（有任务的日期打点 + 回到今天 + 翻月动画） | ✅ |
| 笔记（按日分组、一天多篇、可起标题、补记最近 7 天） | ✅ |
| 总结页（连续天数 / 今日 / 平均 / 累计 / 近 7 天柱状 / 近 8 周热力） | ✅ |
| 写作统计（篇数 / 字数 / 写作天数 / 连续 / 最长 / 12 周热力） | ✅ |
| 四栏悬浮导航 + 转场 / 按压缩放动效 | ✅ |
| 手机 / 平板自适应（内容 720dp 居中，导航与 FAB 跟随） | ✅ |
| 旧设备兼容（minSdk 23，MuMu 6 / Android 6+） | ✅ |
| 暗色模式跟随系统 | ✅ |
| 本地 JSON 持久化 + 原子写入 | ✅ |
| 品牌图标（自适应 + Android 13 主题图标单色层） | ✅ |
| 启动动画（620ms 日升，系统启动画面无缝衔接） | ✅ |
| 每日笔记（同一天多篇 / 自定义分类 / 选择器 / 标题正文搜索） | ✅ |
| 笔记统计（篇数、字数、连续天数、分类占比、12 周热力图） | ✅ |
| 完成当天全部日程庆祝（非阻塞浮层，每次完成触发） | ✅ |
| 十座连续奖杯（铜 / 银 / 金 / 玉 / 铂，奖杯墙 + 解锁动效） | ✅ |
| 日历当天摘要（日程完成 x/y + 笔记 n 篇 / 分类，可展开） | ✅ |
| 未完成复盘（近 7 天列表 + 一键补记 + 日历成功/未完成） | ✅ |
| 备份导出 / 恢复（系统文件选择器，含全部数据） | ✅ |
| 大字号适配（回到今天不裁切、日历动作明确） | ✅ |
| 日历添加入口归位（去掉遮挡卡片的 FAB） | ✅ |
| 逐版本发布说明（docs/releases + 自动索引脚本） | ✅ |
| Release APK（R8 + 签名） | ✅ |

## 8. 远程仓库

同一份代码推了两个远端，提交内容完全一致：

| 远端名 | 地址 | 分支 |
|---|---|---|
| `origin` | `git@github.com:keziea/-.git` | `main` |
| `gitee` | `git@gitee.com:azs_A/day-by-day.git` | `main` |

日常同步：

```bash
git push origin main
git push gitee main
```

**授权方式**：两个远端都走 SSH，用的是本机 `~/.ssh/id_ed25519`
（GitHub 账号 `keziea`，Gitee 账号 `江岳阁@azs_A`，均已添加该公钥）。

> 本机 **HTTPS 通道到 github.com 是不通的**（git 报 `schannel: failed to receive handshake`，
> 走代理报 `CONNECT 502`），只有 SSH 能出网。所以不要改回 HTTPS 地址，否则推不上去。

**注意**：仓库里**不含** `signing/*.jks` 与 `keystore.properties`。
换机器 clone 后 release 会自动退回 debug 签名；要出正式包，需自行补这两个文件。
