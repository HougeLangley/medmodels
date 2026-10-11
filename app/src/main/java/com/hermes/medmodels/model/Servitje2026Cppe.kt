package com.hermes.medmodels.model

/**
 * Servitje-Verdaguer 2026 cPPE（儿童肺炎旁胸腔积液进展复杂化预测）。
 *
 * 来源：Servitje-Verdaguer S, et al. Pediatr Pulmonol 2026;61(9):e71817; PMID 42669040; PMC13526271。
 * 算法：2 CONTINUOUS + 3 BINARY Logistic——项目首个零工程重建模型（方程含截距原文直印，无任何反推）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01k-模型卡-Servitje2026cPPE
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3 节 = Servitje2026Test 锚点（T1–T12）。
 * ⚠️ 床旁评分表只做「速评参照」（bedsideScore 供展示与测试）——禁止把评分带做成结果输出，
 *    概率一律出自连续公式（卡 §3.2 禁用理由：五个 γ 不一致，同分值带内连续概率跨度大）。
 * ⚠️ CRP 是开关不是连续输入——「≥176.5 mg/L 与否」，防误输原始 CRP 值。
 */
object Servitje2026Cppe : AssessmentModel {

    // ===== 冻结参数（2026-10-11，模型卡 §3.1，原文直印零重建）=====
    private const val INTERCEPT = -0.4729
    private const val B_AGE = -0.3122
    private const val B_RETRACTIONS = 1.1711
    private const val B_CRPHIGH = 1.0399
    private const val B_THICKNESS = 0.7719
    private const val B_MEDIASTINAL_SHIFT = 2.3751

    // 操作点（原文 Youden；结果页引用）
    private const val OPERATING_POINT = 0.49

    override val id = "servitje2026_cppe"
    override val displayName = "cPPE 复杂化预测（儿童 PPE）"
    override val category = "呼吸系统"
    override val summary = "Servitje2026 · 入院时点 5 变量 Logistic（AUC 0.883，方程零重建；Pediatr Pulmonol）"

    override val audienceNotice =
        "儿童肺炎旁胸腔积液住院患者（训练患病率 63.1%）；变量取入院表现时点（体格/CRP/床旁超声/胸片）"

    override val inputs = listOf(
        ModelInput("age", "年龄", ModelInput.InputType.CONTINUOUS, 0.0, 18.0, "岁",
            "岁（训练队列 0–18 岁；每岁 OR 0.732——年龄越小风险越高）"),
        ModelInput("retractions", "胸壁凹陷", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院体格检查所见胸壁凹陷（呼吸三凹征象）"),
        ModelInput("crpHigh", "CRP≥176.5 mg/L", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院首次 CRP 是否≥176.5 mg/L（切点为原文 Youden 最优；不输入原始 CRP 值）",
            negativeLabel = "否", positiveLabel = "是"),
        ModelInput("thicknessCm", "积液厚度", ModelInput.InputType.CONTINUOUS, 0.0, 10.0, "cm",
            "入院床旁超声测量的积液厚度，单位 cm"),
        ModelInput("mediastinalShift", "纵隔移位", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院胸片所见纵隔移位"),
    )

    override val disclaimers = listOf(
        "适用：儿童肺炎旁胸腔积液住院患者的 cPPE（需引流/手术或坏死/分隔）风险分层，入院表现时点评估",
        "本模型来自单中心回顾性研究（西班牙，122 例），未经外部验证，原作者自认性能可能偏乐观",
        "原文未报告标准校准分析（无 Hosmer-Lemeshow、校准曲线、bootstrap）；仅报告简化评分四风险组的实测率梯度（4.4%→55.9%→81.8%→93.8%）作为弱式校准对照",
        "队列跨 2008–2023 年，微生物学流行、影像实践与治疗策略的时代变迁可能影响外推性",
        "纵隔移位估计基于较少事件（OR 10.75，CI 上界 51.6），不稳定",
        "所据全文为 PMC 作者手稿版（submittedVersion）；Wiley 刊出版在付费墙内，两版若有出入以刊出版为准",
        "床旁评分表（0–43 分四带）仅作速评参照——分值 γ 与 β 不成比例，同带内连续概率跨度大，概率一律以连续公式为准",
        "预测结果不构成治疗决策依据",
    )

    /** 床旁评分（卡 §3.2）——仅展示参照与测试用，禁止作为结果输出 */
    fun bedsideScore(values: List<Double>): Int {
        val (age, retr, crp, thick, shift) = values
        val ageScore = when {
            age < 4 -> 15; age < 7 -> 12; age < 10 -> 9
            age < 13 -> 6; age < 16 -> 3; else -> 0
        }
        val thickScore = when {
            thick < 1 -> 0; thick < 2 -> 3; thick < 3 -> 6
            thick < 4 -> 9; else -> 12
        }
        return ageScore + 4 * retr.toInt() + 4 * crp.toInt() + thickScore + 8 * shift.toInt()
    }

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_AGE * values[0] + B_RETRACTIONS * values[1] +
            B_CRPHIGH * values[2] + B_THICKNESS * values[3] + B_MEDIASTINAL_SHIFT * values[4]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        // 四带：中/高界=原文 Youden 操作点 0.49；极高界 >90% 对齐评分表极高带；低带 <15%
        val (band, explanation) = when {
            p < 0.15 -> "低风险" to "预测概率低于 15%。"
            p < OPERATING_POINT -> "中风险" to "预测概率 15–49%（低于操作点 0.49）。建议密切随访积液变化，评估复查超声时机。"
            p <= 0.90 -> "高风险" to "预测概率达到/超过操作点 0.49（Se 92.2%/Sp 71.1%）。建议尽早介入评估（引流/手术多学科讨论）。"
            else -> "极高风险" to "预测概率 >90%。强烈建议尽早介入评估（引流/手术多学科讨论）。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Servitje2026 cPPE 预测（PMID 42669040）",
                "性能" to "AUC 0.883（0.817–0.948）；模型切点 0.49：Se 92.2% / Sp 71.1%；评分切点 18 分：Se 96.1% / Sp 62.2%",
                "方程来源" to "原文直印（含截距），本项目首个零工程重建模型",
                "床旁速评参照" to "本例简化评分 ${bedsideScore(values)} 分（表 0–43 分四带：0–15 低/16–21 中/22–27 高/28–43 极高）——分值仅供床旁心算参照，概率一律以连续公式为准",
                "风险带" to "<15% 低 / 15–49% 中 / 49–90% 高 / >90% 极高（工程参照线；中/高界=原文操作点 0.49）",
                "适用人群" to "儿童肺炎旁胸腔积液住院患者；入院表现时点变量",
            ),
        )
    }
}
