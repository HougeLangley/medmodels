package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lamberink 2018 TTS 锚点测试：与模型卡（01o-模型卡-Lamberink2018TTS术后撤药）§5 T1–T5 严格对应。
 * 输入序 (ageWithdrawal, ttr, mriIdx, compIdx, eegIdx, sfreq, naed)。
 *
 * ⚠️ 卡勘误留痕（已报备）：
 * - 卡 §8 表长断言「142/142/100」——JS 真源 RISK2_LONG 实为 99 行（0..49 半步），以 pin commit JS 为准
 * - 卡 §3.1/§8「39 档」——SFREQ_VALS 实为 40 档
 * - 卡 §5 T5「1.1→1.0 档」——浮点距离 |1.2−1.1|=0.09999… < |1.1−1.0|=0.10000…1，JS findClosestSfreq
 *   与本实现一致选中 1.2 档（卡锚按 JS 实际行为修正为 1.1→1.2）
 */
class Lamberink2018TtsTest {

    private fun v(age: Double, ttr: Double, mri: Double, comp: Double, eeg: Double, sfreq: Double, naed: Double) =
        listOf(age, ttr, mri, comp, eeg, sfreq, naed)

    @Test
    fun `T1 域下界 2y饱和下 6y饱和上`() {
        val r = Lamberink2018Tts.assess(v(0.0, 30.0, 0.0, 0.0, 0.0, 5.0, 0.0))
        assertEquals("<2%", r.displayValue)
        assertEquals("<5%", r.extraOutputs[0].second)
        assertEquals(">98%", r.extraOutputs[1].second)
    }

    @Test
    fun `T2 域上界 2y饱和上 6y整数56`() {
        val r = Lamberink2018Tts.assess(v(18.0, 3.0, 1.0, 1.0, 2.0, 0.5, 4.0))
        assertEquals(">54%", r.displayValue)
        assertEquals(">85%", r.extraOutputs[0].second)
        assertEquals("56%", r.extraOutputs[1].second)
        assertTrue(r.bandExplanation.contains("61"))
    }

    @Test
    fun `T3 中间带整数值 9 22 90`() {
        val r = Lamberink2018Tts.assess(v(10.0, 12.0, 0.0, 1.0, 0.0, 2.0, 2.0))
        assertEquals("9%", r.displayValue)
        assertEquals("22%", r.extraOutputs[0].second)
        assertEquals("90%", r.extraOutputs[1].second)
    }

    @Test
    fun `T4 钳位行为`() {
        val clamped = Lamberink2018Tts.assess(v(30.0, 100.0, 0.0, 0.0, 0.0, 5.0, 8.0))
        val direct = Lamberink2018Tts.assess(v(26.0, 85.0, 0.0, 0.0, 0.0, 5.0, 5.0))
        assertEquals(direct.displayValue, clamped.displayValue)
        assertEquals(direct.extraOutputs, clamped.extraOutputs)
    }

    @Test
    fun `T5 发作频率最近邻 1点3到1点4 1点1到1点2`() {
        assertEquals(1.4, EptTables.TTS_SFREQ_VALS[Lamberink2018Tts.findClosestSfreq(1.3)], 1e-9)
        // 卡原锚「1.1→1.0」经 JS 复核为浮点平局败北（实际 1.2 更近），按 JS 真源断言：
        assertEquals(1.2, EptTables.TTS_SFREQ_VALS[Lamberink2018Tts.findClosestSfreq(1.1)], 1e-9)
    }

    @Test
    fun `表长断言 142 142 99 与 SFREQ 40 档`() {
        assertEquals(142, EptTables.TTS_RISK1_2Y.size)
        assertEquals(142, EptTables.TTS_RISK1_5Y.size)
        assertEquals(99, EptTables.TTS_RISK2_LONG.size)   // 卡写 100，JS 真源 99（报备）
        assertEquals(40, EptTables.TTS_SFREQ_VALS.size)   // 卡写 39，JS 真源 40（报备）
        assertEquals(40, EptTables.TTS_SFREQ_PTS2.size)
    }

    @Test
    fun `M3 cure 未移植声明`() {
        // 卡 §1 硬约束：cure 结局不实现——EptTables 不得含 RISK3/CURE 常量
        val fields = EptTables::class.java.declaredFields.map { it.name }
        assertTrue(fields.none { it.contains("RISK3") || it.contains("CURE") || it.contains("AETIOL") })
    }
}
