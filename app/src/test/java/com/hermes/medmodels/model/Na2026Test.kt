package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Na2026 锚点测试：与模型卡（01i-模型卡-Na2026PB复发）第 7 节 T1–T13 严格对应。
 * 三处一致纪律：模型卡 = Na2026PbRecurrence 常量 = 本测试锚点。
 * 概率容差 ±0.005；全部锚点距带界 >2pp，无贴线项。
 */
class Na2026Test {

    private fun v(alb: Double, fever: Double, atel: Double, casts: Double) =
        listOf(alb, fever, atel, casts)

    @Test
    fun `T1 ALB40 全阴 9点0 低`() {
        val r = Na2026PbRecurrence.assess(v(40.0, 0.0, 0.0, 0.0))
        assertEquals(0.09017, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T2 ALB42点61 全阴 5点7 低`() {
        val r = Na2026PbRecurrence.assess(v(42.61, 0.0, 0.0, 0.0))
        assertEquals(0.05727, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T3 ALB30 全阳 98点8 高`() {
        val r = Na2026PbRecurrence.assess(v(30.0, 1.0, 1.0, 1.0))
        assertEquals(0.98834, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T4 ALB20 域极端 99点8 高`() {
        val r = Na2026PbRecurrence.assess(v(20.0, 1.0, 1.0, 1.0))
        assertEquals(0.99819, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T5 ALB40 仅发热 51点2 高`() {
        val r = Na2026PbRecurrence.assess(v(40.0, 1.0, 0.0, 0.0))
        assertEquals(0.51242, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T6 ALB37点09 复发组均值 95点7 高`() {
        val r = Na2026PbRecurrence.assess(v(37.09, 1.0, 1.0, 1.0))
        assertEquals(0.95730, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T7 ALB41点42 切点全阴 7点1 低`() {
        val r = Na2026PbRecurrence.assess(v(41.42, 0.0, 0.0, 0.0))
        assertEquals(0.07057, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T8 ALB55 轴端全阴 0点6 低`() {
        val r = Na2026PbRecurrence.assess(v(55.0, 0.0, 0.0, 0.0))
        assertEquals(0.00591, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T9 ALB35 阳三缺一 91点1 高`() {
        val r = Na2026PbRecurrence.assess(v(35.0, 1.0, 1.0, 0.0))
        assertEquals(0.91126, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T10 ALB45 全阴 3点7 低`() {
        val r = Na2026PbRecurrence.assess(v(45.0, 0.0, 0.0, 0.0))
        assertEquals(0.03735, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T11 域外输入被拒绝`() {
        for (bad in listOf(
            v(19.0, 0.0, 0.0, 0.0),
            v(56.0, 0.0, 0.0, 0.0),
        )) {
            try {
                Na2026PbRecurrence.assess(bad)
                throw AssertionError("应抛 IllegalArgumentException：$bad")
            } catch (e: IllegalArgumentException) {
                // 预期：中文错误消息
            }
        }
    }

    @Test
    fun `T12 BINARY 传非法值被拒绝`() {
        try {
            Na2026PbRecurrence.assess(v(40.0, 2.0, 0.0, 0.0))
            throw AssertionError("应抛 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // 预期
        }
    }

    @Test
    fun `T13 带位常量断言`() {
        assertEquals(0.15, Na2026PbRecurrence.BAND_LOW_UPPER, 0.0)
        assertEquals(0.45, Na2026PbRecurrence.BAND_HIGH_LOWER, 0.0)
    }
}
