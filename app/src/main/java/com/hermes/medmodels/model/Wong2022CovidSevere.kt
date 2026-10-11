package com.hermes.medmodels.model

/**
 * Wong2022 儿童重症/危重 COVID-19 风险预测（PACCOVRA）。
 *
 * 来源：Wong JJM, Abbas Q, … Lee JH. PLOS ONE 2022;17(10):e0275761; PMID 36301941; PMC9612577。
 * 算法：6 BINARY + 3 CONTINUOUS Logistic 概率模型（原文 Table 3 全参数直印，含截距）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01f-模型卡-Wong2022
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3/4 节 = Wong2022Test 锚点（T1–T13）。
 * ⚠️ 定位：高特异度分层工具（LR+ 9.0）——阳性显著抬升重症概率，阴性不能排除（Se 53.2%）。
 * ⚠️ D5a + D7c：显示名人群+时代双标注；audienceNotice 三处显示。
 */
object Wong2022CovidSevere : AssessmentModel {

    // ===== 冻结参数（2026-08-24，模型卡第 3/4 节，D7 冻结版）=====
    private const val INTERCEPT = 4.29
    private const val B_INFANT = 2.18
    private const val B_COMORBIDITY = 1.87
    private const val B_SEIZURES = 3.34
    private const val B_VOMITING = 1.80
    private const val B_FEVER = 1.19
    private const val B_CORYZA = -1.77
    private const val B_ANC = 0.07
    private const val B_HB = -0.57
    private const val B_PLT = -0.01

    // 风险带（D7a，与 Li2026 统一；锚=训练患病率 13.6%）
    const val BAND_LOW_UPPER = 0.03   // <3% 低危
    const val BAND_HIGH_LOWER = 0.20  // >20% 高危；3–20% 中危

    override val id = "wong2022_covid_severe"
    override val displayName = "Wong2022 · 儿童重症/危重新冠风险预测（住院患儿·2020–21 毒株时代）"
    override val category = "感染相关"
    override val summary = "住院患儿 9 变量 Logistic 模型（AUC 0.96/0.92，五中心四国，随机分割内部验证）"

    override val audienceNotice = "验证人群为 2020–21（野生型→Delta 时代）亚洲住院儿童；当代流行株时代的适用性需临床判断"

    override val inputs = listOf(
        ModelInput("infant", "婴儿（<1 岁）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "年龄小于 1 岁"),
        ModelInput("comorbidity", "合并症（任何）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "任何基础合并症"),
        ModelInput("seizures", "惊厥（入院时症状）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "入院时惊厥症状——非癫痫病史"),
        ModelInput("vomiting", "呕吐", ModelInput.InputType.BINARY, 0.0, 1.0, "", "入院时呕吐症状"),
        ModelInput("fever", "发热", ModelInput.InputType.BINARY, 0.0, 1.0, "", "入院时发热"),
        ModelInput("coryza", "卡他症状（鼻部）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "鼻部卡他症状（保护性因素）"),
        ModelInput("anc", "中性粒细胞绝对值", ModelInput.InputType.CONTINUOUS, 0.0, 50.0, "×10⁹/L", "血常规 ANC，单位 ×10⁹/L"),
        ModelInput("hemoglobin", "血红蛋白", ModelInput.InputType.CONTINUOUS, 3.0, 20.0, "g/dL", "血常规 Hb，单位 g/dL（勿填 g/L）"),
        ModelInput("platelets", "血小板", ModelInput.InputType.CONTINUOUS, 5.0, 800.0, "×10⁹/L", "血常规 PLT，单位 ×10⁹/L"),
    )

    override val disclaimers = listOf(
        "适用：住院确诊 COVID-19 / MIS-C 儿童的重症风险分层（入院早期，血常规回报后）",
        "慎用：门诊轻症；当代 Omicron + 疫苗普及人群（重症谱已变，校准必然漂移）",
        "不适用：成人；非 COVID 感染；起病已久的住院后期患者",
        "本模型为高特异度分层工具：阳性（高危）显著抬升重症概率（LR+ 9.0）；阴性/低危不能排除重症（操作点 Se 53.2%）",
        "无外部验证（随机分割内部验证，作者自认需进一步验证）；操作点阈值原文未印，风险带为工程自定",
        "队列含 MIS-C 3.1%（剔除后 AUC 不变）；站间性能不均（部分中心 AUC 0.79）",
        "训练 AUC 0.96 高于 AHA 2026 过拟合警戒线（验证 0.92 未衰减）；惊厥为入院症状非癫痫史",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val infant = values[0]; val comorb = values[1]; val seiz = values[2]
        val vomit = values[3]; val fever = values[4]; val coryza = values[5]
        val anc = values[6]; val hb = values[7]; val plt = values[8]

        val logit = INTERCEPT +
            B_INFANT * infant + B_COMORBIDITY * comorb + B_SEIZURES * seiz +
            B_VOMITING * vomit + B_FEVER * fever + B_CORYZA * coryza +
            B_ANC * anc + B_HB * hb + B_PLT * plt
        val p = 1.0 / (1.0 + Math.exp(-logit))

        val band = when {
            p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 3%（工程划定低危带）。"
            p <= BAND_HIGH_LOWER -> "中风险" to "预测概率 3–20%。建议密切监测病情演变。"
            else -> "高风险" to "预测概率 >20%。阳性结果显著抬升重症概率（LR+ 9.0），建议按重症路径管理。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band.first,
            bandExplanation = band.second + "注意：本模型为高特异度工具，低危不能排除重症（Se 53.2%）。",
            details = listOf(
                "模型" to "Wong2022（PMID 36301941）",
                "证据等级" to "内部验证（随机分割，TRIPOD 2a）：AUC 训练 0.96 / 验证 0.92",
                "风险带" to "<3% 低 / 3–20% 中 / >20% 高（工程划定）",
                "操作点性能" to "Se 53.2% / Sp 94.1% / LR+ 9.0（阈值原文未印）",
                "适用人群" to "2020–21 亚洲住院儿童（野生型→Delta 时代）",
            ),
        )
    }
}
