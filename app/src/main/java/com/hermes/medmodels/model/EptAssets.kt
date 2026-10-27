package com.hermes.medmodels.model

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * EPT 大表资产加载器（JME 撤药 20400×2 int 二进制）。
 *
 * 双通道：
 * - Android 运行时：MainActivity.onCreate 调 [init] 注入 applicationContext，从 assets 读
 * - JVM 单元测试：无 Context，回退读模块相对路径文件（工作目录=app/ 模块根）
 *
 * 资产由转换脚本从 jme-withdrawal-calc.js 逐值生成（int32 LE），勿手改。
 */
internal object EptAssets {

    @Volatile
    private var appContext: Context? = null

    private val cache = HashMap<String, IntArray>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun loadInts(assetName: String, expectedCount: Int): IntArray {
        cache[assetName]?.let { return it }
        val bytes = appContext?.let { ctx ->
            ctx.assets.open(assetName).use { it.readBytes() }
        } ?: File("src/main/assets/$assetName").readBytes()  // 单元测试回退
        require(bytes.size == expectedCount * 4) {
            "EPT 资产 $assetName 大小异常：${bytes.size} 字节，期望 ${expectedCount * 4}"
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val arr = IntArray(expectedCount) { buf.int }
        cache[assetName] = arr
        return arr
    }
}
