package com.hermes.medmodels.model

import kotlin.math.floor

/**
 * Lamberink 2018 术后 AED 撤药复发风险（儿童癫痫外科，TimeToStop）。
 *
 * 来源：Lamberink HJ, et al. Epilepsia 2018;59(3):e28–e33; PMID 29446447（TimeToStop 766 儿童 15 中心）。
 * 算法源：epilepsypredictiontools @ 6444da144e（Apache-2.0）tts-calc.js。
 *
 * 模型卡：Obsidian 医学评估模型/01o-模型卡-Lamberink2018TTS术后撤药
 *
 * ⚠️ 三处一致纪律：EptTables 表值 = 模型卡 §3（pin commit JS 逐值）= 单测 T1–T5。
 * ⚠️ M3「cure」模型不实现（原文弃建——JS 中 AETIOL/NAED_PTS3/RISK3_CURE 一律未移植）。
 * ⚠️ 饱和串 <2/>54、<5/>85、>98/<30 原样显示；发作频率 40 档最近邻（findClosestSfreq 直抄）。
 */
object Lamberink2018Tts : AssessmentModel {

    override val id = "lamberink2018_tts"
    override val displayName = "术后撤药复发风险（儿童癫痫外科）"
    override val category = "神经系统"
    override val summary = "Lamberink 2018 TimeToStop · 766 儿童 15 中心；2y/5y 复发 + 6y 无发作（分值→查表）"

    override val audienceNotice = "儿童癫痫术后无发作、考虑撤药者；非手术缓解者禁用——转 AED 撤药模型"

    override val inputs = listOf(
        ModelInput("ageWithdrawal", "撤药时年龄", ModelInput.InputType.CONTINUOUS, 0.0, 26.0, "岁", "启动撤药时年龄（整数岁，钳位 0–26）"),
        ModelInput("ttr", "手术至撤药间隔", ModelInput.InputType.CONTINUOUS, 0.0, 85.0, "月", "手术到开始撤药的月数（整数，钳位 0–85）"),
        ModelInput("mri", "术前 MRI", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "", options = listOf(
            CategoryOption("focal", "局灶异常", 0.0),
            CategoryOption("multifocal", "多灶异常", 1.0),
            CategoryOption("normal", "正常", 2.0),
        )),
        ModelInput("complete", "切除完整性", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "「未确定」与「完全」同分（原文参考类合并）", options = listOf(
            CategoryOption("complete", "完全切除", 0.0),
            CategoryOption("incomplete", "不完全切除", 1.0),
            CategoryOption("not determined", "未确定", 2.0),
        )),
        ModelInput("eeg", "撤药前 EEG", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "「未做」与「正常」同分", options = listOf(
            CategoryOption("normal", "正常", 0.0),
            CategoryOption("not performed", "未做", 1.0),
            CategoryOption("interictal epileptiform discharges", "间期痫样放电（IED）", 2.0),
        )),
        ModelInput("sfreq", "术前发作频率", ModelInput.InputType.CONTINUOUS, 0.0, 24.0, "次/天", "按 40 档最近邻匹配（原站行为）"),
        ModelInput("naed", "手术时 AED 数量", ModelInput.InputType.CONTINUOUS, 0.0, 5.0, "种", "手术当时用药种数（整数，钳位 0–5）"),
    )

    override val disclaimers = listOf(
        "适用：儿童癫痫术后无发作、考虑撤药者；非手术缓解者请用「撤药复发（AED）」模型",
        "欧洲 TimeToStop 回顾性队列（766 儿童，2000–2008 手术）；内部验证+四大中心交叉，无真正独立外部验证（原文自述局限）",
        "区分度中等（C 0.68/0.73）；大多数患儿预测无发作概率集中在 95–100% 区间（原文注）",
        "仅纳入医生认为可安全撤药的选择性人群——用于「已考虑撤药、权衡风险」场景；对全部术后患儿外推会低估复发",
        "「术后仍有发作」者数据不足（13 例 0 复发）未能建模——该人群不适用",
        "输出为区间整数概率（含饱和档），系原模型发布口径；cure（无发作且停药）结局原文弃建，不提供",
        "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）",
    )

