package com.hermes.medmodels.model

/**
 * Li2026 儿童重症 RSV 感染风险预测模型。
 *
 * 来源：Li Wanyi et al. J Inflamm Res 2026;19:1652-; DOI 10.2147/JIR.S580432; PMID 42007299。
 * 算法：6 变量 Logistic（Table 4 收缩后系数，Bootstrap 校准斜率 0.975 全局收缩）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01b-模型卡-Li2026
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3 节公式 = Li2026Test 锚点。
 * ⚠️ 文案纪律：禁止单变量解读 NLR（条件效应为正、组间中位更低，Simpson 现象）。
 */
object Li2026RsvSevere : AssessmentModel {

    // ===== 冻结参数（2026-08-24，模型卡第 3 节，收缩后） =====
    private const val INTERCEPT = -2.934
    private const val B_NLR = 0.191
    private const val B_ILLNESS_DAYS = -0.007
    private const val B_AGE_MONTHS = -0.047
    private const val B_WINTER = 0.975
    private const val B_HYPOXEMIA = 2.676
    private const val B_PRETERM = 0.910

    // ===== 工程自定风险带（原文无截断，模型卡第 3 节留痕） =====
    const val BAND_LOW_UPPER = 0.03   // <3% 低危
    const val BAND_MID_UPPER = 0.20   // 3–20% 中危；≥20% 高危

    override val id = "li2026_rsv_severe"
    override val displayName = "Li2026 · 儿童重症 RSV 感染风险预测（住院婴幼儿）"
    override val category = "感染相关"
    override val summary = "住院 RSV 患儿的 6 变量 Logistic 模型（AUC 0.847，Elastic Net+收缩，单中心内部验证）"

    override val inputs = listOf(
        ModelInput("nlr", "NLR", ModelInput.InputType.CONTINUOUS, 0.0, 30.0, "", "中性粒/淋巴细胞比值（血常规）"),
        ModelInput("illnessDays", "病程", ModelInput.InputType.CONTINUOUS, 1.0, 60.0, "天", "起病至入院天数"),
        ModelInput("ageMonths", "月龄", ModelInput.InputType.CONTINUOUS, 0.0, 216.0, "月", "年龄（月）——勿填岁"),
        ModelInput("winterOnset", "冬季发病", ModelInput.InputType.BINARY, 0.0, 1.0, "", "发病于 RSV 流行季（当地冬季）"),
        ModelInput("hypoxemia", "低氧血症", ModelInput.InputType.BINARY, 0.0, 1.0, "", "血氧下降/低氧血症"),
        ModelInput("preterm", "早产", ModelInput.InputType.BINARY, 0.0, 1.0, "", "早产史"),
    )

    override val disclaimers = listOf(
        "仅限住院确诊 RSV 患儿（咽拭子抗原/核酸阳性），且为基础健康儿童",
        "不适用于先心病、BPD、先天气道畸形、严重神经疾病、免疫缺陷患儿（原文设计即排除）",
        "风险分带（<3% 低 / 3–20% 中 / ≥20% 高）为工程自定，原文未提供截断值与操作点性能",
        "单中心（天津）回顾性内部验证，概率跨地区/季节外推需谨慎",
        "重症结局为 Wang 毛细支气管炎严重度评分 ≥9 分；本工具不能替代临床判断",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val nlr = values[0]; val days = values[1]; val ageM = values[2]
        val winter = values[3]; val hypox = values[4]; val preterm = values[5]

        val logit = INTERCEPT +
            B_NLR * nlr +
            B_ILLNESS_DAYS * days +
            B_AGE_MONTHS * ageM +
            B_WINTER * winter +
            B_HYPOXEMIA * hypox +
            B_PRETERM * preterm
        val p = 1.0 / (1.0 + Math.exp(-logit))

        val band = when {
            p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 3%。按常规路径管理，持续观察病情演变。"
            p < BAND_MID_UPPER -> "中风险" to "预测概率 3–20%。建议加密监测体温与 SpO₂、动态评估病情演变。"
            else -> "高风险" to "预测概率 ≥20%。建议按重症路径管理：加密监测 SpO₂、评估呼吸支持与住院级别，动态复查。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band.first,
            bandExplanation = band.second,
            details = listOf(
                "模型" to "Li2026（PMID 42007299）",
                "风险带" to "<3% 低 / 3–20% 中 / ≥20% 高（工程自定，原文无截断）",
                "证据等级" to "单中心回顾性 + 内部验证（RECORD）",
                "结局定义" to "Wang 毛细支气管炎严重度评分 ≥9",
            ),
        )
    }
}
