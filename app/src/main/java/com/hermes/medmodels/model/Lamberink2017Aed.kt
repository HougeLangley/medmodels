package com.hermes.medmodels.model

import kotlin.math.floor

/**
 * Lamberink 2017 AED 撤药后发作复发风险（癫痫缓解期，非手术人群）。
 *
 * 来源：Lamberink HJ, et al. Lancet Neurol 2017;16(7):523–531; PMID 28483337。
 * 算法源：epilepsypredictiontools @ 6444da144e（Apache-2.0）aed-calc.js——
 * **分值→风险表架构**：列线图分值表 + 总分查风险表（离散输出，含饱和串）。
 *
 * 模型卡：Obsidian 医学评估模型/01n-模型卡-Lamberink2017AED撤药
 *
 * ⚠️ 三处一致纪律：EptTables 表值 = 模型卡 §3（pin commit JS 逐值）= 单测 T1–T6。
 * ⚠️ 输出纪律：风险表值原样显示（整数% 或饱和串 <10/>90/>99/<40 + "%"），禁连续概率、禁插值。
 * ⚠️ 总分越界 = 报错（不钳位到表端）；输入先按原站规则钳位（ttr 24/dur 40/age 80/naed 9）。
 */
object Lamberink2017Aed : AssessmentModel {

    override val id = "lamberink2017_aed"
    override val displayName = "撤药复发风险（癫痫缓解期·非手术）"
    override val category = "神经系统"
    override val summary = "Lamberink 2017 · IPD meta 10 队列 1769 例；2y/5y 复发 + 10y 无发作（分值→查表）"

    override val audienceNotice = "药物控制无发作、考虑减停 AED 的患者（儿童+成人）；术后缓解者禁用——转 TTS 模型"

