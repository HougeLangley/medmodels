package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lamberink 2017 AED 撤药锚点测试：与模型卡（01n-模型卡-Lamberink2017AED撤药）§5 T1–T6 严格对应。
 * 三处一致纪律：卡 §3 分值表/风险表（pin commit JS 逐值）= EptTables = 本测试期望。
 * 输入序 (ttr, dur, age, histfeb, nseizIdx, benign, delay, eegIdx, naed, sex, famhist, focal)。
 * 饱和串原样断言（<10/>90/>99/<40）；总分越界报错不钳位。
 */
class Lamberink2017AedTest {

    private fun v(ttr: Double, dur: Double, age: Double, hf: Double, ns: Double, benign: Double,
                  delay: Double, eeg: Double, naed: Double, sex: Double, fam: Double, focal: Double) =
        listOf(ttr, dur, age, hf, ns, benign, delay, eeg, naed, sex, fam, focal)

    @Test
    fun `T1 worked example 1 totalRecur27点5 2y28 5y36`() {
        val r = Lamberink2017Aed.assess(v(2.0, 1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals("28%", r.displayValue)
        assertEquals("2 年复发风险", r.bandLabel)
        assertEquals("36%", r.extraOutputs[0].second)
        assertTrue(r.bandExplanation.contains("27.5"))  // 总分随解释行展示
        // JS 复算 10y=98（卡未锚，不断言）
    }

    @Test
    fun `T2 worked example 2 totalLong19点5 10y97`() {
        val r = Lamberink2017Aed.assess(v(2.0, 1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0))
        assertEquals("97%", r.extraOutputs[1].second)
        assertEquals("10 年无发作概率", r.extraOutputs[1].first)
    }

    @Test
    fun `T3 域下界 饱和串下`() {
        val r = Lamberink2017Aed.assess(v(24.0, 0.0, 3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals("<10%", r.displayValue)
        assertEquals("<10%", r.extraOutputs[0].second)
        assertEquals(">99%", r.extraOutputs[1].second)
    }

    @Test
    fun `T4 域上界 饱和串上`() {
        val r = Lamberink2017Aed.assess(v(0.0, 40.0, 80.0, 1.0, 1.0, 0.0, 1.0, 2.0, 9.0, 1.0, 1.0, 1.0))
        assertEquals(">90%", r.displayValue)
        assertEquals(">90%", r.extraOutputs[0].second)
        assertEquals("<40%", r.extraOutputs[1].second)
        assertTrue(r.bandExplanation.contains("59.5"))
    }

    @Test
    fun `T5 中间带整数值 2y58 5y72 10y92`() {
        val r = Lamberink2017Aed.assess(v(5.0, 3.0, 15.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0))
        assertEquals("58%", r.displayValue)
        assertEquals("72%", r.extraOutputs[0].second)
        assertEquals("92%", r.extraOutputs[1].second)
    }

    @Test
    fun `T6 钳位行为 30到24 50到40 90到80 12到9`() {
        // 钳位断言：越界输入 ≡ 显式钳位值输入（任务定义：30→24、50→40、90→80、12→9）
        // 注：卡 §5 期望列写「同 T4」系笔误（钳位后 ttr=24≠T4 的 ttr=0），已报备；本断言为行为等价性
        val clamped = Lamberink2017Aed.assess(v(30.0, 50.0, 90.0, 1.0, 1.0, 0.0, 1.0, 2.0, 12.0, 1.0, 1.0, 1.0))
        val explicit = Lamberink2017Aed.assess(v(24.0, 40.0, 80.0, 1.0, 1.0, 0.0, 1.0, 2.0, 9.0, 1.0, 1.0, 1.0))
        assertEquals(explicit.displayValue, clamped.displayValue)
        assertEquals(explicit.extraOutputs, clamped.extraOutputs)
    }

    @Test
    fun `表长断言 120 120 96`() {
        assertEquals(120, EptTables.AED_RISK2Y.size)
        assertEquals(120, EptTables.AED_RISK5Y.size)
        assertEquals(96, EptTables.AED_RISK_LONG.size)
        assertEquals(25, EptTables.AED_TTR_PTS.size)
        assertEquals(81, EptTables.AED_AGE_PTS.size)
    }

    @Test
    fun `饱和串原样显示纪律`() {
        // 输出纪律：表值原样（整数% 或饱和串+%），禁连续概率——断言 displayValue 无小数点
        val r = Lamberink2017Aed.assess(v(2.0, 1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertTrue(Regex("^[<>]?\\d+%$").matches(r.displayValue))
    }
}
