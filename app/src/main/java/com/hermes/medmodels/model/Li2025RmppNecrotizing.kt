package com.hermes.medmodels.model

/**
 * Li2025 RMPP 并发坏死性肺炎（NP）风险预测。
 *
 * 来源：Li X, Zhao L, … Guo W, Ning J. Ital J Pediatr 2025;51:150; PMID 40399899; PMC12096616（CC BY）。
 * 算法：4 CONTINUOUS Logistic——项目首个纯连续无开关模型。
 * 系数：β 链由列线图 worked example 分值反标定（四 β 全落印刷 OR 95% CI）；
 *       ⚠️ 禁止用 ln(印刷 OR) 实现——IL-6 的 OR 1.007 三位小数在 300 pg/mL 处舍入放大 1.4 logit，直算系统性高估。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01j-模型卡-Li2025RMPP坏死
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3/4 节 = Li2025Test 锚点（T1–T10）。
 */
object Li2025RmppNecrotizing : AssessmentModel {

    // ===== 冻结参数（2026-10-10，模型卡第 3/4 节，D12a 冻结版·反标定 β）=====
    private const val INTERCEPT = -6.891        // 风险轴左锚（TP=0 ↔ 全变量零）
    private const val B_FEVER_DAYS = 0.297      // 印刷 OR 1.475（1.296–1.678），隐含 OR 1.346 偏下缘
    private const val B_WBC = 0.133             // 印刷 OR 1.149（1.073–1.231）
    private const val B_IL6 = 0.00636           // 印刷 OR 1.007（1.002–1.013）——舍入放大关键项
    private const val B_DDIMER = 0.212          // 印刷 OR 1.361（1.121–1.652）

    // 风险带（工程自定，锚=训练患病率 16.0%；原文无预设操作切点）
    const val BAND_LOW_UPPER = 0.08   // <8% 低危
    const val BAND_HIGH_LOWER = 0.30  // >30% 高危；8–30% 中危

    override val id = "li2025_rmpp_np"
    override val displayName = "Li2025 · 坏死性肺炎风险（儿童 RMPP·住院）"
    override val category = "呼吸系统"
    override val summary = "RMPP 坏死性肺炎 4 变量预测（AUC 训练 0.899/验证 0.920，HL+DCA 全报）"

    override val audienceNotice =
        "住院难治性 MPP（大环内酯 7 天仍发热/影像进展）患儿 1–16 岁；本模型预测 RMPP→NP 发生；MPP 已并发 PB 的复发预测用 Na2026 模型（人群不同勿混用）"

    override val inputs = listOf(
        ModelInput("feverDays", "发热时长", ModelInput.InputType.CONTINUOUS, 0.0, 14.0, "天",
            "发热天数（整数），列线图域 0–14 天"),
        ModelInput("wbc", "白细胞计数", ModelInput.InputType.CONTINUOUS, 0.0, 35.0, "×10⁹/L",
            "血常规 WBC，单位 ×10⁹/L（一位小数）"),
        ModelInput("il6", "IL-6", ModelInput.InputType.CONTINUOUS, 0.0, 1600.0, "pg/mL",
            "血清 IL-6，单位 pg/mL（原文标称 ng/L 系笔误，数值链自证为 pg/mL）"),
        ModelInput("ddimer", "D-二聚体", ModelInput.InputType.CONTINUOUS, 0.0, 20.0, "mg/L",
            "D-dimer，单位 mg/L（一位小数）"),
    )

    override val disclaimers = listOf(
        "适用：RMPP 住院患儿的坏死性肺炎风险分层（提示增强 CT 时机/激素与抗凝决策讨论）",
        "慎用：普通 MPP（非难治性）——原文人群全为 RMPP，外推未验证",
        "不适用：非 MP 肺炎；已确诊 NP 者（本模型用于 NP 发生前）；先天心肺疾病/免疫缺陷/气道异物（原文排除）",
        "β 为列线图反标定（非原文直印）：worked example 分值+概率轴像素标定反推，四 β 全落印刷 OR 95% CI（方法见评估报告 21）",
        "发热 β 落 OR CI 下缘（不确定度主源）——±10% β 使 worked example 在 68–92% 区间波动",
        "验证 AUC 0.920 过 0.90 线（审视结论：过拟合风险低——训练 0.899 与验证持平+EPV=15，按 AHA D6a 留痕）",
        "无外部验证（随机分割+bootstrap，TRIPOD 等价 2a）；单中心（天津儿童医院）",
        "风险带为工程参照线（锚=训练患病率 16.0%），非原文操作点",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_FEVER_DAYS * values[0] + B_WBC * values[1] +
            B_IL6 * values[2] + B_DDIMER * values[3]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        val (band, explanation) = when {
            p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 8%（工程参照线，锚=训练患病率 16.0%）。"
            p <= BAND_HIGH_LOWER -> "中风险" to "预测概率 8–30%。建议密切影像随访，评估增强 CT 时机。"
            else -> "高风险" to "预测概率 >30%。建议积极影像评估（增强 CT）并讨论激素/抗凝方案。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Li2025 RMPP→NP 预测（PMID 40399899）",
                "性能" to "AUC 训练 0.899 / 验证 0.920；HL P=0.439/0.526 + 校准图 + DCA",
                "β 来源" to "列线图 worked example 反标定（非直印），四 β 全落印刷 OR 95% CI；截距为风险轴锚定",
                "风险带" to "<8% 低 / 8–30% 中 / >30% 高（工程参照线，非原文操作点）",
                "适用人群" to "住院 RMPP 患儿 1–16 岁；预测 NP 发生（已确诊 NP 者不适用）",
            ),
        )
    }
}
