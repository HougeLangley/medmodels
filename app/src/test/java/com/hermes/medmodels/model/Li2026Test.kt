package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Li2026 锚点测试：与模型卡（01b-模型卡-Li2026）第 7 节冻结值严格对应。
 * 原文无 worked example，锚点为 Hermes 数值核验后自建冻结值（2026-08-24）。
 * 三处一致纪律：模型卡公式 = Li2026RsvSevere 常量 = 本测试锚点。
 */
class Li2026Test {

    @Test
    fun `病例A 高危典型 82点9`() {
        // NLR0.66/病程5/3月/冬/低氧/早产 → 冻结值 82.9%
        val r = Li2026RsvSevere.assess(listOf(0.66, 5.0, 3.0, 1.0, 1.0, 1.0))
        assertEquals(0.829, r.value, 0.005)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `病例B 极低危 0点4`() {
        // NLR1.5/病程7/60月/夏/全无 → 冻结值 0.40%
        val r = Li2026RsvSevere.assess(listOf(1.5, 7.0, 60.0, 0.0, 0.0, 0.0))
        assertEquals(0.004, r.value, 0.003)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `病例C 中危 11点1`() {
        // NLR1.0/病程5/6月/冬/无低氧非早产 → 冻结值 11.1%
        val r = Li2026RsvSevere.assess(listOf(1.0, 5.0, 6.0, 1.0, 0.0, 0.0))
        assertEquals(0.111, r.value, 0.005)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `病例D 全最小是中危而非低危`() {
        // NLR0/病程1/0月/全阴 → 冻结值 5.05%（年龄保护方向：0月龄反而抬概率）
        val r = Li2026RsvSevere.assess(listOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.0505, r.value, 0.003)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `病例E 右边界饱和`() {
        // NLR30/病程60/0月/全阳 → >99%
        val r = Li2026RsvSevere.assess(listOf(30.0, 60.0, 0.0, 1.0, 1.0, 1.0))
        assertTrue("应 >0.999，实得 ${r.value}", r.value > 0.999)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `非法输入被拒绝`() {
        val bad = listOf(
            listOf(35.0, 5.0, 3.0, 1.0, 1.0, 1.0),   // NLR 超范围
            listOf(1.0, 0.0, 3.0, 1.0, 1.0, 1.0),    // 病程 <1
            listOf(1.0, 5.0, 300.0, 1.0, 1.0, 1.0),  // 月龄超范围（填了"岁"被拦截）
        )
        for (v in bad) {
            try {
                Li2026RsvSevere.assess(v)
                throw AssertionError("应抛 IllegalArgumentException：$v")
            } catch (e: IllegalArgumentException) {
                // 预期
            }
        }
    }

    @Test
    fun `风险带常量与模型卡一致`() {
        assertEquals(0.03, Li2026RsvSevere.BAND_LOW_UPPER, 1e-9)
        assertEquals(0.20, Li2026RsvSevere.BAND_MID_UPPER, 1e-9)
    }
}
