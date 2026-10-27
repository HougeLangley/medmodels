package com.hermes.medmodels.model

/**
 * Chen2026 SMPP 入院 24h 重症预测（儿童 MPP 早期分层）。
 *
 * 来源：Chen Z, Ding W, Gao Z. Front Pediatr 2026;14:1927137; PMID 42890885（CC BY）。
 * 算法：2 CONTINUOUS + 3 BINARY Logistic——零工程重建（截距与 β 全部原文直印）。
 *
 * 模型卡：Obsidian 医学评估模型/01l-模型卡-Chen2026SMPP
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 3.1 节 = Chen2026Test 锚点（T1–T12）。
 * ⚠️ 无分带硬约束（卡 §3.2）：原文未定义风险带——禁止编造分带；
 *    输出=连续概率 + 切点参照线 0.553（高于/低于切点，非风险档位判定）。
 * ⚠️ 列线图分值表禁止实现（无反应项 γ 偏离 +13%，R 渲染取整——卡 §3.3）。
 */
object Chen2026Smpp : AssessmentModel {

    // ===== 冻结参数（2026-10-27，模型卡 §3.1，原文直印零重建）=====
    private const val INTERCEPT = -5.312
    private const val B_FEVER_DAYS = 0.703
    private const val B_SHORTNESS_OF_BREATH = 4.715
    private const val B_MACROLIDE_UNRESPONSIVE = 1.305
    private const val B_IL6 = 0.051
    private const val B_CONSOLIDATION = 3.454

    // 原文操作切点（参照线，非风险带阈值；卡 §3.2）
    const val OPERATING_CUTOFF = 0.553

    override val id = "chen2026_smpp"
    override val displayName = "SMPP 重症预测（儿童 MPP·入院 24h）"
    override val category = "呼吸系统"
    override val summary = "Chen2026 · 入院 24h 内 5 变量 Logistic（AUC 训练 0.916/时间外部 0.817，零重建）"

    override val audienceNotice =
        "住院 MPP 患儿（1–14 岁、发热 1–5 天）入院 24h 内评估；门诊/发热>5 天患儿超出验证域"

    override val inputs = listOf(
        ModelInput("feverDays", "入院前发热时长", ModelInput.InputType.CONTINUOUS, 1.0, 5.0, "天",
            "入组域 24–120h（1–5 天，步进 0.5 天）"),
        ModelInput("shortnessOfBreath", "气促", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "呼吸频率 1–5 岁≥40、>5 岁≥30 次/分（原文定义）"),
        ModelInput("macrolideUnresponsive", "入院前大环内酯无反应", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院前大环内酯治疗 48–72h 无临床/影像改善或加重", negativeLabel = "否", positiveLabel = "是"),
        ModelInput("il6", "IL-6", ModelInput.InputType.CONTINUOUS, 0.0, 100.0, "",
            "原文标 ng/mL，量级疑为 pg/mL——按检验报告原值输入，勿换算"),
        ModelInput("consolidation", "肺实变", ModelInput.InputType.BINARY, 0.0, 1.0, "",
            "入院胸片/CT 至少一叶均匀致密影（含空气支气管征）"),
    )

    override val disclaimers = listOf(
        "适用：住院 MPP 患儿（1–14 岁、发热 1–5 天）入院 24h 内的 SMPP 风险分层（需加用激素的重症型）",
        "本模型来自单中心回顾性研究（深圳，412 例），时间外部验证（次年同院）AUC 降至 0.817，跨中心外推未验证",
        "训练集 AUC≥0.90（D6a 留痕）：外部校准斜率 0.836，过校正风险中等；巨系数（气促 OR 111.6、实变 31.6）基于少数阳性病例，点估计不稳定（CI 宽）",
        "SMPP 结局定义含「需加用激素」成分——部分治疗锚定，存在指征偏倚通道",
        "IL-6 原文单位标 ng/mL，量级与 pg/mL 吻合，存在标称笔误可能；按检验报告原值输入",
        "适用域：1–14 岁住院 MPP、入院发热 24–120h、入院 24h 内变量；高患病率（53.6%/67.9%）住院场景下开发",
        "2023–24 中国 MRMP 大流行年代锚定",
        "原文未定义风险带——本工具输出连续概率与切点参照（0.553），不作风险档位判定",
        "预测结果不构成治疗决策依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_FEVER_DAYS * values[0] + B_SHORTNESS_OF_BREATH * values[1] +
            B_MACROLIDE_UNRESPONSIVE * values[2] + B_IL6 * values[3] + B_CONSOLIDATION * values[4]
        val p = 1.0 / (1.0 + Math.exp(-logit))
        // 无分带（卡 §3.2 硬约束）：仅输出相对切点参照线的位置，非风险档位
        val (band, explanation) = if (p >= OPERATING_CUTOFF) {
            "高于切点" to "预测概率达到/超过原文切点 0.553。建议按重症路径加强评估（激素使用指征讨论、影像复查）。"
        } else {
            "低于切点" to "预测概率低于原文切点 0.553。建议常规路径监测，动态复评。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Chen2026 SMPP 预测（PMID 42890885）",
                "切点参照" to "原文切点 0.553：时间外部验证 Se 61.2%/Sp 90.2%（训练 Se 78.2%/Sp 90.3%）",
                "性能" to "AUC 训练 0.916 / 内部 0.907 / 时间外部 0.817；校准 外部 slope 0.836；HL 训练 P=0.749/验证 P=0.206",
                "方程来源" to "原文直印（含截距），零工程重建",
                "输出说明" to "原文未定义风险带——切点参照线非风险档位判定；列线图分值表未实现（γ 取整偏差）",
                "适用人群" to "住院 MPP 1–14 岁、发热 1–5 天；入院 24h 内变量",
            ),
        )
    }
}
