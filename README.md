# Android 应用快速构建框架

本仓库提供一套 **可复用的 Android 工程骨架与构建约定**，用于在统一技术栈下快速搭建、迭代原生应用。

下文汇总默认采用的 **语言、构建工具与依赖分层**，便于对齐版本与选型。

---

## 语言与构建

| 项目 | 说明 |
|------|------|
| 语言 | Kotlin **2.0** |
| JVM | Java **11** |
| Android Gradle Plugin | **8.5** |
| 依赖管理 | **Gradle Version Catalog**（集中声明版本） |
| SDK | `compileSdk` / `minSdk` / `targetSdk` 按工程与渠道统一配置 |

---

## 核心框架（AndroidX）

- **AndroidX**：`Core`、`AppCompat`、`Activity`、`Lifecycle`
- **布局**：`ConstraintLayout`
- **视图绑定**：`ViewBinding`

---

## 依赖注入

- **Hilt**（基于 Dagger 的 Android 官方推荐方案）

---

## 网络与数据

- **Retrofit** — HTTP 接口
- **Gson** — JSON 序列化
- **OkHttp Logging** — 调试期请求日志

---

## 图片

- **Coil** — 图片加载
- **Coil GIF** — GIF 支持

---

## 分析与归因

- **Adjust** — 移动归因与营销分析
- **Install Referrer** — 安装来源（与商店/归因链路配合）

---

## 按渠道可选（Firebase）

通过渠道或构建变体按需开启，常见组合：

- **Firebase BOM** — 统一 Firebase 库版本
- **Analytics** — 分析
- **Messaging** — 推送（FCM）

---

## 其他

- **AndroidX Browser** — Custom Tabs 等浏览器能力封装

---

## 测试

| 类型 | 依赖 |
|------|------|
| 单元测试 | **JUnit 4** |
| Android 仪器化 | **AndroidX JUnit** |
| UI 测试 | **Espresso** |

---

## 使用说明

实际工程中的具体版本号、渠道开关与 `minSdk`/`targetSdk` 应以 **Version Catalog**、`app/build.gradle` 及渠道 `buildconfig` 为准；升级 AGP / Kotlin 时请对照 [Android Gradle Plugin 发行说明](https://developer.android.com/build/releases/gradle-plugin) 与 Kotlin 兼容性表。
