import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

// 工程名称影响 Gradle 产物名称；插件在 IDE 中的显示名称由 pluginName 配置决定。
rootProject.name = "AsPlugin"

// Foojay 在需要时解析 JDK Toolchain；平台 settings 插件提供 IDE 依赖仓库支持。
// 此阶段 libs 访问器尚未生成，settings 插件在这里声明版本；项目插件使用版本目录。
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.10.5"
}

// 仓库在 settings 中集中声明；gradle/libs.versions.toml 会被 Gradle 自动识别。
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            // 增加 IntelliJ Platform、IDE 下载和相关工具使用的默认仓库。
            defaultRepositories()
        }
    }
}
