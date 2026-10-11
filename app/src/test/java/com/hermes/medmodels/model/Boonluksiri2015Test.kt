package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Boonluksiri2015 锚点测试：与模型卡（01c-模型卡-Boonluksiri2015）第 7 节 T1–T9 严格对应。
 * 三处一致纪律：模型卡公式 = Boonluksiri2015Dre 常量 = 本测试锚点。
 */
class Boonluksiri2015Test {

    // 便捷输入：分值直接传入（年龄/缺陷/EEG）
    private fun v(a: Double, d: Double, e: Double) = listOf(a, d, e)

    @Test
    fun `T1 全基线 0分低危`() {
        val r = Boonluksiri2015Dre.assess(v(0.0, 0.0, 0.0))
        assertEquals(0.0, r.value, 1e-9)
        assertEquals("低风险", r.bandLabel)
        assertEquals("0 分", r.displayValue)
    }

    @Test
    fun `T2 满分 19 高危`() {
        val r = Boonluksiri2015Dre.assess(v(2.5, 13.0, 3.5))
        assertEquals(19.0, r.value, 1e-9)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T3 切点上侧 12点5 高危`() {
        val r = Boonluksiri2015Dre.assess(v(0.0, 9.0, 3.5))
        assertEquals(12.5, r.value, 1e-9)
        assertEquals("高风险", r.bandLabel)   // >12
    }

    @Test
    fun `T4 切点闭区间上缘 12 中危`() {
        val r = Boonluksiri2015Dre.assess(v(2.5, 6.0, 3.5))
        assertEquals(12.0, r.value, 1e-9)
        assertEquals("中风险", r.bandLabel)   // 6–12 闭区间
    }

    @Test
    fun `T5 切点闭区间下缘 6 中危`() {
        val r = Boonluksiri2015Dre.assess(v(0.0, 6.0, 0.0))
        assertEquals(6.0, r.value, 1e-9)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T6 全面加年长 15点5 高危`() {
        val r = Boonluksiri2015Dre.assess(v(1.5, 13.0, 1.0))
        assertEquals(15.5, r.value, 1e-9)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T7 单调性 含全面缺陷必高危`() {
        // 全面缺陷 13 分单项即 >12：与任意年龄/EEG 组合均高危
        for (a in listOf(0.0, 1.5, 2.5)) {
            for (e in listOf(0.0, 1.0, 3.5)) {
                val r = Boonluksiri2015Dre.assess(v(a, 13.0, e))
                assertTrue("a=$a e=$e 应 ≥13", r.value >= 13.0)
                assertEquals("高风险", r.bandLabel)
            }
        }
    }

    @Test
    fun `T8 未选择被拒绝`() {
        try {
            Boonluksiri2015Dre.assess(listOf(0.0, null as Double?, 0.0) as List<Double>)
            throw AssertionError("应抛 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // 预期（「既往神经缺陷」未选择）
        }
    }

    @Test
    fun `T9 验后换算 先验30 LR12点5 约84`() {
        val post = Boonluksiri2015Dre.posteriorProbability(12.5, 0.30)
        assertEquals(0.843, post, 0.005)
        // 整数显示断言（D2 拍板：验后概率取整数）
        val shown = Math.round(post * 100).toInt()
        assertEquals(84, shown)
    }
}
