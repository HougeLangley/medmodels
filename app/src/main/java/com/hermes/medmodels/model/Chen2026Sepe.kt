package com.hermes.medmodels.model

/**
 * Chen2026 SEPE 癫痫耐药预后分层评分（成人验证队列）。
 *
 * 来源：Chen Xu, Ge Jian-Qing, et al. BMC Neurol 2026;26:183; PMID 41699518; PMC13015163。
 * 算法：3 BINARY + 3 CATEGORICAL 查表求和（0–10 分，0.5 步进）+ 三带判读。
 * 性质：验证研究（TRIPOD 3，506 例外部验证）；分层提示工具，非概率预测器。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01e-模型卡-Chen2026SEPE
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3/4 节 = Chen2026SepeTest 锚点（T1–T12）。
 * ⚠️ 性能白名单硬约束：只准引用 AUC 0.68 / ≥6 带 91.2% DRE / ≤4 带 89.4% 药物反应，其余数字禁现。
 */
object Chen2026Sepe : AssessmentModel {

    // ===== 冻结参数（2026-08-24，模型卡第 4 节）=====
    const val BAND_GOOD_MAX = 4.0   // ≤4 预后良好带（闭区间）
    const val BAND_HIGH_MIN = 6.0   // ≥6 高耐药风险带（闭区间）；4<分<6 中间带

    override val id = "chen2026_sepe"
    override val displayName = "Chen2026 · SEPE 癫痫耐药预后评分（成人验证队列）"
    override val category = "神经系统"
    override val summary = "成人为主混合队列（506 例）验证的 6 项预后分层量表（AUC 0.68；分层工具，非概率预测）"

    override val audienceNotice = "验证人群为成人为主的混合队列（0.1–81 岁）；儿童患者建议优先使用 Geng2022 / Boonluksiri2015"

    private val sleepOptions = listOf(
        CategoryOption("none", "无发作", 0.0),
        CategoryOption("sleepOnly", "仅睡眠期", 1.0),
        CategoryOption("wakeOnly", "仅清醒期", 1.0),
        CategoryOption("both", "全程（睡眠+清醒）", 2.0),
    )
    private val typeCountOptions = listOf(
        CategoryOption("none", "无", 0.0),
        CategoryOption("one", "一种", 1.0),
        CategoryOption("two", "两种", 2.0),
        CategoryOption("threePlus", "≥三种", 3.0),
    )
    private val freqOptions = listOf(
        CategoryOption("none", "无发作", 0.0),
        CategoryOption("initial", "初诊即评估", 0.5),
        CategoryOption("gt2mo", "≥2 月一次", 1.0),
        CategoryOption("lt2mo", "2 月内 ≥1 次", 2.0),
    )

    override val inputs = listOf(
        ModelInput("familyHistory", "癫痫家族史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "一级亲属癫痫史"),
        ModelInput("intellectualDisability", "智力障碍", ModelInput.InputType.BINARY, 0.0, 1.0, "", "智力障碍诊断"),
        ModelInput(
            key = "sleepWakeCycle", label = "睡眠-觉醒周期分布", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 2.0, unit = "", hint = "发作在睡眠/觉醒周期的分布",
            options = sleepOptions,
        ),
        ModelInput(
            key = "seizureTypeCount", label = "发作类型数", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 3.0, unit = "", hint = "ILAE 发作类型种数",
            options = typeCountOptions,
        ),
        ModelInput(
            key = "seizureFrequency", label = "发作频率", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 2.0, unit = "", hint = "初诊即评估计 0.5 分（0.5 步进来源）",
            options = freqOptions,
        ),
        ModelInput("mriAbnormal", "头颅 MRI", ModelInput.InputType.BINARY, 0.0, 1.0, "", "异常（含可疑）"),
    )

    override val disclaimers = listOf(
        "适用：成人癫痫患者（主要目标）与大龄青少年；儿童患者建议优先使用 Geng2022 / Boonluksiri2015",
        "不适用：急性症状性发作（代谢紊乱/中毒/外伤/感染相关）、热性惊厥、获得性结构病变继发癫痫（肿瘤/卒中/脑炎/颅脑外伤后）、已确诊 DRE 者",
        "验证队列为成人为主的混合年龄住院队列（0.1–81 岁，南京脑科医院 506 例，1–2 年随访）",
        "量表 6 项中 3 项（家族史/智力障碍/MRI）在该队列多因素分析中未达显著",
        "量表开发于 2021 年（原团队自建），本文为其外部验证；中间带（4<分<6）判别价值有限",
        "本工具为分层提示工具，非概率预测器；不输出连续概率，不构成治疗方案依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val family = values[0]; val id = values[1]; val sleep = values[2]
        val typeCount = values[3]; val freq = values[4]; val mri = values[5]
        val total = family + id + sleep + typeCount + freq + mri

        val band: String
        val explanation: String
        when {
            total <= BAND_GOOD_MAX -> {
                band = "预后良好倾向"
                explanation = "总分 ≤4。验证队列中 89.4% 药物反应；仍需规范随访。"
            }
            total < BAND_HIGH_MIN -> {
                band = "中间带"
                explanation = "总分 4–6 之间。判别价值有限（验证队列中约 1/3 为 DRE、过半药物反应）；建议结合专科综合判断。"
            }
            else -> {
                band = "高耐药风险"
                explanation = "总分 ≥6。验证队列中 91.2% 患者符合 DRE 标准；建议专科评估。"
            }
        }
        val fmt = if (total == total.toInt().toDouble()) "%.0f" else "%.1f"
        return AssessmentResult(
            value = total,
            displayValue = String.format(fmt, total) + " 分",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Chen2026 SEPE（PMID 41699518）",
                "判读" to "≤4 预后良好 | 4–6 中间带 | ≥6 高耐药风险",
                "AUC" to "0.68（全队列 0.6827 / 非手术 0.6812）",
                "证据等级" to "外部验证（TRIPOD 3，506 例）",
                "适用人群" to "成人为主混合队列（0.1–81 岁）",
            ),
        )
    }
}
