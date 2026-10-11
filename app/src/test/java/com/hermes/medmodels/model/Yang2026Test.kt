package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Yang2026 锚点测试：与模型卡（01a-模型卡-Yang2026）第 3/6 节锚点严格对应。
 * 三处一致纪律：模型卡公式 = Yang2026Rmpp 常量 = 本测试锚点。
 */
class Yang2026Test {

    private fun vararg(f: Double, t: Double, l: Double, sp: Double, c: Double, h: Double) =
        listOf(f, t, l, sp, c, h)

    @Test
    fun `病例A 锚点 原文worked example 复现98点6`() {
        // 热程4天/峰值40℃/LDH 415/痰栓+/实变+/低氧+ → 原文 98.6%
        val r = Yang2026Rmpp.assess(vararg(f = 4.0, t = 40.0, l = 415.0, sp = 1.0, c = 1.0, h = 1.0))
        assertEquals(0.986, r.value, 0.01)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `病例B 低危表型 个位数概率`() {
        val r = Yang2026Rmpp.assess(vararg(f = 2.0, t = 38.5, l = 250.0, sp = 0.0, c = 0.0, h = 0.0))
        assertTrue("低危应 <10%，实得 ${r.value}", r.value in 0.01..0.10)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `病例C 中危表型 双截断下档位一致`() {
        val r = Yang2026Rmpp.assess(vararg(f = 5.0, t = 39.5, l = 350.0, sp = 0.0, c = 1.0, h = 0.0))
        assertTrue("中危应在 25–60%，实得 ${r.value}", r.value in 0.25..0.60)
        assertEquals("高风险", r.bandLabel) // ≥0.222
    }

    @Test
    fun `病例D 左边界 全阴低危`() {
        val r = Yang2026Rmpp.assess(vararg(f = 0.0, t = 36.0, l = 0.0, sp = 0.0, c = 0.0, h = 0.0))
        assertTrue(r.value < 0.001)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `病例E 右边界 全阳逼近饱和`() {
        val r = Yang2026Rmpp.assess(vararg(f = 14.0, t = 42.0, l = 3000.0, sp = 1.0, c = 1.0, h = 1.0))
        assertTrue("高危应 >0.999，实得 ${r.value}", r.value > 0.999)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `病例F 非法输入被拒绝`() {
        val assertions = listOf(
            listOf(20.0, 39.0, 300.0, 0.0, 0.0, 0.0),   // 热程超范围
            listOf(4.0, 43.0, 300.0, 0.0, 0.0, 0.0),    // 体温超范围
            listOf(4.0, 39.0, 9999.0, 0.0, 0.0, 0.0),   // LDH 超范围
        )
        for (v in assertions) {
            try {
                Yang2026Rmpp.assess(v)
                throw AssertionError("应抛 IllegalArgumentException：$v")
            } catch (e: IllegalArgumentException) {
                // 预期
            }
        }
    }

    @Test
    fun `截断常量与模型卡一致`() {
        assertEquals(0.222, Yang2026Rmpp.CUTOFF_TRAIN, 1e-9)
        assertEquals(0.247, Yang2026Rmpp.CUTOFF_VALID, 1e-9)
    }
}
