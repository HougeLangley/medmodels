package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Knight2020 4C 锚点测试：与模型卡（01g-模型卡-Knight2020_4C）第 6 节 T1–T14 严格对应。
 * 三处一致纪律：模型卡分档函数 = Knight2020FourC 实现 = 本测试锚点。
 * 整数评分容差 0；T6/T7/T9/T10 为分档函数专项（每切点两侧都有断言）。
 * 输入顺序：age/rr/spo2/urea/crp/sex/impaired/comorb
 */
class Knight2020FourCTest {

    private fun v(age: Double, rr: Double, spo2: Double, urea: Double, crp: Double,
                  sex: Double, impaired: Double, comorb: Double) =
        listOf(age, rr, spo2, urea, crp, sex, impaired, comorb)

    @Test
    fun `T1 理论最低 0分 低危`() {
        val r = Knight2020FourC.assess(v(49.0, 18.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0))
        assertEquals(0.0, r.value, 0.0)
        assertEquals("低风险", r.bandLabel)
    }

    @Test
    fun `T2 五切点下限 5分 中危`() {
        // 50岁(2)+RR20(1)+SpO₂92(0)+尿素7.0(1)+CRP50(1)+女(0)+无意识(0)+无合并(0)
        val r = Knight2020FourC.assess(v(50.0, 20.0, 92.0, 7.0, 50.0, 0.0, 0.0, 0.0))
        assertEquals(5.0, r.value, 0.0)
        assertEquals("中风险", r.bandLabel)
    }

    @Test
    fun `T3 高带下限 9分 高危`() {
        // 65岁(4)+男(1)+1合并(1)+RR24(1)+SpO₂93(0)+GCS15(0)+尿素8(1)+CRP60(1)
        val r = Knight2020FourC.assess(v(65.0, 24.0, 93.0, 8.0, 60.0, 1.0, 0.0, 1.0))
        assertEquals(9.0, r.value, 0.0)
        assertEquals("高风险", r.bandLabel)
    }

    @Test
    fun `T4 极高带下限 15分 极高危`() {
        // 75岁(6)+男(1)+≥2合并(2)+RR26(1)+SpO₂90(2)+GCS15(0)+尿素10(1)+CRP120(2)
        val r = Knight2020FourC.assess(v(75.0, 26.0, 90.0, 10.0, 120.0, 1.0, 0.0, 2.0))
        assertEquals(15.0, r.value, 0.0)
        assertEquals("极高风险", r.bandLabel)
    }

    @Test
    fun `T5 满格 21分 极高危`() {
        // 85(7)+男(1)+≥2(2)+RR32(2)+SpO₂88(2)+GCS13(2)+尿素20(3)+CRP150(2)
        val r = Knight2020FourC.assess(v(85.0, 32.0, 88.0, 20.0, 150.0, 1.0, 1.0, 2.0))
        assertEquals(21.0, r.value, 0.0)
        assertEquals("极高风险", r.bandLabel)
    }

    @Test
    fun `T6 年龄界扫描 逐点断言`() {
        val ages = listOf(49, 50, 59, 60, 69, 70, 79, 80)
        val expected = listOf(0, 2, 2, 4, 4, 6, 6, 7)
        ages.zip(expected).forEach { (a, e) ->
            val r = Knight2020FourC.assess(v(a.toDouble(), 18.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0))
            assertEquals("age=$a", e.toDouble(), r.value, 0.0)
        }
    }

    @Test
    fun `T7 呼吸频率界扫描`() {
        val rrs = listOf(19, 20, 29, 30)
        val expected = listOf(0, 1, 1, 2)
        rrs.zip(expected).forEach { (rr, e) ->
            val r = Knight2020FourC.assess(v(49.0, rr.toDouble(), 95.0, 5.0, 10.0, 0.0, 0.0, 0.0))
            assertEquals("rr=$rr", e.toDouble(), r.value, 0.0)
        }
    }

    @Test
    fun `T8 SpO2 界 91与92差2分`() {
        val a = Knight2020FourC.assess(v(49.0, 18.0, 91.0, 5.0, 10.0, 0.0, 0.0, 0.0)).value
        val b = Knight2020FourC.assess(v(49.0, 18.0, 92.0, 5.0, 10.0, 0.0, 0.0, 0.0)).value
        assertEquals(2.0, a - b, 0.0)
    }

