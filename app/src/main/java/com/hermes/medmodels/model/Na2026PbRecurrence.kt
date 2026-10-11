package com.hermes.medmodels.model

/**
 * Na2026 MPP 塑形性支气管炎（PB）取栓后复发风险预测。
 *
 * 来源：Na F, Li W, … Liu J. Front Pediatr 2026;14:1959849; PMID 42780927; PMC13597871（CC BY）。
 * 算法：1 CONTINUOUS + 3 BINARY Logistic（原文 Table 3 β 直印；截距工程重建）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01i-模型卡-Na2026PB复发
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3/4 节 = Na2026Test 锚点（T1–T13）。
 * ⚠️ 截距 5.19 为工程重建（±0.3，患病率匹配 MLE 四法交叉）——结果页永久声明行；解读以序贯比较为主。
 * ⚠️ 禁止实现为评分制（列线图整数分值与 β 不成比例，R 渲染取整）——只做连续概率。
 */
object Na2026PbRecurrence : AssessmentModel {

    // ===== 冻结参数（2026-10-10，模型卡第 3/4 节，D11a 冻结版）=====
    private const val INTERCEPT = 5.19          // 工程重建（±0.3），非原文直印
    private const val B_ALBUMIN = -0.18754
    private const val B_FEVER72 = 2.3613
    private const val B_ATELECTASIS = 1.3417
    private const val B_CASTS2LOBES = 1.1727

    // 风险带（工程自定，锚=训练患病率 30.25%；原文无预设操作切点）
    const val BAND_LOW_UPPER = 0.15   // <15% 低危
    const val BAND_HIGH_LOWER = 0.45  // >45% 高危；15–45% 中危

    override val id = "na2026_pb_recurrence"
    override val displayName = "Na2026 · PB 取栓后复发风险（儿童 MPP·住院）"
    override val category = "呼吸系统"
    override val summary = "MPP 并发 PB 首次取栓后复发预测（AUC 0.853/0.833/外部 0.811，双中心）"

    override val audienceNotice =
        "住院 MPP 并发 PB 且已行首次支气管镜取栓的儿童（中位 ~7.7 岁）；变量取首次支气管镜时点"

    override val inputs = listOf(
        ModelInput("albumin", "血清白蛋白", ModelInput.InputType.CONTINUOUS, 20.0, 55.0, "g/L",
            "首次 BAL 前后血清白蛋白，单位 g/L（国内检验常规，勿填 g/dL）"),
        ModelInput("fever72", "首次 BAL 后发热≥72h", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "首次灌洗后持续发热≥72 小时（时点：首次 BAL 之后）"),
        ModelInput("atelectasis", "肺不张", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院胸片/CT 所见肺不张"),
        ModelInput("casts2lobes", "支气管铸型≥2 叶", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "首次支气管镜所见铸型累及≥2 个肺叶"),
    )

    override val disclaimers = listOf(
        "适用：MPP+PB 首次取栓后的复发风险分层（决定随访强度/复镜警觉）",
        "慎用：非 MPP 病因的 PB（淋巴漏/先心术后）；MPP 未并发 PB 者——本模型不预测 PB 发生，预测 NP 用 Li2025 模型",
        "不适用：成人；门诊患儿；合并先心/淋巴引流异常/混合感染（原文排除标准）",
        "截距为工程重建（±0.3 不确定度）：中段概率受传播影响可达 ±5–8pp——解读以序贯比较为主（较基线抬升/下降），绝对概率供参考",
        "风险带为工程参照线（锚=训练患病率 30.25%），非原文临床决策阈值",
        "外部队列仅 44 例（AUC CI 0.661–0.962 宽）；无前瞻验证；被引 0（2026-09 上线）",
        "结局确认依赖临床指征复镜——轻度复发可能漏检（回顾性固有偏倚）",
        "ALB 单变量切点 41.42 g/L（Se 85.88%/Sp 58.16%/AUC 0.776）备查",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_ALBUMIN * values[0] + B_FEVER72 * values[1] +
            B_ATELECTASIS * values[2] + B_CASTS2LOBES * values[3]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        val (band, explanation) = when {
            p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 15%（工程参照线，锚=训练患病率 30.25%）。"
            p <= BAND_HIGH_LOWER -> "中风险" to "预测概率 15–45%。建议加强随访与复镜警觉。"
            else -> "高风险" to "预测概率 >45%。建议提高随访强度、缩短复镜评估间隔。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Na2026 PB 复发预测（PMID 42780927）",
                "性能" to "AUC 训练 0.853 / 内部 0.833 / 外部 0.811（独立中心 44 例）；HL 三队列 P=0.747/0.499/0.693",
                "截距声明" to "截距 5.19 为工程重建（±0.3，患病率匹配四法交叉），非原文直印——绝对概率供参考，解读以序贯比较为主",
                "风险带" to "<15% 低 / 15–45% 中 / >45% 高（工程参照线，非原文操作点）",
                "适用人群" to "住院 MPP 并发 PB 已行首次取栓的儿童；变量取首次支气管镜时点",
            ),
        )
    }
}
