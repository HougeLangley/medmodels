package com.hermes.medmodels.model

/**
 * Guo2026 ICU 脓毒症双列线图（模型 A：新冠脓毒症 / 模型 B：非新冠脓毒症）。
 *
 * 来源：Guo M, Shu L, … He Z. Sci Rep 2026 (AIP); DOI 10.1038/s41598-026-58450-w; PMID 42298051。
 * 系数来源：Additional file 2 Tables S4/S7 直印（Springer 补材）；外部验证 MIMIC-IV。
 * 一卡双模型（01h）：本文件承载两个 AssessmentModel 条目，共享门类「重症/ICU」。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01h-模型卡-Guo2026脓毒症
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3/4 节 = Guo2026SepsisTest 锚点（A-T1~T7 / B-T1~T7 / T15）。
 * ⚠️ 硬约束：原文未报分带死亡率 → 结果页不得出现任何「组内死亡率」行（与 4C 不同源，禁止仿写）。
 */

/** 共享常量与横幅（两模型同门类同场景） */
private const val ICU_CATEGORY = "重症/ICU"
private const val ICU_AUDIENCE_NOTICE =
    "ICU 脓毒症成人患者（确诊后 24h 内评估）；需血气（乳酸/氧合指数）与凝血实验室数据；外部验证人群死亡率谱较轻，绝对概率向高估方向漂移"
private const val BAND_LOW_UPPER = 0.10
private const val BAND_HIGH_LOWER = 0.40

private fun sepsisBand(p: Double): Pair<String, String> = when {
    p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 10%（工程划定低危带，锚 ICU 死亡率谱）。"
    p <= BAND_HIGH_LOWER -> "中风险" to "预测概率 10–40%。建议强化监测与感染源控制评估。"
    else -> "高风险" to "预测概率 >40%。建议按高死亡风险路径管理：强化器官支持与治疗方案复核。"
}

private fun sepsisResult(
    p: Double, modelId: String, isCovid: Boolean,
    internalAuc: String, externalAuc: String, extraNotes: List<Pair<String, String>>,
): AssessmentResult {
    val (band, explanation) = sepsisBand(p)
    return AssessmentResult(
        value = p,
        displayValue = String.format("%.1f", p * 100) + "%",
        bandLabel = band,
        bandExplanation = explanation,
        details = buildList {
            add("模型" to "Guo2026${if (isCovid) " 新冠脓毒症" else " 非新冠脓毒症"}（PMID 42298051）")
            add("证据等级" to "外部验证（MIMIC-IV）：$externalAuc；内部 $internalAuc")
            add("风险带" to "<10% 低 / 10–40% 中 / >40% 高（工程划定，原文未报分带死亡率）")
            addAll(extraNotes)
            add("适用人群" to "ICU 成人脓毒症（Sepsis-3），确诊 24h 内评估")
        },
    )
}

/** 模型 A：ICU 新冠脓毒症（6 CONTINUOUS） */
object Guo2026SepsisCovid : AssessmentModel {
    private const val INTERCEPT = -6.394
    private const val B_AGE = 0.035
    private const val B_HR = 0.056
    private const val B_CRP = 0.010
    private const val B_LYM = -0.424
    private const val B_LAC = 1.221
    private const val B_OI = -0.011

    override val id = "guo2026_sepsis_covid"
    override val displayName = "Guo2026 · ICU 新冠脓毒症死亡风险（ICU 成人·确诊 24h 内）"
    override val category = ICU_CATEGORY
    override val summary = "ICU 新冠脓毒症 6 变量 Logistic（AUC 0.938→外部 0.841，MIMIC-IV 外部验证）"
    override val audienceNotice = ICU_AUDIENCE_NOTICE

    override val inputs = listOf(
        ModelInput("age", "年龄", ModelInput.InputType.CONTINUOUS, 18.0, 110.0, "岁", "成人（≥18）"),
        ModelInput("hr", "心率", ModelInput.InputType.CONTINUOUS, 30.0, 220.0, "bpm", "ICU 监测心率"),
        ModelInput("crp", "C 反应蛋白", ModelInput.InputType.CONTINUOUS, 0.0, 500.0, "mg/L", "CRP，单位 mg/L"),
        ModelInput("lym", "淋巴细胞比例", ModelInput.InputType.CONTINUOUS, 1.0, 40.0, "%", "淋巴细胞百分比"),
        ModelInput("lac", "乳酸", ModelInput.InputType.CONTINUOUS, 0.1, 20.0, "mmol/L", "动脉或静脉血气乳酸，mmol/L"),
        ModelInput("oi", "氧合指数", ModelInput.InputType.CONTINUOUS, 50.0, 500.0, "mmHg", "氧合指数=PaO₂/FiO₂（mmHg），需动脉血气"),
    )

