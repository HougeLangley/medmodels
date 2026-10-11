package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wong2022 锚点测试：与模型卡（01f-模型卡-Wong2022）第 7 节 T1–T13 严格对应。
 * 三处一致纪律：模型卡 = Wong2022CovidSevere 常量 = 本测试锚点。
 * 概率容差 ±0.005；T8/T10 贴 3% 阈值（<2pp），按避坑纪律仅做概率断言，不做带位断言。
 */
class Wong2022Test {

    // 输入顺序：infant/comorbidity/seizures/vomiting/fever/coryza + ANC/Hb/PLT
    private fun v(inf: Double, com: Double, sei: Double, vom: Double, fev: Double, cor: Double,
                  anc: Double, hb: Double, plt: Double) =
        listOf(inf, com, sei, vom, fev, cor, anc, hb, plt)

    @Test
    fun `T1 全阴中位 0点3 低危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 294.0))
        assertEquals(0.00319, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T2 加发热 1点0 低危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.9, 12.8, 294.0))
        assertEquals(0.01043, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T3 加卡他保护 0点06 低危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.9, 12.8, 294.0))
        assertEquals(0.00055, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T4 理论最重 99点95 高危`() {
        val r = Wong2022CovidSevere.assess(v(1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 10.0, 10.0, 200.0))
        assertEquals(0.99953, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T5 重症典型谱 33点5 高危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 5.0, 10.2, 250.0))
        assertEquals(0.33537, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T6 贫血Hb7 8点0 中危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 7.0, 294.0))
        assertEquals(0.08039, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T7 PLT50 3点5 中危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 50.0))
        assertEquals(0.03547, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T8 婴儿单项 2点8 仅概率断言`() {
        // 0.02757 距 3% 阈值 <2pp，按避坑纪律（贴线锚点）只断言概率，不断言带位
        val r = Wong2022CovidSevere.assess(v(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 294.0))
        assertEquals(0.02757, r.value, 0.005)
    }

    @Test
    fun `T9 惊厥单项 8点3 中危`() {
        val r = Wong2022CovidSevere.assess(v(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.9, 12.8, 294.0))
        assertEquals(0.08294, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T10 合并症单项 2点0 仅概率断言`() {
        // 0.02037 贴 3% 阈值，只断言概率
        val r = Wong2022CovidSevere.assess(v(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 294.0))
        assertEquals(0.02037, r.value, 0.005)
    }

    @Test
    fun `T11 域外连续值被拒绝`() {
        val bad = listOf(
            v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 60.0, 12.8, 294.0),   // ANC 域外
            v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 25.0, 294.0),     // Hb 域外
            v(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 1000.0),    // PLT 域外
        )
        for (b in bad) {
            try {
                Wong2022CovidSevere.assess(b)
                throw AssertionError("应抛 IllegalArgumentException：$b")
            } catch (e: IllegalArgumentException) { /* 预期 */ }
        }
    }

    @Test
    fun `T12 BINARY 非法值被拒绝`() {
        try {
            Wong2022CovidSevere.assess(v(2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9, 12.8, 294.0))
            throw AssertionError("应抛 IllegalArgumentException")
        } catch (e: IllegalArgumentException) { /* 预期 */ }
    }

    @Test
    fun `T13 带位常量与模型卡一致`() {
        assertEquals(0.03, Wong2022CovidSevere.BAND_LOW_UPPER, 1e-9)
        assertEquals(0.20, Wong2022CovidSevere.BAND_HIGH_LOWER, 1e-9)
    }
}
