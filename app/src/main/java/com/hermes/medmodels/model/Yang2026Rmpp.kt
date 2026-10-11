package com.hermes.medmodels.model

/**
 * Yang2026 RMPP 风险预测模型。
 *
 * 来源：Yang J, Yan X. J Vis Exp 2026; DOI 10.3791/71588; PMID 42612067。
 * 算法：6 变量 Logistic 回归（Table 2 β 系数 + worked-example 校准截距）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01a-模型卡-Yang2026
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3 节公式 = Yang2026Test 锚点。
 * 改任何一处必须三处同步，先改模型卡留痕。
 */
object Yang2026Rmpp : AssessmentModel {

    // ===== 冻结参数（2026-08-23，模型卡第 3 节）=====
    private const val INTERCEPT = -40.87
    private const val B_FEVER_DAYS = 0.21
    private const val B_PEAK_TEMP = 0.906
    private const val B_LDH = 0.009
    private const val B_SPUTUM_EFFUSION = 1.244
    private const val B_CONSOLIDATION = 1.029
    private const val B_HYPOXEMIA = 2.034

    /** 训练集 Youden 最优截断（默认） */
    const val CUTOFF_TRAIN = 0.222

    /** 验证集最优截断（备选） */
    const val CUTOFF_VALID = 0.247

    override val id = "yang2026_rmpp"
    override val displayName = "Yang2026 · 儿童难治性支原体肺炎（RMPP）风险预测（住院儿童）"
    override val category = "呼吸系统"
    override val summary = "住院 MPP 患儿的 6 变量 Logistic 模型（AUC 0.840/0.831，单中心内部验证）"

    override val inputs = listOf(
        ModelInput("feverDays", "入院前热程", ModelInput.InputType.CONTINUOUS, 0.0, 14.0, "天", "大环内酯治疗前发热天数"),
        ModelInput("peakTempC", "峰值体温", ModelInput.InputType.CONTINUOUS, 36.0, 42.0, "℃", "本次病程最高体温"),
        ModelInput("ldh", "血清 LDH", ModelInput.InputType.CONTINUOUS, 0.0, 3000.0, "U/L", "乳酸脱氢酶"),
        ModelInput("sputumEffusion", "痰栓或胸腔积液", ModelInput.InputType.BINARY, 0.0, 1.0, "", "胸部 CT 示痰栓或胸腔积液"),
        ModelInput("consolidation", "肺实变", ModelInput.InputType.BINARY, 0.0, 1.0, "", "胸部 CT 示肺实变"),
        ModelInput("hypoxemia", "低氧血症", ModelInput.InputType.BINARY, 0.0, 1.0, "", "血氧下降/低氧血症"),
    )

    override val disclaimers = listOf(
        "适用人群：住院肺炎支原体肺炎（MPP）患儿；不适用于门诊轻症与成人",
        "依赖胸部 CT 判读与 LDH 化验结果，无 CT 场景不适用",
        "截断值 0.222 基于单中心（金华）RMPP 患病率 25.8% 背景，跨地区/季节概率会失真",
        "本模型为单中心回顾性研究（TRIPOD 2a，内部验证），未达临床指南推荐级别，仅供临床参考与研究使用",
        "预测的是「未来将符合 RMPP 定义」的概率，不能替代临床判断",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val feverDays = values[0]; val peakTemp = values[1]; val ldh = values[2]
        val sputum = values[3]; val consol = values[4]; val hypox = values[5]

        val logit = INTERCEPT +
            B_FEVER_DAYS * feverDays +
            B_PEAK_TEMP * peakTemp +
            B_LDH * ldh +
            B_SPUTUM_EFFUSION * sputum +
            B_CONSOLIDATION * consol +
            B_HYPOXEMIA * hypox
        val p = 1.0 / (1.0 + Math.exp(-logit))

        val high = p >= CUTOFF_TRAIN
        return AssessmentResult(
            value = p,
            displayValue = "${(p * 100).let { if (it > 99.95) 99.95 else it }.let { String.format("%.1f", it) }}%",
            bandLabel = if (high) "高风险" else "低风险",
            bandExplanation = if (high)
                "预测概率 ≥ 截断值 $CUTOFF_TRAIN（训练集 Youden 最优，敏感度 78.7% / 特异度 80.1%）。建议加密监测体温与 SpO₂，复查影像，提前评估抗微生物升级与免疫调节时机。"
            else
                "预测概率 < 截断值 $CUTOFF_TRAIN。按常规路径管理，持续观察病情演变。",
            details = listOf(
                "模型" to "Yang2026（PMID 42612067）",
                "截断" to "$CUTOFF_TRAIN（训练集）/ $CUTOFF_VALID（验证集可选）",
                "证据等级" to "单中心回顾性 + 内部验证（TRIPOD 2a）",
            ),
        )
    }
}