    override val inputs = listOf(
        ModelInput("ttr", "距末次发作时间", ModelInput.InputType.CONTINUOUS, 0.0, 24.0, "年", "撤药前无发作年限（整数年，原站钳位 0–24）"),
        ModelInput("duration", "癫痫病程", ModelInput.InputType.CONTINUOUS, 0.0, 40.0, "年", "起病至撤药的总病程（整数年，钳位 0–40）"),
        ModelInput("ageOnset", "起病年龄", ModelInput.InputType.CONTINUOUS, 0.0, 80.0, "岁", "首次发作年龄（整数岁，钳位 0–80）"),
        ModelInput("histfeb", "热性惊厥史", ModelInput.InputType.BINARY, 0.0, 1.0, "", ""),
        ModelInput("nseizures", "发作总次数", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "撤药前累计发作次数", options = listOf(
            CategoryOption("0-9", "0–9 次", 0.0),
            CategoryOption("10 or more", "≥10 次", 1.0),
        )),
        ModelInput("benign", "自限性癫痫综合征", ModelInput.InputType.BINARY, 0.0, 1.0, "", "如儿童良性 Rolandic 癫痫等", negativeLabel = "否", positiveLabel = "是"),
        ModelInput("delay", "发育迟滞", ModelInput.InputType.BINARY, 0.0, 1.0, "", ""),
        ModelInput("eeg", "撤药前 EEG", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "", options = listOf(
            CategoryOption("normal", "正常", 0.0),
            CategoryOption("not performed", "未做", 1.0),
            CategoryOption("epileptiform abnormality", "痫样异常", 2.0),
        )),
        ModelInput("naed", "撤药时 AED 数量", ModelInput.InputType.CONTINUOUS, 0.0, 9.0, "种", "正在服用的抗发作药物种数（整数，钳位 0–9）"),
        ModelInput("sex", "性别", ModelInput.InputType.BINARY, 0.0, 1.0, "", "", negativeLabel = "男", positiveLabel = "女"),
        ModelInput("famhist", "癫痫家族史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "一级亲属癫痫史"),
        ModelInput("focal", "局灶性发作", ModelInput.InputType.BINARY, 0.0, 1.0, "", "发作类型含局灶性（仅全面性=无）"),
    )

    override val disclaimers = listOf(
        "适用：药物缓解期、考虑撤药的癫痫患者（儿童+成人）；术后缓解者请用「术后撤药（TTS）」模型",
        "IPD meta 分析（10 队列 1769 例，儿童+成人混合）；区分度中等（C 0.65/0.71）——个体层面不确定性大，结果供医患共同决策参考",
        "输出为区间整数概率（含 <10/>90 等饱和档），非连续概率——系原模型发布口径",
        "即使复发后仍可再获无发作——10 年无发作率高于「从未复发」概率（原文注）",
        "队列年代 1970s–2014、欧美为主；当代诊疗背景下外推需注意",
        "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）",
        "撤药决策须医生主导，本工具不回答「应否撤药」",
    )

    /** JS Math.round（非负数）：floor(x + 0.5) */
    private fun jsRound(x: Double) = floor(x + 0.5)

    /** total → round(×2)/2；索引 = round(total×2)；越界报错（不钳位） */
    private fun lookupRisk(total: Double, table: Array<String>, tableName: String): Pair<Double, String> {
        val rounded = jsRound(total * 2.0) / 2.0
        val idx = jsRound(total * 2.0).toInt()
        require(idx >= 0 && idx < table.size) { "$tableName 总分 $rounded 越界（表长 ${table.size}）" }
        return rounded to table[idx]
    }

    /** 饱和串/整数 → 数值（供 value 字段；显示一律用原串） */
    private fun riskToNumber(s: String): Double = when {
        s.startsWith("<") -> (s.substring(1).toDoubleOrNull() ?: 0.0) / 100.0
        s.startsWith(">") -> (s.substring(1).toDoubleOrNull() ?: 100.0) / 100.0
        else -> (s.toDoubleOrNull() ?: 0.0) / 100.0
    }

    private fun findIdx(vals: IntArray, v: Int) = vals.indexOf(v).also {
        require(it >= 0) { "分值表查无输入值 $v" }
    }

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        // 钳位（原站行为，卡 §3）；整数化（原站 parseInt）
        val ttr = values[0].toInt().coerceIn(0, 24)
        val dur = values[1].toInt().coerceIn(0, 40)
        val age = values[2].toInt().coerceIn(0, 80)
        val naed = values[8].toInt().coerceIn(0, 9)
        val t = EptTables

        // ── 复发模型（8 变量）──
        val ttrIdx = findIdx(t.AED_TTR_VALUES, ttr)
        val durIdx = findIdx(t.AED_DUR_VALUES, dur)
        val ageIdx = findIdx(t.AED_AGE_VALUES, age)
        val eegIdx = values[7].toInt()  // 选项 score=索引 0/1/2
        val recurPts = t.AED_TTR_PTS[ttrIdx] + t.AED_DUR_PTS[durIdx] + t.AED_AGE_PTS[ageIdx] +
            (if (values[3] == 1.0) 3.5 else 0.0) +          // 热惊史
            (if (values[4] == 1.0) 3.0 else 0.0) +          // ≥10 次
            (if (values[5] == 0.0) 5.5 else 0.0) +          // 非自限（否=5.5）
            (if (values[6] == 1.0) 2.0 else 0.0) +          // 发育迟滞
            t.AED_EEG_PTS[eegIdx]                            // 痫样 4（未做=0）

        // ── 长期模型（8 变量）──
        val longPts = t.AED_TTR_PTS2[ttrIdx] + t.AED_DUR_PTS2[durIdx] + t.AED_NAED_PTS2[naed] +
            (if (values[9] == 1.0) 1.5 else 0.0) +          // 女
            (if (values[10] == 1.0) 2.0 else 0.0) +         // 家族史
            (if (values[4] == 1.0) 2.5 else 0.0) +          // ≥10 次
            (if (values[11] == 1.0) 3.0 else 0.0) +         // 局灶
            t.AED_EEG_PTS2[eegIdx]                           // 痫样 2

        val (totalR, r2y) = lookupRisk(recurPts, t.AED_RISK2Y, "RISK2Y")
        val (_, r5y) = lookupRisk(recurPts, t.AED_RISK5Y, "RISK5Y")
        val (totalL, rLong) = lookupRisk(longPts, t.AED_RISK_LONG, "RISK_LONG")

        return AssessmentResult(
            value = riskToNumber(r2y),
            displayValue = "$r2y%",
            bandLabel = "2 年复发风险",
            bandExplanation = "撤药后 2 年发作复发风险（查表整数口径，含饱和档）。总分 $totalR（复发模型）/ $totalL（长期模型）。",
            valueLabel = "撤药后 2 年复发（查表整数口径）",
            extraOutputs = listOf("5 年复发风险" to "$r5y%", "10 年无发作概率" to "$rLong%"),
            details = listOf(
                "模型" to "Lamberink 2017 AED 撤药（PMID 28483337，Lancet Neurol）",
                "性能" to "C-stat 复发 0.65（0.65–0.66）/ 长期 0.71（0.70–0.71）；IECV 跨队列稳定；双模型校准图拟合良好",
                "队列" to "IPD meta 10 队列 1769 例（儿童+成人，随访中位 5 年）",
                "输出口径" to "风险表整数值/饱和串原样显示，非连续概率（原站发布口径）",
                "适用人群" to "药物缓解期考虑撤药者（儿童+成人）；术后缓解者转 TTS",
            ),
        )
    }
}
