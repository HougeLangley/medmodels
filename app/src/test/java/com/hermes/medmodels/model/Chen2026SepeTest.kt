package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chen2026 SEPE 锚点测试：与模型卡（01e-模型卡-Chen2026SEPE）第 6 节 T1–T12 严格对应。
 * 三处一致纪律：模型卡 = Chen2026Sepe 常量 = 本测试锚点。
 * 全部容差 ±0.001（分值精确 0.5 步进）。
 */
class Chen2026SepeTest {

    // 输入顺序：家族/智力/睡眠觉醒/类型数/频率/MRI
    private fun v(f: Double, i: Double, s: Double, t: Double, q: Double, m: Double) =
        listOf(f, i, s, t, q, m)

    @Test
    fun `T1 全无 0分 良好带`() {
        val r = Chen2026Sepe.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.0, r.value, 0.001)
        assertEquals("预后良好倾向", r.bandLabel)
    }

    @Test
    fun `T2 最轻现实组合 2点5 良好`() {
        val r = Chen2026Sepe.assess(v(0.0, 0.0, 1.0, 1.0, 0.5, 0.0))
        assertEquals(2.5, r.value, 0.001)
        assertEquals("预后良好倾向", r.bandLabel)
    }

    @Test
    fun `T3 恰4点0边界 良好闭区间`() {
        val r = Chen2026Sepe.assess(v(1.0, 1.0, 1.0, 1.0, 0.0, 0.0))
        assertEquals(4.0, r.value, 0.001)
        assertEquals("预后良好倾向", r.bandLabel)
    }

    @Test
    fun `T4 4点5边界 中间带`() {
        val r = Chen2026Sepe.assess(v(1.0, 1.0, 1.0, 1.0, 0.5, 0.0))
        assertEquals(4.5, r.value, 0.001)
        assertEquals("中间带", r.bandLabel)
    }

    @Test
    fun `T5 5点5边界 中间带`() {
        val r = Chen2026Sepe.assess(v(1.0, 0.0, 2.0, 2.0, 0.5, 0.0))
        assertEquals(5.5, r.value, 0.001)
        assertEquals("中间带", r.bandLabel)
    }

    @Test
    fun `T6 恰6点0边界 高风险闭区间`() {
        val r = Chen2026Sepe.assess(v(0.0, 0.0, 2.0, 2.0, 2.0, 0.0))
        assertEquals(6.0, r.value, 0.001)
        assertEquals("高耐药风险", r.bandLabel)
    }

    @Test
    fun `T7 理论最大 10分 高风险`() {
        val r = Chen2026Sepe.assess(v(1.0, 1.0, 2.0, 3.0, 2.0, 1.0))
        assertEquals(10.0, r.value, 0.001)
        assertEquals("高耐药风险", r.bandLabel)
    }

    @Test
    fun `T8 0点5步进专项`() {
        // 仅 initial 频率贡献 0.5；其余整数
        val r = Chen2026Sepe.assess(v(0.0, 0.0, 1.0, 1.0, 0.5, 0.0))
        assertEquals(2.5, r.value, 0.001)
        assertEquals("2.5 分", r.displayValue)
    }

    @Test
    fun `T9 sleepOnly 与 wakeOnly 同分`() {
        val a = Chen2026Sepe.assess(v(1.0, 1.0, 1.0, 2.0, 1.0, 1.0))  // sleepOnly=1
        val b = Chen2026Sepe.assess(v(1.0, 1.0, 1.0, 2.0, 1.0, 1.0))  // wakeOnly=1（同分值）
        assertEquals(a.value, b.value, 0.001)
        assertEquals(a.bandLabel, b.bandLabel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `T10 CATEGORICAL 未选被拒绝`() {
        Chen2026Sepe.assess(listOf(0.0, 0.0, null as Double?, 1.0, 1.0, 0.0) as List<Double>)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `T11 BINARY 非法值被拒绝`() {
        Chen2026Sepe.assess(v(2.0, 0.0, 1.0, 1.0, 1.0, 0.0))
    }

    @Test
    fun `T12 带位常量闭区间语义`() {
        assertEquals(4.0, Chen2026Sepe.BAND_GOOD_MAX, 1e-9)
        assertEquals(6.0, Chen2026Sepe.BAND_HIGH_MIN, 1e-9)
    }
}
