package com.hermes.medmodels.model

/**
 * Geng2022 儿童药物难治性癫痫（DRE）早期风险预测列线图。
 *
 * 来源：Geng Hua, Chen Xuqin. Front Pediatr 2022;10:905177; PMID 36110106; PMC9468368。
 * 算法：6 BINARY + 1 CATEGORICAL（病因六分类，参照=遗传）Logistic 概率模型。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01d-模型卡-Geng2022
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 2/3 节 = Geng2022Test 锚点（T1–T11）。
 * ⚠️ 截距 −3.776 非原文直报：由 Fig 3 worked example（起病<1+SE+结构 → 84%）反推，用户已确认。
 * ⚠️ 病因参照组 = 遗传（与 worked example 自洽的唯一解读），留痕冻结。
 */
object Geng2022DreEarly : AssessmentModel {

    // ===== 冻结参数（2026-08-24，模型卡第 2/3 节）=====
    private const val INTERCEPT = -3.776
    private const val B_ONSET_LT1 = 1.376
    private const val B_STATUS_EPILEPTICUS = 1.560
    private const val B_FOCAL = 1.058
    private const val B_PRETREAT_GT20 = 1.179
    private const val B_DEE = 1.292
    private const val B_NEURO_ABNORMAL = 1.744

    // 工程自定风险带（原文未报截断；中位锚 = 训练患病率 33.7%）
    const val BAND_LOW_UPPER = 0.15   // <15% 低危
    const val BAND_MID_UPPER = 0.50   // 15–50% 中危；>50% 高危

    override val id = "geng2022_dre_early"
    override val displayName = "Geng2022 · 儿童药物难治性癫痫（DRE）早期风险预测（AED 治疗前）"
    override val category = "神经系统"
    override val summary = "初诊癫痫患儿 AED 治疗前的 7 变量 Logistic 模型（AUC 0.92/0.91，外部验证，TRIPOD 3）"

    private val etiologyOptions = listOf(
        CategoryOption("genetic", "遗传性（基线）", 0.0),
        CategoryOption("structural", "结构性", 2.498),
        CategoryOption("metabolic", "代谢性", 2.519),
        CategoryOption("immune", "免疫性 · 原文未达显著", 0.542),
        CategoryOption("infectious", "感染性", 1.366),
        CategoryOption("unknown", "未知病因", -0.942),
    )

    override val inputs = listOf(
        ModelInput("onsetLT1", "起病年龄 <1 岁", ModelInput.InputType.BINARY, 0.0, 1.0, "", "起病年龄小于 1 岁"),
        ModelInput("statusEpilepticus", "癫痫持续状态史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "既往癫痫持续状态"),
        ModelInput("focal", "局灶性发作", ModelInput.InputType.BINARY, 0.0, 1.0, "", "发作形式为局灶性"),
        ModelInput("pretreatGT20", "治疗前发作 >20 次", ModelInput.InputType.BINARY, 0.0, 1.0, "", "AED 治疗开始前发作次数超过 20 次"),
        ModelInput("dee", "发育性/癫痫性脑病", ModelInput.InputType.BINARY, 0.0, 1.0, "", "DEE 诊断"),
        ModelInput("neuroAbnormal", "神经系统异常", ModelInput.InputType.BINARY, 0.0, 1.0, "", "神经系统检查异常/缺陷"),
        ModelInput(
            key = "etiology", label = "病因分类（ILAE 六分类）", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 3.0, unit = "",
            hint = "参照组=遗传性；免疫性原文 P=0.421 未达显著，结果解释应保守",
            options = etiologyOptions,
        ),
    )

    override val disclaimers = listOf(
        "适用人群：初诊癫痫儿童、AED 治疗开始前评估（目标是提前 ≥1 年识别 DRE 风险）",
        "病因分类依赖 MRI/代谢筛查/抗体等辅助检查结论；未完成病因评估的患儿不适用或只能选「未知」",
        "截距由原文 Fig 3 例题反推（非原文直报），模型参数以 OR 表 + worked example 锚定",
        "风险分带（<15% 低 / 15–50% 中 / >50% 高）为工程划定，原文未提供操作点性能",
        "江苏 2015–2018 三级儿童医院队列（训练 679 + 外部验证 347）；跨省/当代外推需谨慎",
        "免疫病因类别原文未达统计显著（OR 1.72，P=0.421），选该项时解释应更保守",
        "预测「将来发展为 DRE」的概率，不构成治疗方案选择依据；不适用于已确诊 DRE 者",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val onset = values[0]; val se = values[1]; val focal = values[2]
        val gt20 = values[3]; val dee = values[4]; val neuro = values[5]; val etioBeta = values[6]

        val logit = INTERCEPT +
            B_ONSET_LT1 * onset +
            B_STATUS_EPILEPTICUS * se +
            B_FOCAL * focal +
            B_PRETREAT_GT20 * gt20 +
            B_DEE * dee +
            B_NEURO_ABNORMAL * neuro +
            etioBeta
        val p = 1.0 / (1.0 + Math.exp(-logit))

        val band = when {
            p < BAND_LOW_UPPER -> "低风险" to "预测概率低于 15%（工程划定低危带）。按常规路径启动治疗与随访。"
            p <= BAND_MID_UPPER -> "中风险" to "预测概率 15–50%。建议密切随访治疗反应，动态评估发作控制情况。"
            else -> "高风险" to "预测概率 >50%。建议早期密切监测治疗反应、提前规划多药方案与专科随访，防范 DRE 发展。"
        }
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = band.first,
            bandExplanation = band.second,
            details = listOf(
                "模型" to "Geng2022（PMID 36110106）",
                "证据等级" to "外部验证（TRIPOD 3）：AUC 训练 0.92 / 外部 0.91",
                "风险带" to "<15% 低 / 15–50% 中 / >50% 高（工程划定）",
                "截距来源" to "原文 Fig 3 例题反推（−3.776）",
            ),
        )
    }
}