    private fun jsRound(x: Double) = floor(x + 0.5)

    private fun lookupRisk(total: Double, table: Array<String>, tableName: String): Pair<Double, String> {
        val rounded = jsRound(total * 2.0) / 2.0
        val idx = jsRound(total * 2.0).toInt()
        require(idx >= 0 && idx < table.size) { "$tableName 总分 $rounded 越界（表长 ${table.size}）" }
        return rounded to table[idx]
    }

    private fun riskToNumber(s: String): Double = when {
        s.startsWith("<") -> (s.substring(1).toDoubleOrNull() ?: 0.0) / 100.0
        s.startsWith(">") -> (s.substring(1).toDoubleOrNull() ?: 100.0) / 100.0
        else -> (s.toDoubleOrNull() ?: 0.0) / 100.0
    }

    /** findClosestSfreq 直抄：严格小于才更新（平手取先出现档；浮点 ties 按实际距离） */
    internal fun findClosestSfreq(v: Double): Int {
        val vals = EptTables.TTS_SFREQ_VALS
        var best = 0
        var bestDiff = Math.abs(vals[0] - v)
        for (i in 1 until vals.size) {
            val d = Math.abs(vals[i] - v)
            if (d < bestDiff) { bestDiff = d; best = i }
        }
        return best
    }

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        val age = values[0].toInt().coerceIn(0, 26)
        val ttr = values[1].toInt().coerceIn(0, 85)
        val naed = values[6].toInt().coerceIn(0, 5)
        val t = EptTables

        val ageIdx = t.TTS_AGEWITH_VALS.indexOf(age).also { require(it >= 0) { "年龄 $age 查无分值" } }
        val ttrIdx = t.TTS_TTR_VALS.indexOf(ttr).also { require(it >= 0) { "TTR $ttr 查无分值" } }
        val mriPt = when (values[2].toInt()) { 1 -> 12.0; else -> 0.0 }            // 多灶=12，余=0
        val compPt = when (values[3].toInt()) { 1 -> 12.5; else -> 0.0 }           // 不完全=12.5，余=0
        val eegPt = t.TTS_EEG_PTS1[values[4].toInt()]                              // IED=7.5，余=0

        // M1（2y/5y 复发）
        val total1 = t.TTS_AGEWITH_PTS1[ageIdx] + t.TTS_TTR_PTS1[ttrIdx] + mriPt + compPt + eegPt
        // M2（6y 无发作）
        val sfIdx = findClosestSfreq(values[5])
        val total2 = t.TTS_SFREQ_PTS2[sfIdx] + t.TTS_NAED_PTS2[t.TTS_NAED_VALS.indexOf(naed)] + compPt

        val (t1, r2y) = lookupRisk(total1, t.TTS_RISK1_2Y, "RISK1_2Y")
        val (_, r5y) = lookupRisk(total1, t.TTS_RISK1_5Y, "RISK1_5Y")
        val (t2, rLong) = lookupRisk(total2, t.TTS_RISK2_LONG, "RISK2_LONG")

        return AssessmentResult(
            value = riskToNumber(r2y),
            displayValue = "$r2y%",
            bandLabel = "2 年复发风险",
            bandExplanation = "术后撤药 2 年复发风险（查表整数口径）。M1 总分 $t1 / M2 总分 $t2；发作频率按 ${t.TTS_SFREQ_VALS[sfIdx]} 次/天档计。",
            valueLabel = "撤药后 2 年复发（查表整数口径）",
            extraOutputs = listOf("5 年复发风险" to "$r5y%", "6 年无发作概率" to "$rLong%"),
            details = listOf(
                "模型" to "Lamberink 2018 TTS 术后撤药（PMID 29446447，Epilepsia）",
                "性能" to "C-stat M1 0.68（0.67–0.69）/ M2 0.73（0.72–0.75）；IECV 0.61–0.69 / 0.71–0.75；双模型校准图",
                "队列" to "TimeToStop 766 儿童（欧洲 15 中心，8 国）",
                "输出口径" to "查表整数/饱和串原样显示；cure 结局原文弃建不提供",
                "适用人群" to "儿童癫痫术后无发作、考虑撤药者",
            ),
        )
    }
}
