# AsPlugin

基于 IntelliJ Platform 的 Android Studio 插件初始工程，保留构建、运行、打包及界面文案管理基础。当前未注册菜单、侧边栏、服务或启动活动。

插件显示名称为 `AS Starter`，插件 ID 为 `com.github.zhangwei0621.asplugin`，初始版本为 `0.0.1`。

## 开发环境

- IntelliJ IDEA。
- JDK 25，Project SDK 和 Gradle JVM 均选择该版本。
- 当前目标为本地 Android Studio，Build 为 `AI-262.9437.185.2621.16467767`。
- 当前插件兼容范围为 IntelliJ Platform `262.*`。

本机 Android Studio 内置的 JBR 可作为 JDK 25 使用：

```text
/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

## 目标 IDE

在 `gradle.properties` 中修改 `platformLocalPath` 指向 Android Studio 安装目录，macOS 路径应包含 `.app/Contents`。也可以通过命令行覆盖：

```sh
sh ./gradlew runIde -PplatformLocalPath="/Applications/Android Studio.app/Contents"
```

更换目标 IDE 版本时，需要同步检查 `build.gradle.kts` 中的 Java Toolchain 和插件兼容范围。配置 CI 时，将本地 IDE 依赖改为可下载的目标 IDE 依赖。

## 常用命令

执行前确认 `JAVA_HOME` 指向 JDK 25。macOS 可使用本机 Android Studio 的 JBR：

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
sh ./gradlew runIde
sh ./gradlew check
sh ./gradlew buildPlugin
sh ./gradlew verifyPluginStructure
```

`runIde` 启动隔离的测试 Android Studio；`buildPlugin` 将插件 ZIP 输出到 `build/distributions/`。当前没有测试用例，`check` 用于执行已有构建检查。

IDEA 中保留了 `Run Plugin`、`Run Tests` 和 `Run Verifications` 运行配置，其中 `Run Verifications` 执行插件结构校验。发布前还需要配置验证目标并执行 `verifyPlugin`，检查实际 API 兼容性。

## 工程结构

| 路径 | 用途 |
| --- | --- |
| `settings.gradle.kts` | 工程名称、Gradle 插件版本和仓库 |
| `build.gradle.kts` | 目标 IDE、Java Toolchain、依赖和兼容范围 |
| `gradle.properties` | 包名、版本、本地 IDE 路径及缓存设置 |
| `src/main/resources/META-INF/plugin.xml` | 插件身份、依赖和功能注册 |
| `src/main/kotlin/com/github/zhangwei0621/asplugin/` | Kotlin 功能代码和 `MyBundle` |
| `src/main/resources/messages/MyBundle.properties` | 集中管理界面文案 |

## 开始开发

按需求新增 `AnAction`、`ToolWindowFactory` 或 `Configurable`，并在 `plugin.xml` 中注册。界面文案统一放入 `MyBundle.properties`，通过 `MyBundle` 获取。

JUnit 4 依赖已保留，可按需创建 `src/test/kotlin/` 添加逻辑测试。需要 IDE 集成测试时，再添加 IntelliJ Platform 测试框架依赖。

工程未配置 GitHub 自动构建、发布、签名或 Marketplace 上传流程，后续按实际需要添加。
