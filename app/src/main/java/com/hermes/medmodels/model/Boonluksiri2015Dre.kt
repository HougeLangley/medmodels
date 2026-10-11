package com.hermes.medmodels.model

/**
 * Boonluksiri 2015 儿童药物难治性癫痫（DRE）风险评分（CPR）。
 *
 * 来源：Boonluksiri P, et al. J Epilepsy Res 2015;5(2):84-88; PMID 26819940; PMC4724856。
 * 算法：三组互斥单选查表求和（0–19 分，0.5 步进）+ 三档分层 + LR 验后换算（工程）。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01c-模型卡-Boonluksiri2015
 *
 * ⚠️ 三处一致纪律：本文件常量 = 模型卡第 2/3 节 = Boonluksiri2015Test 锚点（T1–T9）。
 * ⚠️ 收录基准 = 印刷版评分（D1 拍板）：「癫痫样放电」3.5 分（B/0.267=4.9 版仅存档于模型卡）。
 * ⚠️ 验后概率为贝叶斯 LR 工程换算，非原文数据（D2 拍板：整数显示 + 永久脚注）。
 */
object Boonluksiri2015Dre : AssessmentModel {

    // ===== 冻结参数（2026-08-23，模型卡第 2/3 节）=====
    private const val BAND_LOW_MAX_EXCLUSIVE = 6.0    // <6 低危
    private const val BAND_MID_MAX_INCLUSIVE = 12.0   // 6–12 中危（闭区间）；>12 高危
    private const val LR_LOW = 0.5
    private const val LR_MID = 1.8
    private const val LR_HIGH = 12.5

    override val id = "boonluksiri2015_dre"
    override val displayName = "Boonluksiri2015 · 儿童药物难治性癫痫（DRE）风险评分（基础健康儿童）"
    override val category = "神经系统"
    override val summary = "癫痫患儿的 3 变量 CPR 评分（AUROC 0.764，派生未验证，TRIPOD 1a）"

    private val ageOptions = listOf(
        CategoryOption("lt5", "<5 岁", 2.5),
        CategoryOption("b5to12", "5–12 岁（基线）", 0.0),
        CategoryOption("gt12p5", ">12.5 岁", 1.5),
    )
    private val deficitOptions = listOf(
        CategoryOption("none", "无（基线）", 0.0),
        CategoryOption("motor", "运动性缺陷", 9.0),
        CategoryOption("mental", "精神/认知缺陷", 6.0),
        CategoryOption("global", "全面性缺陷", 13.0),
    )
    private val eegOptions = listOf(
        CategoryOption("normal", "正常（基线）", 0.0),
        CategoryOption("epileptic", "癫痫样放电", 3.5),
        CategoryOption("nonepileptic", "非癫痫样异常放电", 1.0),
    )

    override val inputs = listOf(
        ModelInput(
            key = "onsetAge", label = "起病年龄", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 19.0, unit = "",
            hint = "(12, 12.5] 岁未定义区按基线 0 分计",
            options = ageOptions,
        ),
        ModelInput(
            key = "neuroDeficit", label = "既往神经缺陷", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 19.0, unit = "",
            hint = "运动 / 精神（认知）/ 全面性（运动+精神/心理社会）",
            options = deficitOptions,
        ),
        ModelInput(
            key = "eeg", label = "基础 EEG", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 19.0, unit = "",
            hint = "治疗前首次常规脑电图",
            options = eegOptions,
        ),
    )

    override val disclaimers = listOf(
        "适用人群：已确诊癫痫、开始 AED 治疗的儿童；输出为「将来发展为 DRE」的风险分层",
        "本评分为派生工具，未经内部/外部验证（TRIPOD 1a；21 条引文无验证研究）",
        "原队列 DRE 率 42%（转诊中心）；普通门诊请按可调先验（默认 30%）解读验后概率",
        "泰国苯巴比妥为主的一线方案，与国内主流（丙戊酸/左乙拉西坦）治疗背景不同",
        "模型本质是神经缺陷严重度评分（缺陷组效应占 74.5%），EEG 与年龄是修正项",
        "不适用于已明确耐药者；BECTS/JME 等综合征已含在派生队列内但异质性自认",
        "验后概率为贝叶斯 LR 工程换算，非原文数据",
    )

    /** 供测试与 UI 使用的验后换算（贝叶斯） */
    fun posteriorProbability(lr: Double, prior: Double): Double {
        val odds = lr * (prior / (1.0 - prior))
        return odds / (1.0 + odds)
    }

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val agePts = values[0]; val deficitPts = values[1]; val eegPts = values[2]
        val total = agePts + deficitPts + eegPts

        val band: String
        val lr: Double
        when {
            total < BAND_LOW_MAX_EXCLUSIVE -> { band = "低风险"; lr = LR_LOW }
            total <= BAND_MID_MAX_INCLUSIVE -> { band = "中风险"; lr = LR_MID }
            else -> { band = "高风险"; lr = LR_HIGH }
        }
        val explanation = when (band) {
            "低风险" -> "总分 <6 分。对应 +LR 0.5（验后概率低于所选基线风险）。"
            "中风险" -> "总分 6–12 分。对应 +LR 1.8。"
            else -> "总分 >12 分。对应 +LR 12.5（验后概率显著高于所选基线风险）。"
        }
        val fmt = if (total == total.toInt().toDouble()) "%.0f" else "%.1f"
        return AssessmentResult(
            value = total,
            displayValue = String.format(fmt, total) + " 分",
            bandLabel = band,
            bandExplanation = explanation,
            details = listOf(
                "模型" to "Boonluksiri2015（PMID 26819940）",
                "总分构成" to "起病年龄 ${String.format(fmt, agePts)} + 神经缺陷 ${String.format(fmt, deficitPts)} + EEG ${String.format(fmt, eegPts)}",
                "分档" to "<6 低 / 6–12 中 / >12 高（闭区间归中危）",
                "证据等级" to "派生评分，无验证（TRIPOD 1a）",
            ),
            posterior = PosteriorInfo(likelihoodRatio = lr),
        )
    }
}