    @Test
    fun `T9 尿素界扫描 勘误语义专项`() {
        // <7 与 >14 开区间，中档双闭：6.9→0、7.0→1、14.0→1、14.1→3
        val ureas = listOf(6.9, 7.0, 14.0, 14.1)
        val expected = listOf(0, 1, 1, 3)
        ureas.zip(expected).forEach { (u, e) ->
            val r = Knight2020FourC.assess(v(49.0, 18.0, 95.0, u, 10.0, 0.0, 0.0, 0.0))
            assertEquals("urea=$u", e.toDouble(), r.value, 0.0)
        }
    }

    @Test
    fun `T10 CRP 界扫描`() {
        // 49.9→0、50.0→1、99.9→1、100.0→2（50 闭入中档；100 闭入高档）
        val crps = listOf(49.9, 50.0, 99.9, 100.0)
        val expected = listOf(0, 1, 1, 2)
        crps.zip(expected).forEach { (c, e) ->
            val r = Knight2020FourC.assess(v(49.0, 18.0, 95.0, 5.0, c, 0.0, 0.0, 0.0))
            assertEquals("crp=$c", e.toDouble(), r.value, 0.0)
        }
    }

    @Test
    fun `T11 四个档界逐分钉死`() {
        // 3 = 59岁(2)+RR20(1)
        assertEquals(3.0, Knight2020FourC.assess(v(59.0, 20.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0)).value, 0.0)
        // 4 = 60岁单项
        assertEquals(4.0, Knight2020FourC.assess(v(60.0, 19.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0)).value, 0.0)
        // 8 = 70岁(6)+男(1)+RR20(1)，其余零
        assertEquals(8.0, Knight2020FourC.assess(v(70.0, 20.0, 95.0, 5.0, 10.0, 1.0, 0.0, 0.0)).value, 0.0)
        // 14 = 80岁(7)+男(1)+≥2合并(2)+RR30(2)+CRP100(2)，SpO₂92/GCS15/尿素6
        assertEquals(14.0, Knight2020FourC.assess(v(80.0, 30.0, 92.0, 6.0, 100.0, 1.0, 0.0, 2.0)).value, 0.0)
        // 档位断言
        assertEquals("低风险", Knight2020FourC.assess(v(59.0, 20.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0)).bandLabel)
        assertEquals("中风险", Knight2020FourC.assess(v(60.0, 19.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0)).bandLabel)
        assertEquals("中风险", Knight2020FourC.assess(v(70.0, 20.0, 95.0, 5.0, 10.0, 1.0, 0.0, 0.0)).bandLabel)
        assertEquals("高风险", Knight2020FourC.assess(v(80.0, 30.0, 92.0, 6.0, 100.0, 1.0, 0.0, 2.0)).bandLabel)
    }

    @Test
    fun `T12 域外与17岁拒绝消息指引儿童模型`() {
        // 17 岁：消息须含 Wong2022 指引
        try {
            Knight2020FourC.assess(v(17.0, 18.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0))
            throw AssertionError("应抛 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assert(e.message?.contains("Wong2022") == true) { "17 岁拒绝消息应指向 Wong2022：${e.message}" }
        }
        val bad = listOf(
            v(65.0, 18.0, 95.0, 200.0, 10.0, 0.0, 0.0, 0.0),   // 尿素域外
            v(65.0, 18.0, 95.0, 5.0, 1000.0, 0.0, 0.0, 0.0),   // CRP 域外
            v(65.0, 70.0, 95.0, 5.0, 10.0, 0.0, 0.0, 0.0),     // RR 域外
            v(65.0, 18.0, 40.0, 5.0, 10.0, 0.0, 0.0, 0.0),     // SpO₂ 域外
        )
        for (b in bad) {
            try {
                Knight2020FourC.assess(b)
                throw AssertionError("应抛 IllegalArgumentException：$b")
            } catch (e: IllegalArgumentException) { /* 预期 */ }
        }
    }

    @Test
    fun `T13 非法二元与未选合并症被拒绝`() {
        try {
            Knight2020FourC.assess(v(65.0, 18.0, 95.0, 5.0, 10.0, 2.0, 0.0, 0.0))
            throw AssertionError("应抛 IllegalArgumentException（性别传 2.0）")
        } catch (e: IllegalArgumentException) { /* 预期 */ }
        try {
            Knight2020FourC.assess(listOf(65.0, 18.0, 95.0, 5.0, 10.0, 0.0, 0.0, null as Double?) as List<Double>)
            throw AssertionError("应抛 IllegalArgumentException（合并症未选）")
        } catch (e: IllegalArgumentException) { /* 预期 */ }
    }

    @Test
    fun `T14 带界常量闭区间语义`() {
        assertEquals(3, Knight2020FourC.LOW_MAX)
        assertEquals(8, Knight2020FourC.MID_MAX)
        assertEquals(14, Knight2020FourC.HIGH_MAX)
    }
}
