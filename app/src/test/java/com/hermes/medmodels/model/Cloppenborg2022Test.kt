package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cloppenborg 2022 认知预后锚点测试：与模型卡（01p-模型卡-Cloppenborg2022认知预后）§5 T1–T12 对应。
 * 三处一致纪律：EptIqTables（400/200/160 行逐值）= 卡 §3 = 本测试期望。
 * T1/T5/T8 断原始浮点；其余断一位小数显示值。
 */
class Cloppenborg2022Test {

    // ── IQ85（输入：preIQ, surgery, invasive）──
    @Test
    fun `T1 IQ85 域下界 0点004096121`() {
        val r = Cloppenborg2022Iq85.assess(listOf(41.0, 0.0, 0.0))
        assertEquals(0.004096121, r.value, 1e-9)
        assertEquals("0.4%", r.displayValue)
    }

    @Test
    fun `T2 IQ85 100 focal no 98点0`() {
        val r = Cloppenborg2022Iq85.assess(listOf(100.0, 0.0, 0.0))
        assertEquals("98.0%", r.displayValue)
    }

    @Test
    fun `T3 IQ85 140 multilobar yes 99点9`() {
        val r = Cloppenborg2022Iq85.assess(listOf(140.0, 1.0, 1.0))
        assertEquals("99.9%", r.displayValue)
    }

    @Test
    fun `T4 IQ85 80 multilobar no 25点1`() {
        val r = Cloppenborg2022Iq85.assess(listOf(80.0, 1.0, 0.0))
        assertEquals("25.1%", r.displayValue)
    }

    // ── IQ70（输入：preIQ, surgery）──
    @Test
    fun `T5 IQ70 41 focal 0点0346909`() {
        val r = Cloppenborg2022Iq70.assess(listOf(41.0, 0.0))
        assertEquals(0.0346909, r.value, 1e-9)
        assertEquals("3.5%", r.displayValue)
    }

    @Test
    fun `T6 IQ70 100 focal 99点9`() {
        val r = Cloppenborg2022Iq70.assess(listOf(100.0, 0.0))
        assertEquals("99.9%", r.displayValue)
    }

    @Test
    fun `T7 IQ70 60 multilobar 13点0`() {
        val r = Cloppenborg2022Iq70.assess(listOf(60.0, 1.0))
        assertEquals("13.0%", r.displayValue)
    }

    // ── DQ50（输入：preDQ, mcd）──
    @Test
    fun `T8 DQ50 1 no 0点011114224`() {
        val r = Cloppenborg2022Dq50.assess(listOf(1.0, 0.0))
        assertEquals(0.011114224, r.value, 1e-9)
        assertEquals("1.1%", r.displayValue)
    }

    @Test
    fun `T9 DQ50 40 no 50点5`() {
        val r = Cloppenborg2022Dq50.assess(listOf(40.0, 0.0))
        assertEquals("50.5%", r.displayValue)
    }

    @Test
    fun `T10 DQ50 80 no 99点0`() {
        val r = Cloppenborg2022Dq50.assess(listOf(80.0, 0.0))
        assertEquals("99.0%", r.displayValue)
    }

    @Test
    fun `T11 DQ50 40 yes 20点1`() {
        val r = Cloppenborg2022Dq50.assess(listOf(40.0, 1.0))
        assertEquals("20.1%", r.displayValue)
    }

    @Test
    fun `T12 钳位行为`() {
        // IQ 30→41 / 150→140
        assertEquals(Cloppenborg2022Iq85.assess(listOf(41.0, 0.0, 0.0)).displayValue,
                     Cloppenborg2022Iq85.assess(listOf(30.0, 0.0, 0.0)).displayValue)
        assertEquals(Cloppenborg2022Iq85.assess(listOf(140.0, 1.0, 1.0)).displayValue,
                     Cloppenborg2022Iq85.assess(listOf(150.0, 1.0, 1.0)).displayValue)
        // DQ 0→1 / 90→80
        assertEquals(Cloppenborg2022Dq50.assess(listOf(1.0, 0.0)).displayValue,
                     Cloppenborg2022Dq50.assess(listOf(0.0, 0.0)).displayValue)
        assertEquals(Cloppenborg2022Dq50.assess(listOf(80.0, 0.0)).displayValue,
                     Cloppenborg2022Dq50.assess(listOf(90.0, 0.0)).displayValue)
    }

    @Test
    fun `行数断言 400 200 160`() {
        assertEquals(400, EptIqTables.IQ85.size)
        assertEquals(200, EptIqTables.IQ70.size)
        assertEquals(160, EptIqTables.DQ50.size)
    }
}
