package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Li2025 锚点测试：与模型卡（01j-模型卡-Li2025RMPP坏死）第 7 节 T1–T10 严格对应。
 * 三处一致纪律：模型卡 = Li2025RmppNecrotizing 常量 = 本测试锚点。
 * 概率容差 ±0.005；T6 距 30% 带界 0.67pp <2pp——只做概率断言不做带位断言（卡 §7 贴线纪律）。
 */
class Li2025Test {

    private fun v(fever: Double, wbc: Double, il6: Double, dd: Double) =
        listOf(fever, wbc, il6, dd)

    @Test
    fun `T1 worked example 83点0 高`() {
        val r = Li2025RmppNecrotizing.assess(v(10.0, 17.5, 300.0, 6.0))
        assertEquals(0.83012, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T2 全低 2点2 低`() {
        val r = Li2025RmppNecrotizing.assess(v(5.0, 8.0, 50.0, 1.0))
        assertEquals(0.02163, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T3 全高 99点7 高`() {
        val r = Li2025RmppNecrotizing.assess(v(14.0, 25.0, 500.0, 10.0))
        assertEquals(0.99725, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T4 NP阴性中位 4点9 低`() {
        val r = Li2025RmppNecrotizing.assess(v(7.0, 7.63, 88.2, 1.31))
        assertEquals(0.04933, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T5 NP阳性中位 26点5 中`() {
        val r = Li2025RmppNecrotizing.assess(v(11.0, 9.8, 137.9, 2.0))
        assertEquals(0.26511, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T6 仅发热拉满 29点3 仅概率断言`() {
        // 0.29327 距 30% 高危带界仅 0.67pp——贴线纪律：只断言概率值，不断言带位（卡 §7）
        val r = Li2025RmppNecrotizing.assess(v(14.0, 7.63, 88.2, 1.31))
        assertEquals(0.29327, r.value, 0.005)
    }

    @Test
    fun `T7 域下界全零 0点1 低`() {
        val r = Li2025RmppNecrotizing.assess(v(0.0, 0.0, 0.0, 0.0))
        assertEquals(0.00102, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T8 域外输入被拒绝`() {
        for (bad in listOf(
            v(15.0, 7.63, 88.2, 1.31),     // 发热 15 > 14
            v(14.0, 36.0, 88.2, 1.31),     // WBC 36 > 35
            v(14.0, 7.63, 1601.0, 1.31),   // IL-6 1601 > 1600
            v(14.0, 7.63, 88.2, 21.0),     // D-dimer 21 > 20
        )) {
            try {
                Li2025RmppNecrotizing.assess(bad)
                throw AssertionError("应抛 IllegalArgumentException：$bad")
            } catch (e: IllegalArgumentException) {
                // 预期：中文错误消息
            }
        }
    }

    @Test
    fun `T9 负数输入被拒绝`() {
        for (bad in listOf(
            v(-1.0, 8.0, 50.0, 1.0),
            v(5.0, -0.1, 50.0, 1.0),
            v(5.0, 8.0, -5.0, 1.0),
            v(5.0, 8.0, 50.0, -0.5),
        )) {
            try {
                Li2025RmppNecrotizing.assess(bad)
                throw AssertionError("应抛 IllegalArgumentException：$bad")
            } catch (e: IllegalArgumentException) {
                // 预期
            }
        }
    }

    @Test
    fun `T10 带位常量断言`() {
        assertEquals(0.08, Li2025RmppNecrotizing.BAND_LOW_UPPER, 0.0)
        assertEquals(0.30, Li2025RmppNecrotizing.BAND_HIGH_LOWER, 0.0)
    }
}
