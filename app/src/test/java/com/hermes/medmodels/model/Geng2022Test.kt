package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Geng2022 锚点测试：与模型卡（01d-模型卡-Geng2022）第 7 节 T1–T11 严格对应。
 * 三处一致纪律：模型卡公式 = Geng2022DreEarly 常量 = 本测试锚点。
 * T1 为原文 Fig 3 worked example（84%），容差 ±0.02；其余 ±0.005。
 */
class Geng2022Test {

    // 输入顺序：onset/SE/focal/gt20/dee/neuro/病因β
    private fun v(onset: Double, se: Double, focal: Double, gt20: Double,
                  dee: Double, neuro: Double, etio: Double) =
        listOf(onset, se, focal, gt20, dee, neuro, etio)

    // 病因 β 快捷
    private val G = 0.0; private val S = 2.498; private val M = 2.519
    private val I = 0.542; private val INF = 1.366; private val U = -0.942

    @Test
    fun `T1 worked example 84点0 高危`() {
        // 起病<1 + SE + 结构病因（原文 Fig 3：总分 184 → 84%）
        val r = Geng2022DreEarly.assess(v(1.0, 1.0, 0.0, 0.0, 0.0, 0.0, S))
        assertEquals(0.840, r.value, 0.02)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T2 全阴基线 2点2 低危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, G))
        assertEquals(0.022, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T3 全阳结构 99点9 高危`() {
        val r = Geng2022DreEarly.assess(v(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, S))
        assertEquals(0.999, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T4 全阳未知 97点0 高危`() {
        // 保护性病因（β=−0.942）下全阳仍高危
        val r = Geng2022DreEarly.assess(v(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, U))
        assertEquals(0.970, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T5 中等组合 43点9 中危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, G))
        assertEquals(0.439, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T6 仅保护性病因 0点9 低危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, U))
        assertEquals(0.009, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T7 免疫不显著 3点8 低危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, I))
        assertEquals(0.038, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T8 仅神经异常 11点6 低危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, G))
        assertEquals(0.116, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T9 起病小于1加局灶 20点7 中危`() {
        val r = Geng2022DreEarly.assess(v(1.0, 0.0, 1.0, 0.0, 0.0, 0.0, G))
        assertEquals(0.207, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T10 代谢加DEE加神经 85点6 高危`() {
        val r = Geng2022DreEarly.assess(v(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, M))
        assertEquals(0.856, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T11 病因未选与非法二元被拒绝`() {
        // 病因未选 → null
        try {
            Geng2022DreEarly.assess(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null as Double?) as List<Double>)
            throw AssertionError("应抛 IllegalArgumentException（病因未选）")
        } catch (e: IllegalArgumentException) { /* 预期 */ }
        // 二元传 2.0 → 非法
        try {
            Geng2022DreEarly.assess(v(2.0, 0.0, 0.0, 0.0, 0.0, 0.0, G))
            throw AssertionError("应抛 IllegalArgumentException（二元取值非法）")
        } catch (e: IllegalArgumentException) { /* 预期 */ }
    }

    @Test
    fun `风险带常量与模型卡一致`() {
        assertEquals(0.15, Geng2022DreEarly.BAND_LOW_UPPER, 1e-9)
        assertEquals(0.50, Geng2022DreEarly.BAND_MID_UPPER, 1e-9)
    }
}
