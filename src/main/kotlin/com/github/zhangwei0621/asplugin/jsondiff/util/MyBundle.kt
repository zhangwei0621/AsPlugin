package com.github.zhangwei0621.asplugin.jsondiff.util

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

// 对应 src/main/resources/messages/MyBundle.properties，与 Kotlin 类所在包无关。
@NonNls
private const val BUNDLE = "messages.MyBundle"

/**
 * 界面文案的统一访问入口，通过 DynamicBundle 获取资源并替换 {0} 等参数占位符。
 * PropertyKey 让 IDE 能检查资源键是否存在；NonNls 表示资源路径本身无需翻译。
 */
object MyBundle : DynamicBundle(BUNDLE) {

    /** 支持 MyBundle["资源键", 参数] 写法，返回可直接展示的本地化字符串。 */
    operator fun get(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)
}
