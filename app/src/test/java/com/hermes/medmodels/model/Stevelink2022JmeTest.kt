package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stevelink 2022 JME 锚点测试：与模型卡（01q-模型卡-Stevelink2022JME）§5 T1–T12 严格对应。
 * 三处一致纪律：卡 §3 双表（pin commit JS 逐值）= 资产/EptJmeResistanceTable = 本测试期望。
 * 撤药表为二进制资产（单元测试经 EptAssets 文件回退通道加载，工作目录=app 模块根）。
 */
class Stevelink2022JmeTest {

    // ═══ 撤药复发（输入：asm, sf, age）═══

    @Test
    fun `T1 asm1 sf2 age20 60和73`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(1.0, 2.0, 20.0))
        assertEquals(0.6020, r.value, 1e-9)
        assertEquals("60%", r.displayValue)
        assertEquals("73%", r.extraOutputs[0].second)
    }

    @Test
    fun `T2 asm1 sf2 age25 54和67`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(1.0, 2.0, 25.0))
        assertEquals(0.5415, r.value, 1e-9)
        assertEquals("54%", r.displayValue)
        assertEquals("67%", r.extraOutputs[0].second)
    }

    @Test
    fun `T3 asm1 sf2 age30 48和61`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(1.0, 2.0, 30.0))
        assertEquals(0.4831, r.value, 1e-9)
        assertEquals("48%", r.displayValue)
        assertEquals("61%", r.extraOutputs[0].second)
    }

    @Test
    fun `T4 asm1 sf0 age25 71和83`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(1.0, 0.0, 25.0))
        assertEquals(0.7093, r.value, 1e-9)
        assertEquals("71%", r.displayValue)
        assertEquals("83%", r.extraOutputs[0].second)
    }

    @Test
    fun `T5 asm1 sf10 age25 43和55`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(1.0, 10.0, 25.0))
        assertEquals(0.4302, r.value, 1e-9)
        assertEquals("43%", r.displayValue)
        assertEquals("55%", r.extraOutputs[0].second)
    }

    @Test
    fun `T6 asm2 sf2 age25 68和80`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(2.0, 2.0, 25.0))
        assertEquals(0.6753, r.value, 1e-9)
        assertEquals("68%", r.displayValue)
        assertEquals("80%", r.extraOutputs[0].second)
    }

    @Test
    fun `T7 asm4 sf2 age25 90和97`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(4.0, 2.0, 25.0))
        assertEquals(0.9038, r.value, 1e-9)
        assertEquals("90%", r.displayValue)
        assertEquals("97%", r.extraOutputs[0].second)
    }

    @Test
    fun `T8 域上界 asm4 sf50 age100 8和11`() {
        val r = Stevelink2022JmeWithdrawal.assess(listOf(4.0, 50.0, 100.0))
        assertEquals(0.0760, r.value, 1e-9)
        assertEquals("8%", r.displayValue)
        assertEquals("11%", r.extraOutputs[0].second)
    }

    // ═══ 耐药（输入：ethnicityIdx, histfeb, status, psych, famhist, threetypes, catamenial, eegfocal, cae）═══

    @Test
    fun `T9 worked example Caucasian组合 89点8`() {
        // Caucasian|yes|never|yes|no|yes|yes|yes|no（卡锚：原文 worked example 89 分→90%）
        val r = Stevelink2022JmeResistance.assess(listOf(0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0))
        assertEquals(0.897989222, r.value, 1e-9)
        assertEquals("90%", r.displayValue)
    }

    @Test
    fun `T10 域低端 Other全阴 18点3`() {
        val r = Stevelink2022JmeResistance.assess(listOf(3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.1834, r.value, 5e-4)
        assertEquals("18%", r.displayValue)
    }

    @Test
    fun `T11 域高端 Asian全阳 91点0`() {
        val r = Stevelink2022JmeResistance.assess(listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        assertEquals(0.9099, r.value, 5e-4)
        assertEquals("91%", r.displayValue)
    }

    @Test
    fun `T12 钳位行为`() {
        // age=0→1、sf=99→50、asm=7→4：钳位后与边界值同结果
        assertEquals(
            Stevelink2022JmeWithdrawal.assess(listOf(4.0, 50.0, 100.0)).displayValue,
            Stevelink2022JmeWithdrawal.assess(listOf(7.0, 99.0, 150.0)).displayValue,
        )
        assertEquals(
            Stevelink2022JmeWithdrawal.assess(listOf(1.0, 0.0, 1.0)).displayValue,
            Stevelink2022JmeWithdrawal.assess(listOf(0.0, -5.0, 0.0)).displayValue,
        )
    }

    @Test
    fun `行数断言 20400双表 1024耐药`() {
        val p2 = EptAssets.loadInts("jme_withdrawal_p2.bin", 20400)
        val p5 = EptAssets.loadInts("jme_withdrawal_p5.bin", 20400)
        assertEquals(20400, p2.size)
        assertEquals(20400, p5.size)
        assertEquals(1024, EptJmeResistanceTable.LOOKUP.size)
    }
}
