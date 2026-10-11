package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Servitje2026 锚点测试：与模型卡（01k-模型卡-Servitje2026cPPE）第 5 节 T1–T12 严格对应。
 * 三处一致纪律：模型卡 §3.1 常量 = Servitje2026Cppe 实现 = 本测试锚点。
 * 概率容差 ±0.005；T6/T7 因评分-组率口径差（卡 §5）只做概率断言不做带位断言。
 * bedsideScore 仅核验展示参照算术，非结果输出。
 */
class Servitje2026Test {

    private fun v(age: Double, retr: Double, crp: Double, thick: Double, shift: Double) =
        listOf(age, retr, crp, thick, shift)

    @Test
    fun `T1 全阴下界 0点2 低`() {
        val r = Servitje2026Cppe.assess(v(18.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.00225, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
        assertEquals(0, Servitje2026Cppe.bedsideScore(v(18.0, 0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `T2 全阴基线 0点4 低`() {
        val r = Servitje2026Cppe.assess(v(16.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.00420, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
        assertEquals(0, Servitje2026Cppe.bedsideScore(v(16.0, 0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `T3 加凹陷 1点3 低`() {
        val r = Servitje2026Cppe.assess(v(16.0, 1.0, 0.0, 0.0, 0.0))
        assertEquals(0.01343, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
        assertEquals(4, Servitje2026Cppe.bedsideScore(v(16.0, 1.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `T4 加凹陷加CRP 3点7 低`() {
        val r = Servitje2026Cppe.assess(v(16.0, 1.0, 1.0, 0.0, 0.0))
        assertEquals(0.03707, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
        assertEquals(8, Servitje2026Cppe.bedsideScore(v(16.0, 1.0, 1.0, 0.0, 0.0)))
    }

    @Test
    fun `T5 加厚1cm 7点7 低`() {
        val r = Servitje2026Cppe.assess(v(16.0, 1.0, 1.0, 1.0, 0.0))
        assertEquals(0.07690, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)
        assertEquals(11, Servitje2026Cppe.bedsideScore(v(16.0, 1.0, 1.0, 1.0, 0.0)))
    }

    @Test
    fun `T6 中组代表 73点6 仅概率断言`() {
        // 评分 23 落高带，但原文中组实测率 55.9%——评分带与组率是两个口径，不做带位断言（卡 §5）
        val r = Servitje2026Cppe.assess(v(6.0, 1.0, 1.0, 1.5, 0.0))
        assertEquals(0.73551, r.value, 0.005)
        assertEquals(23, Servitje2026Cppe.bedsideScore(v(6.0, 1.0, 1.0, 1.5, 0.0)))
    }

    @Test
    fun `T7 高组代表 91点8 仅概率断言`() {
        // 同 T6：评分-组率口径差，只断言概率（卡 §5）
        val r = Servitje2026Cppe.assess(v(4.0, 1.0, 1.0, 2.5, 0.0))
        assertEquals(0.91827, r.value, 0.005)
        assertEquals(26, Servitje2026Cppe.bedsideScore(v(4.0, 1.0, 1.0, 2.5, 0.0)))
    }

    @Test
    fun `T8 全阳 99点9 极高`() {
        val r = Servitje2026Cppe.assess(v(2.0, 1.0, 1.0, 4.0, 1.0))
        assertEquals(0.99861, r.value, 0.005)
        assertEquals("极高风险", r.bandLabel)
        assertEquals(43, Servitje2026Cppe.bedsideScore(v(2.0, 1.0, 1.0, 4.0, 1.0)))
    }

    @Test
    fun `T9 成人全阳 90点1 极高`() {
        // ≥16 岁档 0 分验证（卡 §5：16 岁全阳评分 28）
        val r = Servitje2026Cppe.assess(v(16.0, 1.0, 1.0, 4.0, 1.0))
        assertEquals(0.90075, r.value, 0.005)
        assertEquals("极高风险", r.bandLabel)
        assertEquals(28, Servitje2026Cppe.bedsideScore(v(16.0, 1.0, 1.0, 4.0, 1.0)))
    }

    @Test
    fun `T10 全阳无移位 98点5 极高`() {
        // 移位贡献差分验证：T8−T10 logit 差 = 2.3751（B_MEDIASTINAL_SHIFT）
        val r8 = Servitje2026Cppe.assess(v(2.0, 1.0, 1.0, 4.0, 1.0))
        val r10 = Servitje2026Cppe.assess(v(2.0, 1.0, 1.0, 4.0, 0.0))
        assertEquals(0.98524, r10.value, 0.005)
        assertEquals("极高风险", r10.bandLabel)
        assertEquals(35, Servitje2026Cppe.bedsideScore(v(2.0, 1.0, 1.0, 4.0, 0.0)))
        val logitDiff = Math.log(r8.value / (1 - r8.value)) - Math.log(r10.value / (1 - r10.value))
        assertEquals(2.3751, logitDiff, 0.001)
    }

    @Test
    fun `T11 仅厚度 11点4 低`() {
        val r = Servitje2026Cppe.assess(v(10.0, 0.0, 0.0, 2.0, 0.0))
        assertEquals(0.11394, r.value, 0.005)
        assertEquals("低风险", r.bandLabel)  // 11.4% < 15% 低带（卡 §5 断言：低）
        assertEquals(12, Servitje2026Cppe.bedsideScore(v(10.0, 0.0, 0.0, 2.0, 0.0)))
    }

    @Test
    fun `T12 档位边界 97点4 极高`() {
        // 3.99 岁→15 分（<4 档）、0.99 cm→0 分（<1 档）双侧边界同击（卡 §5）
        val r = Servitje2026Cppe.assess(v(3.99, 1.0, 1.0, 0.99, 1.0))
        assertEquals(0.97421, r.value, 0.005)
        assertEquals("极高风险", r.bandLabel)
        assertEquals(31, Servitje2026Cppe.bedsideScore(v(3.99, 1.0, 1.0, 0.99, 1.0)))
    }

    @Test
    fun `域外输入被拒绝`() {
        for (bad in listOf(
            v(19.0, 0.0, 0.0, 0.0, 0.0),     // 年龄 19 > 18
            v(16.0, 0.0, 0.0, 11.0, 0.0),    // 厚度 11 > 10
            v(-1.0, 0.0, 0.0, 0.0, 0.0),     // 负年龄
            v(16.0, 2.0, 0.0, 0.0, 0.0),     // 开关传非法值
        )) {
            try {
                Servitje2026Cppe.assess(bad)
                throw AssertionError("应抛 IllegalArgumentException：$bad")
            } catch (e: IllegalArgumentException) {
                // 预期：中文错误消息
            }
        }
    }
}
