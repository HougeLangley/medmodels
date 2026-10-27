package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chen2026 锚点测试：与模型卡（01l-模型卡-Chen2026SMPP）第 5 节 T1–T12 严格对应。
 * 三处一致纪律：模型卡 §3.1 常量 = Chen2026Smpp 实现 = 本测试锚点。
 * 概率容差 ±0.005（T3 worked example 为 ±0.02：原文仅声明「>95%」）。
 * 无分带硬约束：带位断言=切点侧（相对 0.553 参照线），非风险档位。
 */
class Chen2026Test {

    private fun v(fever: Double, sob: Double, macro: Double, il6: Double, cons: Double) =
        listOf(fever, sob, macro, il6, cons)

    @Test
    fun `T1 域下界 1点3 低于切点`() {
        val r = Chen2026Smpp.assess(v(1.0, 0.0, 0.0, 5.0, 0.0))
        assertEquals(0.01269, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T2 热5天全阴 17点6 低于切点`() {
        val r = Chen2026Smpp.assess(v(5.0, 0.0, 0.0, 5.0, 0.0))
        assertEquals(0.17624, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T3 worked example 98点8 仅概率断言`() {
        // 原文 worked example 声明「>95%」，直算 98.753%——按卡 §5 用 ±0.02 锁（双通道自洽锚）
        val r = Chen2026Smpp.assess(v(4.0, 1.0, 1.0, 16.7, 0.0))
        assertEquals(0.98753, r.value, 0.02)
    }

    @Test
    fun `T4 WE加实变 99点96 高于切点`() {
        val r = Chen2026Smpp.assess(v(4.0, 1.0, 1.0, 16.7, 1.0))
        assertEquals(0.99960, r.value, 0.005)
        assertEquals("高于切点", r.bandLabel)
    }

    @Test
    fun `T5 21点7 低于切点`() {
        val r = Chen2026Smpp.assess(v(3.0, 0.0, 1.0, 12.0, 0.0))
        assertEquals(0.21653, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T6 53点0 低于切点 贴线边界`() {
        // 贴线纪律（卡 §5）：0.52996 距切点 0.553 仅 2.3pp——>2pp 允许切点侧断言；
        // 若实现舍入导致切点侧翻转，须报备并回卡复核，不得静默改断言。
        val r = Chen2026Smpp.assess(v(5.0, 0.0, 1.0, 12.0, 0.0))
        assertEquals(0.52996, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T7 78点1 高于切点`() {
        val r = Chen2026Smpp.assess(v(3.0, 0.0, 0.0, 20.0, 1.0))
        assertEquals(0.78091, r.value, 0.005)
        assertEquals("高于切点", r.bandLabel)
    }

    @Test
    fun `T8 2点5 低于切点`() {
        val r = Chen2026Smpp.assess(v(2.0, 0.0, 0.0, 5.0, 0.0))
        assertEquals(0.02531, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T9 热5天加气促 96点0 高于切点`() {
        val r = Chen2026Smpp.assess(v(5.0, 1.0, 0.0, 5.0, 0.0))
        assertEquals(0.95981, r.value, 0.005)
        assertEquals("高于切点", r.bandLabel)
    }

    @Test
    fun `T10 气促单项 85点4 高于切点`() {
        val r = Chen2026Smpp.assess(v(3.0, 1.0, 0.0, 5.0, 0.0))
        assertEquals(0.85408, r.value, 0.005)
        assertEquals("高于切点", r.bandLabel)
    }

    @Test
    fun `T11 WE去气促 41点5 低于切点`() {
        val r = Chen2026Smpp.assess(v(4.0, 0.0, 1.0, 16.7, 0.0))
        assertEquals(0.41501, r.value, 0.005)
        assertEquals("低于切点", r.bandLabel)
    }

    @Test
    fun `T12 去气促加实变 95点7 高于切点`() {
        val r = Chen2026Smpp.assess(v(4.0, 0.0, 1.0, 16.7, 1.0))
        assertEquals(0.95733, r.value, 0.005)
        assertEquals("高于切点", r.bandLabel)
    }

    @Test
    fun `切点常量与域外输入`() {
        assertEquals(0.553, Chen2026Smpp.OPERATING_CUTOFF, 0.0)
        for (bad in listOf(
            v(0.5, 0.0, 0.0, 5.0, 0.0),    // 发热 0.5 < 1 域外
            v(5.5, 0.0, 0.0, 5.0, 0.0),    // 发热 5.5 > 5 域外
            v(3.0, 0.0, 0.0, 101.0, 0.0),  // IL-6 > 100 域外
            v(3.0, 2.0, 0.0, 5.0, 0.0),    // 开关非法值
        )) {
            try {
                Chen2026Smpp.assess(bad)
                throw AssertionError("应抛 IllegalArgumentException：$bad")
            } catch (e: IllegalArgumentException) {
                // 预期：中文错误消息
            }
        }
    }
}
