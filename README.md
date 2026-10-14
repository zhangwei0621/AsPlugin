# JsonDiff

基于 IntelliJ Platform 的 Android Studio JSON 比对插件，提供自动格式化和左右双栏差异高亮。

插件名称、ID、版本及最低兼容 Build 统一维护在 [gradle.properties](gradle.properties) 中。

## 安装与打开

1. 在 Android Studio 中打开 `Settings > Plugins`，点击齿轮并选择 `Install Plugin from Disk`。
2. 选择 `build/distributions/` 下生成的插件 ZIP，无需解压，按 IDE 提示重启。
3. 打开任意项目，从 `Tools > JsonDiff` 或底部 `JsonDiff` 工具窗口进入比对界面。

插件适用于 API 响应核对、本地配置比对和调试数据检查。JSON 内容在本地处理，比对操作无需 Android SDK 或模拟器。安装兼容范围由构建脚本根据配置注入，详见[项目配置](#项目配置)。后续 IDE 仍需通过兼容性校验确认。

版本 `1.0.0` 已通过以下 Plugin Verifier API 兼容性检查（2026-10-10）：

| 校验目标 | Build | 结果 |
| --- | --- | --- |
| Android Studio Giraffe 2022.3.1 Patch 2 | `AI-223.8836.35.2231.11090377` | Compatible |
| 当前本机 Android Studio | `AI-262.9437.185.2621.16467767` | Compatible |

此外，10 项 JUnit 测试通过；安装包在 Java 17 与当前 AS 的 Java 25 环境中通过 JSON 格式化和严格校验检查。API 校验不等同于覆盖所有 IDE 版本的界面实测。

## 使用教程

### 比对字段修改与新增

左侧粘贴原始 JSON：

```json
{"name":"测试","count":1,"enabled":true}
```

右侧粘贴待比对 JSON：

```json
{"name":"测试","count":2,"enabled":false,"extra":"新增"}
```

默认开启自动格式化，停止输入约 600 毫秒后，两侧会展开为带缩进的多行 JSON。预期可观察到以下差异：

| 字段 | 左侧 | 右侧 | 预期效果 |
| --- | --- | --- | --- |
| `name` | `测试` | `测试` | 没有差异 |
| `count` | `1` | `2` | 修改高亮 |
| `enabled` | `true` | `false` | 修改高亮 |
| `extra` | 不存在 | `新增` | 新增高亮 |

使用 Diff 工具栏中的上一处、下一处差异按钮逐项检查，也可启用同步滚动。高亮颜色随 IDE 主题变化。把右侧替换为与左侧相同的内容后，应显示无差异。

### 排除对象键顺序差异

左侧输入：

```json
{"version":1,"enabled":true}
```

右侧输入：

```json
{"enabled":true,"version":1}
```

勾选顶部“对象键排序”，会立即格式化两侧，并按键名递归排序对象；两侧结果相同，键顺序造成的差异消失。数组元素顺序保持不变，因此 `[1,2]` 与 `[2,1]` 仍存在差异。

排序会修改编辑器中的键顺序。关闭排序选项不会恢复之前的顺序，可通过撤销恢复。

### 处理格式错误和重复键

尝试删除一侧 JSON 的右花括号，底部应显示该侧的格式错误，悬停状态文字可查看错误路径，原文保持不变。修正后会自动重新校验和格式化。

也可以输入以下内容验证重复键检查：

```json
{"count":1,"count":2}
```

底部应提示重复键，原文不会被格式化覆盖。仅支持标准 JSON，键名和字符串需使用双引号，不支持注释、尾随逗号及重复键。

### 工具栏操作

图标按钮可通过悬停查看名称。

| 控件 | 操作效果 |
| --- | --- |
| 格式化两侧 JSON | 立即格式化合法的一侧或两侧；错误输入保持原文 |
| 交换左右 JSON | 交换两侧内容，重新更新校验和差异 |
| 清空两侧 JSON | 清空本次输入，开始新的比对 |
| 自动格式化 | 默认开启；关闭后仍会校验输入和更新文本差异 |
| 对象键排序 | 默认关闭；切换时立即格式化，开启后递归排序对象键 |

关闭自动格式化后，不同的缩进或换行也可能产生差异，可通过手动格式化统一排版。

底部显示两侧校验状态和行数、字符数；窄窗口优先显示校验状态。输入错误或存在重复键时，悬停状态文字可查看错误路径，原文保持不变。格式化保留 `null` 和数字精度；支持对象、数组及其他合法 JSON 顶层值。撤销或重做后不会立即再次自动格式化，可继续编辑或手动格式化。

当前输入内容仅保留在当前工具窗口会话中，关闭项目后不保存。

## 常见问题

- **找不到 JsonDiff 入口：**确认插件已启用并完成重启，且当前已经打开项目，再查看 `Tools > JsonDiff`。
- **安装后仍看到旧的插件介绍：**重新从磁盘安装最新 ZIP 并按提示重启；重新打包不会自动替换已安装的插件。
- **测试 IDE 要求下载 SDK：**`runAndroidStudio` 使用独立配置，首次启动可能显示 Android Studio 初始化向导。也可直接把安装包安装到日常使用的 IDE 中测试。
- **关闭项目后输入消失：**当前版本不保存比对内容，关闭项目前请自行保留需要的数据。

## 开发环境

- IntelliJ IDEA。
- JDK 17 或 21；Gradle JVM 建议选择 JDK 21，编译 Toolchain 使用 JDK 17。
- 默认使用 IntelliJ IDEA Community `2022.3.3`（Platform `223`）作为编译基线，首次构建会下载旧版平台以检查 API。
- 插件生成 Java 17 字节码，Kotlin API 目标为 1.7，适配旧版 IDE 的运行库。
- 兼容性校验目标为 Android Studio Giraffe `2022.3.1 Patch 2` 和本机 Android Studio。

当前构建使用 Gradle Wrapper 和版本目录指定的 Kotlin 编译器，以保留对旧版平台和 Kotlin API 的支持。本机可用的 JDK 路径：

```text
/Users/zw/Library/Java/JavaVirtualMachines/temurin-21.0.12.1/Contents/Home
/Users/zw/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
```

## 目标 IDE

发布构建使用 `gradle.properties` 中的 `platformVersion` 指定最低支持平台。需要启动本机 Android Studio 调试时，执行 `runAndroidStudio`；`verificationLocalPath` 指向 AS 安装目录，macOS 路径应包含 `.app/Contents`：

```sh
sh ./gradlew runAndroidStudio -PverificationLocalPath="/Applications/Android Studio.app/Contents"
```

发布时使用默认旧版基线重新构建，避免引入新版专属 API。`verificationLocalPath` 指向当前 AS 的安装目录，可用于兼容性校验，不影响编译基线。

## 常用命令

执行前确认 `JAVA_HOME` 指向 JDK 21，且本机安装了 JDK 17 供编译 Toolchain 使用：

```sh
export JAVA_HOME="/Users/zw/Library/Java/JavaVirtualMachines/temurin-21.0.12.1/Contents/Home"
sh ./gradlew runAndroidStudio
sh ./gradlew check
sh ./gradlew buildPlugin
sh ./gradlew verifyPluginStructure
sh ./gradlew verifyPlugin
```

`runAndroidStudio` 启动隔离的本机 Android Studio，`runIde` 启动编译基线 IDEA；两者均使用最低平台编译出的插件。`buildPlugin` 将插件 ZIP 输出到 `build/distributions/`，可通过 IDE 的“从磁盘安装插件”安装。`check` 包含 JSON 格式化和光标映射的 JUnit 测试。`verifyPlugin` 会对旧版和本机 AS 进行 API 兼容性校验，首次执行会下载校验工具及旧版 AS，报告位于 `build/reports/pluginVerifier/`。

## 项目配置

项目配置统一在 [gradle.properties](gradle.properties) 中维护：

| 配置项 | 用途 |
| --- | --- |
| `pluginId` | 插件固定标识 |
| `pluginName` | 插件显示名称 |
| `version` | 插件版本及安装包版本 |
| `pluginVendor` | 插件作者 |
| `pluginSinceBuild` | 最低兼容 IDE Build |
| `platformVersion` | 编译使用的 IDEA Community 版本 |
| `verificationLocalPath` | 本机 AS 路径，用于运行和兼容性校验 |
| `kotlin.stdlib.default.dependency` | 关闭自动引入 Kotlin 标准库，使用 IDE 自带版本 |
| `org.gradle.configuration-cache` | 缓存构建配置 |
| `org.gradle.caching` | 缓存任务产物 |

插件元数据由 [build.gradle.kts](build.gradle.kts) 注入 `plugin.xml`，安装版本上限也在该脚本中定义。Kotlin、Shadow 及 Gson、JUnit 的版本统一维护在 [gradle/libs.versions.toml](gradle/libs.versions.toml) 中，通过 `libs` 别名引用。Foojay 和 IntelliJ Platform settings 插件在版本目录访问器生成前加载，因此版本维护在 [settings.gradle.kts](settings.gradle.kts) 中；项目中的 IntelliJ Platform 插件复用 settings 插件加载的同版本实现。

插件介绍来自 `docs/plugin-description.html`，更新说明统一维护在 `docs/plugin-change-notes.html` 中。HTML 文件仅包含正文，无需编写 CDATA。

插件品牌图标为 `src/main/resources/META-INF/pluginIcon.svg`，深色主题使用同目录的 `pluginIcon_dark.svg`，IDE 自动识别。这组 40×40 图标组合了 Lucide 的 [Braces](https://lucide.dev/icons/braces) 与 [Arrow Left Right](https://lucide.dev/icons/arrow-left-right)：蓝色、玫红色花括号表达 JSON，绿色双向箭头表达数据比对；ISC 许可证保留在同目录的 `LICENSE-LUCIDE.txt` 中。

工具窗口和 Tools 菜单入口通过 `ui/JsonDiffIcons.kt` 共用品牌图标，并缩放为 16×16；`plugin.xml` 使用该类的静态字段注册工具窗口图标。

IDEA 中保留了 `Run Plugin`、`Run Tests` 和 `Run Verifications` 运行配置，其中 `Run Verifications` 执行插件结构校验。发布前执行 `verifyPlugin` 检查实际 API 兼容性。

## 工程结构

| 路径 | 用途 |
| --- | --- |
| `settings.gradle.kts` | 工程名称、settings 插件加载和仓库 |
| `gradle/libs.versions.toml` | 依赖库、构建插件的版本与别名 |
| `build.gradle.kts` | 目标 IDE、Java Toolchain、依赖和兼容范围 |
| `gradle.properties` | 插件元数据、兼容下限、最低编译平台、校验 IDE 路径及缓存设置 |
| `src/main/resources/META-INF/plugin.xml` | 插件依赖、文案资源和功能注册 |
| `docs/plugin-description.html` | 精简的插件文字介绍 |
| `docs/plugin-change-notes.html` | 插件详情中的版本更新说明 |
| `src/main/kotlin/com/github/zhangwei0621/asplugin/jsondiff/ui/` | Diff 面板、工具栏布局、工具窗口及菜单入口 |
| `src/main/kotlin/com/github/zhangwei0621/asplugin/jsondiff/logic/` | 比对控制器、后台格式化、输入校验及文档更新 |
| `src/main/kotlin/com/github/zhangwei0621/asplugin/jsondiff/util/` | 光标映射和界面文案资源解析 |
| `src/main/resources/messages/MyBundle.properties` | 集中管理界面文案 |
| `src/test/kotlin/com/github/zhangwei0621/asplugin/jsondiff/` | 按 ui、logic、util 分包的布局、格式化及光标映射测试 |

## 开始开发

JsonDiff 使用插件自带的 Gson 严格解析 JSON，构建时通过 Shadow 迁移到独立包名，避免与 IDE 自带 Gson 冲突；界面复用原生双栏 Diff 编辑器。解析在后台执行，自动格式化只应用最新输入对应的结果。工具窗口销毁后，监听器、定时任务及 Diff 编辑器由平台统一释放。

`JsonDiffPanel` 负责布局、控件绑定和状态展示；`JsonDiffController` 负责输入监听、格式化调度、交换与清空、撤销及光标恢复。控制器通过回调交付校验结果，操作文案由 UI 层解析后传入。

界面文案统一放入 `MyBundle.properties`，通过 `MyBundle` 获取；格式化、交换、清空和状态图标使用 IDE 自带的 `AllIcons`，插件列表、工具窗口及菜单入口使用 `META-INF` 中的品牌图标。测试覆盖严格格式校验、重复键、递归排序、数组顺序、数字精度、中文、嵌套深度及格式化后的光标映射。需要 IDE 集成测试时，再添加 IntelliJ Platform 测试框架依赖。