    override val disclaimers = listOf(
        "适用：ICU 内成人新冠脓毒症（Sepsis-3 + SARS-CoV-2 阳性），确诊 24h 内评估的院内死亡风险",
        "不适用：儿童；门诊；非 ICU 住院；免疫抑制/血液病/终末期肿瘤/妊娠（原队列排除组）；非脓毒症感染",
        "外部验证人群（MIMIC-IV）死亡率谱较轻（39.4% vs 内部 56.3%）——绝对概率在轻谱人群向高估漂移",
        "内部 AUC 0.938 高于 AHA 过拟合警戒线；外部 0.841（衰减 0.097），optimism-corrected 0.913",
        "原文未报告分带死亡率，本工具不提供组内死亡率判读",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_AGE * values[0] + B_HR * values[1] + B_CRP * values[2] +
            B_LYM * values[3] + B_LAC * values[4] + B_OI * values[5]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        return sepsisResult(p, id, isCovid = true,
            internalAuc = "0.938", externalAuc = "0.841",
            extraNotes = listOf("对照" to "外部 AUC 0.841 vs SOFA 0.693 / APACHE II 0.753"))
    }
}

/** 模型 B：ICU 非新冠脓毒症（8 CONTINUOUS） */
object Guo2026SepsisNonCovid : AssessmentModel {
    private const val INTERCEPT = -28.512
    private const val B_AGE = 0.091
    private const val B_HR = 0.027
    private const val B_K = 0.483
    private const val B_NA = 0.078
    private const val B_LDH = 0.002
    private const val B_PT = 0.212
    private const val B_DDIMER = 0.067
    private const val B_LAC = 0.715

    override val id = "guo2026_sepsis_noncovid"
    override val displayName = "Guo2026 · ICU 非新冠脓毒症死亡风险（ICU 成人·确诊 24h 内）"
    override val category = ICU_CATEGORY
    override val summary = "ICU 非新冠脓毒症 8 变量 Logistic（AUC 0.871→外部 0.859，MIMIC-IV 外部验证）"
    override val audienceNotice = ICU_AUDIENCE_NOTICE

    override val inputs = listOf(
        ModelInput("age", "年龄", ModelInput.InputType.CONTINUOUS, 18.0, 110.0, "岁", "成人（≥18）"),
        ModelInput("hr", "心率", ModelInput.InputType.CONTINUOUS, 30.0, 220.0, "bpm", "ICU 监测心率"),
        ModelInput("k", "血钾", ModelInput.InputType.CONTINUOUS, 2.0, 8.0, "mmol/L", "血清钾（一位小数）；原文该项 P=0.057 未达显著"),
        ModelInput("na", "血钠", ModelInput.InputType.CONTINUOUS, 110.0, 165.0, "mmol/L", "血清钠（一位小数）"),
        ModelInput("ldh", "乳酸脱氢酶", ModelInput.InputType.CONTINUOUS, 50.0, 5000.0, "U/L", "LDH"),
        ModelInput("pt", "凝血酶原时间", ModelInput.InputType.CONTINUOUS, 8.0, 40.0, "秒", "PT 秒数"),
        ModelInput("ddimer", "D-二聚体", ModelInput.InputType.CONTINUOUS, 0.01, 100.0, "mg/L", "D-dimer，单位 mg/L（FEU）"),
        ModelInput("lac", "乳酸", ModelInput.InputType.CONTINUOUS, 0.1, 20.0, "mmol/L", "动脉或静脉血气乳酸，mmol/L"),
    )

    override val disclaimers = listOf(
        "适用：ICU 内成人非新冠脓毒症（Sepsis-3，SARS-CoV-2 阴性或无新冠），确诊 24h 内评估的院内死亡风险",
        "不适用：儿童；门诊；非 ICU 住院；免疫抑制/血液病/终末期肿瘤/妊娠（原队列排除组）；非脓毒症感染",
        "外部验证人群（MIMIC-IV）死亡率谱较轻（29.1% vs 内部 40.7%）——绝对概率在轻谱人群向高估漂移",
        "血钾项原文 P=0.057 未达统计显著（保留系忠实原文），解释应保守",
        "截距 −28.512 极深负（由血钠/年龄大项抵消）——输入值域经防呆校验，勿手工换算",
        "原文未报告分带死亡率，本工具不提供组内死亡率判读",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_AGE * values[0] + B_HR * values[1] + B_K * values[2] +
            B_NA * values[3] + B_LDH * values[4] + B_PT * values[5] + B_DDIMER * values[6] + B_LAC * values[7]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        return sepsisResult(p, id, isCovid = false,
            internalAuc = "0.871", externalAuc = "0.859",
            extraNotes = listOf("对照" to "外部 AUC 0.859 vs SOFA 0.705 / APACHE II 0.739"))
    }
}
