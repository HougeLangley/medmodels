package com.hermes.medmodels.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * van Diessen 2018 锚点测试：与模型卡（01m-模型卡-vanDiessen2018FSC）§5 T1–T6 严格对应。
 * 三处一致纪律：模型卡 §3 常量 = VanDiessen2018Fsc 实现 = 本测试锚点。
 * 输入序 (male, age, bilateral, cramp, lateral, tone, auto, hxNeuro, hxPsych, syndrome, eeg系数)。
 */
class VanDiessen2018Test {

    private fun v(male: Double, age: Double, bil: Double, cramp: Double, lat: Double, tone: Double,
                  auto: Double, hxn: Double, hxp: Double, synd: Double, eeg: Double) =
        listOf(male, age, bil, cramp, lat, tone, auto, hxn, hxp, synd, eeg)

    @Test
    fun `T1 域下界女婴全阴 2点3`() {
        val r = VanDiessen2018Fsc.assess(v(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.02311, r.value, 0.005)
        assertEquals("2.3%", r.displayValue)
    }

    @Test
    fun `T2 年龄单变量 3点5`() {
        val r = VanDiessen2018Fsc.assess(v(0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(0.03473, r.value, 0.005)
    }

    @Test
    fun `T3 低中位 26点0`() {
        val r = VanDiessen2018Fsc.assess(v(0.0, 5.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.27371))
        assertEquals(0.26027, r.value, 0.005)
        assertEquals("26.0%", r.displayValue)
    }

    @Test
    fun `T4 自动症神经史全面棘波 98点2`() {
        val r = VanDiessen2018Fsc.assess(v(1.0, 8.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 3.80806))
        assertEquals(0.98218, r.value, 0.005)
    }

    @Test
    fun `T5 多体征局灶棘波 99点8`() {
        val r = VanDiessen2018Fsc.assess(v(1.0, 12.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 3.20263))
        assertEquals(0.99810, r.value, 0.005)
    }

    @Test
    fun `T6 域上界全阳性 99点98`() {
        val r = VanDiessen2018Fsc.assess(v(1.0, 18.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 3.80806))
        assertEquals(0.99982, r.value, 0.005)
    }

    @Test
    fun `年龄钳位 25按18计 与T6同值`() {
        val r = VanDiessen2018Fsc.assess(v(1.0, 25.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 3.80806))
        assertEquals(0.99982, r.value, 0.005)  // 年龄 25 钳位到 18，与 T6 同值
    }
}
