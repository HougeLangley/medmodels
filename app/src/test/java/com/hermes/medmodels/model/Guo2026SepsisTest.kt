package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guo2026 双模型锚点测试：与模型卡（01h-模型卡-Guo2026脓毒症）第 6 节严格对应。
 * 三处一致纪律：模型卡 = Guo2026Sepsis 实现 = 本测试锚点。
 * 概率容差 ±0.005；A-T4 贴 10% 阈值 0.9pp 仅做概率断言（卡 §5 纪律）。
 */
class Guo2026SepsisTest {

    // ===== 模型 A：新冠脓毒症（age/hr/crp/lym/lac/oi）=====
    private fun a(age: Double, hr: Double, crp: Double, lym: Double, lac: Double, oi: Double) =
        listOf(age, hr, crp, lym, lac, oi)

    @Test
    fun `A-T1 队列中位 78点4 高`() {
        val r = Guo2026SepsisCovid.assess(a(68.0, 105.0, 99.0, 4.0, 1.81, 189.0))
        assertEquals(0.78432, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `A-T2 极轻 0点8 低`() {
        val r = Guo2026SepsisCovid.assess(a(50.0, 90.0, 20.0, 8.0, 1.0, 300.0))
        assertEquals(0.00758, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `A-T3 死亡向 99点7 高`() {
        val r = Guo2026SepsisCovid.assess(a(75.0, 120.0, 150.0, 2.0, 3.0, 120.0))
        assertEquals(0.99739, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `A-T4 LYM12 10点9 仅概率断言`() {
        // 0.10900 贴 10% 阈值 0.9pp，按贴线纪律只断言概率（卡 §5）
        val r = Guo2026SepsisCovid.assess(a(68.0, 105.0, 99.0, 12.0, 1.81, 189.0))
        assertEquals(0.10900, r.value, 0.005)
    }

    @Test
    fun `A-T5 Lac4 98点1 高`() {
        val r = Guo2026SepsisCovid.assess(a(68.0, 105.0, 99.0, 4.0, 4.0, 189.0))
        assertEquals(0.98139, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `A-T6 OI100 90点6 高`() {
        val r = Guo2026SepsisCovid.assess(a(68.0, 105.0, 99.0, 4.0, 1.81, 100.0))
        assertEquals(0.90636, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `A-T7 域外输入被拒绝`() {
        val bad = listOf(
            a(17.0, 105.0, 99.0, 4.0, 1.81, 189.0),   // 年龄域外
            a(68.0, 250.0, 99.0, 4.0, 1.81, 189.0),   // 心率域外
            a(68.0, 105.0, 600.0, 4.0, 1.81, 189.0),  // CRP 域外
            a(68.0, 105.0, 99.0, 0.5, 1.81, 189.0),   // 淋巴比例域外
            a(68.0, 105.0, 99.0, 4.0, 25.0, 189.0),   // 乳酸域外
            a(68.0, 105.0, 99.0, 4.0, 1.81, 30.0),    // 氧合指数域外
        )
        for (b in bad) {
            try {
                Guo2026SepsisCovid.assess(b)
                throw AssertionError("应抛 IllegalArgumentException：$b")
            } catch (e: IllegalArgumentException) { /* 预期 */ }
        }
    }

    // ===== 模型 B：非新冠脓毒症（age/hr/k/na/ldh/pt/ddimer/lac）=====
    private fun b(age: Double, hr: Double, k: Double, na: Double, ldh: Double, pt: Double, dd: Double, lac: Double) =
        listOf(age, hr, k, na, ldh, pt, dd, lac)

    @Test
    fun `B-T1 队列中位 22点7 中`() {
        val r = Guo2026SepsisNonCovid.assess(b(60.5, 112.5, 4.06, 142.25, 455.0, 14.38, 5.99, 1.86))
        assertEquals(0.22746, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `B-T2 极轻 0点3 低`() {
        val r = Guo2026SepsisNonCovid.assess(b(40.0, 95.0, 3.8, 140.0, 200.0, 12.0, 1.0, 1.2))
        assertEquals(0.00339, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `B-T3 死亡向 98点0 高`() {
        val r = Guo2026SepsisNonCovid.assess(b(70.0, 125.0, 5.0, 148.0, 800.0, 18.0, 12.0, 3.5))
        assertEquals(0.98044, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `B-T4 Na135 14点3 中`() {
        val r = Guo2026SepsisNonCovid.assess(b(60.5, 112.5, 4.06, 135.0, 455.0, 14.38, 5.99, 1.86))
        assertEquals(0.14329, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `B-T5 PT20 49点2 高`() {
        val r = Guo2026SepsisNonCovid.assess(b(60.5, 112.5, 4.06, 142.25, 455.0, 20.0, 5.99, 1.86))
        assertEquals(0.49218, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `B-T6 DD20 42点9 高`() {
        val r = Guo2026SepsisNonCovid.assess(b(60.5, 112.5, 4.06, 142.25, 455.0, 14.38, 20.0, 1.86))
        assertEquals(0.42946, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `B-T7 域外输入被拒绝`() {
        val bad = listOf(
            b(60.5, 112.5, 1.5, 142.25, 455.0, 14.38, 5.99, 1.86),   // K 域外
            b(60.5, 112.5, 4.06, 100.0, 455.0, 14.38, 5.99, 1.86),   // Na 域外
            b(60.5, 112.5, 4.06, 142.25, 6000.0, 14.38, 5.99, 1.86), // LDH 域外
            b(60.5, 112.5, 4.06, 142.25, 455.0, 45.0, 5.99, 1.86),   // PT 域外
            b(60.5, 112.5, 4.06, 142.25, 455.0, 14.38, 200.0, 1.86), // D-dimer 域外
            b(60.5, 112.5, 4.06, 142.25, 455.0, 14.38, 5.99, 25.0),  // 乳酸域外
        )
        for (x in bad) {
            try {
                Guo2026SepsisNonCovid.assess(x)
                throw AssertionError("应抛 IllegalArgumentException：$x")
            } catch (e: IllegalArgumentException) { /* 预期 */ }
        }
    }

    @Test
    fun `T15 双模型风险带常量与模型卡一致`() {
        // 模型卡：BAND_LOW_UPPER=0.10、BAND_HIGH_LOWER=0.40（两模型同）——通过行为验证
        // 中位病例 B-T1 = 0.22746 应为中带（10–40 内）
        val r = Guo2026SepsisNonCovid.assess(b(60.5, 112.5, 4.06, 142.25, 455.0, 14.38, 5.99, 1.86))
        assertEquals("中风险", r.bandLabel)
        // A-T2 = 0.00758 低带；A-T1 = 0.78432 高带
        assertEquals("低风险", Guo2026SepsisCovid.assess(a(50.0, 90.0, 20.0, 8.0, 1.0, 300.0)).bandLabel)
        assertEquals("高风险", Guo2026SepsisCovid.assess(a(68.0, 105.0, 99.0, 4.0, 1.81, 189.0)).bandLabel)
    }
}
