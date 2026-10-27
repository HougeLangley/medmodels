package com.hermes.medmodels.model

/**
 * van Diessen 2018 儿童癫痫确诊概率（首次就诊）。
 *
 * 来源：van Diessen E, et al. Pediatrics 2018;142(6):e20180931; PMID 30389715。
 * 算法源：作者官方开源 epilepsypredictiontools @ 6444da144e（Apache-2.0）fsc-calc.js——
 * 截距+系数直印，零工程重建。
 *
 * 模型卡：Obsidian 医学评估模型/01m-模型卡-vanDiessen2018FSC
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡 §3 = VanDiessen2018Test 锚点（T1–T6+钳位）。
 * ⚠️ 年龄钳位 0–18（原站行为）；无分带（原站无带）——输出连续概率一位小数。
 */
object VanDiessen2018Fsc : AssessmentModel {

    // ===== 冻结参数（2026-10-27，模型卡 §3 = fsc-calc.js 直印）=====
    private const val INTERCEPT = -3.79076
    private const val B_MALE = 0.65583
    private const val B_AGE = 0.04660
    private const val B_BILATERAL = 0.58519
    private const val B_CRAMP = 0.65431
    private const val B_LATERALIZATION = 0.76494
    private const val B_LOSS_TONUS = 1.12028
    private const val B_AUTOMATISM = 1.62695
    private const val B_HX_NEURO = 1.33676
    private const val B_HX_PSYCH = 0.21355
    private const val B_SYNDROME = 0.79069
    // EEG 四分类系数（normal 0 / generalizedEpi 3.80806 / localizedEpi 3.20263 / hypofunctional 1.27371）
    // 直接承载于选项 score

    override val id = "vandiessen2018_fsc"
    override val displayName = "癫痫确诊概率（儿童·首诊）"
    override val category = "神经系统"
    override val summary = "van Diessen 2018 · 首诊 11 变量 Logistic（外部验证 AUC 0.86，零重建）"

    override val audienceNotice = "≤18 岁、≥1 次疑似发作后首次门诊评估；非急诊/非已确诊患儿"

    override val inputs = listOf(
        ModelInput("male", "男性", ModelInput.InputType.BINARY, 0.0, 1.0, "", "", negativeLabel = "女", positiveLabel = "男"),
        ModelInput("ageFirstEvent", "首次事件年龄", ModelInput.InputType.CONTINUOUS, 0.0, 18.0, "岁", "首次疑似发作时年龄（原站钳位 0–18）"),
        ModelInput("bilateral", "双侧抽动", ModelInput.InputType.BINARY, 0.0, 1.0, "", "发作时双侧肢体抽动"),
        ModelInput("cramp", "强直/痉挛样动作", ModelInput.InputType.BINARY, 0.0, 1.0, "", "肢体僵硬或节律性痉挛"),
        ModelInput("lateralization", "偏侧化症状", ModelInput.InputType.BINARY, 0.0, 1.0, "", "一侧肢体起病/头眼偏转等"),
        ModelInput("lossTonus", "肌张力丧失/无力", ModelInput.InputType.BINARY, 0.0, 1.0, "", "发作中肌张力突然丧失"),
        ModelInput("automatism", "自动症", ModelInput.InputType.BINARY, 0.0, 1.0, "", "咂嘴/摸索/无目的重复动作"),
        ModelInput("historyNeurological", "神经科病史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "既往神经科疾病史"),
        ModelInput("historyPsychiatric", "精神科病史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "既往精神科疾病史"),
        ModelInput("syndrome", "代谢/遗传综合征", ModelInput.InputType.BINARY, 0.0, 1.0, "", "已知代谢性或遗传性综合征"),
        ModelInput("eegResult", "首诊间期 EEG", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "首次就诊间期脑电图", options = listOf(
            CategoryOption("normal", "正常", 0.0),
            CategoryOption("generalizedEpi", "全面性痫样放电", 3.80806),
            CategoryOption("localizedEpi", "局灶性痫样放电", 3.20263),
            CategoryOption("hypofunctional", "非特异性异常（低功能）", 1.27371),
        )),
    )

    override val disclaimers = listOf(
        "适用：≤18 岁、因≥1 次疑似发作首次就诊的儿童；EEG 为首诊间期 EEG",
        "开发于荷兰单中心门诊队列（451 例），外部验证为同国另一中心 187 例；原文未报告校准（无校准图/HL），仅有区分度与 PPV/NPV",
        "结局为「≥1 年随访后确诊癫痫」；随访期外迟发癫痫不在预测域内",
        "与「首发后复发评分（Boonluksiri2015）」是相邻但不同问题（确诊 vs 复发），不可互换使用",
        "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）",
        "预测结果不构成诊断依据",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        // 年龄钳位 0–18（原站行为，卡 §3）；其余开关/分类由选项约束
        val v = values.toMutableList()
        v[1] = v[1].coerceIn(0.0, 18.0)
        inputs.forEachIndexed { i, input ->
            if (i != 1) input.validate(v[i])?.let { throw IllegalArgumentException(it) }
        }
        val logit = INTERCEPT + B_MALE * v[0] + B_AGE * v[1] + B_BILATERAL * v[2] + B_CRAMP * v[3] +
            B_LATERALIZATION * v[4] + B_LOSS_TONUS * v[5] + B_AUTOMATISM * v[6] +
            B_HX_NEURO * v[7] + B_HX_PSYCH * v[8] + B_SYNDROME * v[9] + v[10]  // v[10]=EEG 选项 score=β
        val p = 1.0 / (1.0 + Math.exp(-logit))
        return AssessmentResult(
            value = p,
            displayValue = String.format("%.1f", p * 100) + "%",
            bandLabel = "确诊概率",
            bandExplanation = "预测该患儿在 ≥1 年随访后最终确诊癫痫的概率（连续输出，原站无风险分带）。",
            valueLabel = "确诊癫痫概率（≥1 年随访）",
            details = listOf(
                "模型" to "van Diessen 2018 首诊确诊（PMID 30389715）",
                "性能" to "外部验证 AUC 0.86（0.80–0.92）；PPV 0.93 / NPV 0.76；诊断不明亚组 AUC 0.73",
                "校准" to "原文未报告（限制页声明）",
                "方程来源" to "作者官方开源实现直印（Apache-2.0），零工程重建",
                "适用人群" to "≤18 岁儿童首诊（≥1 次疑似发作后）",
            ),
        )
    }
}
