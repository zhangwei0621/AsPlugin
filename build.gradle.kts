import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File
import org.gradle.api.artifacts.Configuration
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Kotlin 负责源码编译，IntelliJ Platform 负责 IDE 依赖与插件打包，Shadow 负责隔离 Gson。
plugins {
    alias(libs.plugins.kotlinJvm)
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.shadow)
}

// Provider 按需读取配置，运行和校验共用本机 AS 路径，兼容 Gradle 配置缓存。
val androidStudioPath = providers.gradleProperty("verificationLocalPath")

// 原始 Gson 只放入独立配置，由 Shadow 处理后再提供给源码和最终安装包。
val jsonLibrary: Configuration by configurations.creating
val relocatedJsonJar = tasks.register<ShadowJar>("relocatedJsonJar") {
    description = "生成使用独立包名的 Gson 依赖。"
}

dependencies {
    add(jsonLibrary.name, libs.gson)
    // 引用任务产物让 Gradle 自动安排执行顺序：先生成隔离 JAR，再编译依赖它的源码。
    implementation(files(relocatedJsonJar))
    testImplementation(libs.junit)

    intellijPlatform {
        // 用旧版 IDEA 作为编译基线，减少意外调用新版专属 API 的风险。
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        pluginVerifier()
    }
}

// Java 与 Kotlin 都输出 Java 17 字节码，满足旧版目标 IDE 的运行环境。
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Toolchain 指定编译 JDK，与启动 Gradle 所使用的 JVM 可以不同。
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // 限制可用标准库 API，适配旧版 IDE 自带的 Kotlin 运行库；语言语法单独指定。
        apiVersion = KotlinVersion.KOTLIN_1_7
        languageVersion = KotlinVersion.KOTLIN_1_9
    }
}

intellijPlatform {
    // 插件没有 Settings 配置页，不需要额外启动 IDE 生成设置搜索索引。
    buildSearchableOptions = false
    pluginConfiguration {
        // 注入插件身份与兼容元数据；plugin.xml 仅负责资源、依赖和入口注册。
        id = providers.gradleProperty("pluginId")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("version")
        // HTML 正文独立维护，构建时由平台插件写入描述，不在 XML 中重复保存。
        description = providers.fileContents(layout.projectDirectory.file("docs/plugin-description.html")).asText
        changeNotes = providers.fileContents(layout.projectDirectory.file("docs/plugin-change-notes.html")).asText
        vendor {
            name = providers.gradleProperty("pluginVendor")
        }
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // 不设置安装版本上限；是否实际兼容仍由 Plugin Verifier 和实测确认。
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            // 此处是校验用的 AS 发行版本，与插件版本及 IDEA 编译基线版本用途不同。
            create(IntelliJPlatformType.AndroidStudio, "2022.3.1.22")
            local(androidStudioPath)
        }
    }
}

intellijPlatformTesting {
    // 默认 runIde 使用编译基线 IDEA；此任务使用本机 AS 和独立沙箱调试插件。
    runIde.register("runAndroidStudio") {
        localPath = layout.dir(androidStudioPath.map { File(it) })
    }
}

// 源码直接引用迁移后的包名，避免旧版 IDE 自带 Gson 缺少严格校验 API。
relocatedJsonJar.configure {
    configurations = listOf(jsonLibrary)
    relocate("com.google.gson", "com.github.zhangwei0621.asplugin.internal.gson")
    archiveBaseName = "jsondifftool-gson"
    // JAR 版本沿用依赖版本目录，与插件发布版本分别管理。
    archiveVersion = libs.versions.gson
    archiveClassifier = ""
    // 只保留主实现，移除 Maven 元数据、多版本目录和模块描述文件。
    exclude("META-INF/maven/**", "META-INF/versions/**", "module-info.class")
}
