package com.hermes.medmodels.model

/**
 * Cloppenborg 2022 儿童癫痫术后认知预后（IQ≥85 / IQ≥70 / DQ≥50，一卡三条目）。
 *
 * 来源：Cloppenborg T, et al. Neurology 2022;98(2):e225–e235; PMID 34795046。
 * 算法源：epilepsypredictiontools @ 6444da144e（Apache-2.0）iq85/iq70/dq50-calc.js——
 * **纯查表架构**：模型在输入网格上的全概率查表（非系数转抄），键不存在→报错。
 *
 * 模型卡：Obsidian 医学评估模型/01p-模型卡-Cloppenborg2022认知预后
 *
 * ⚠️ 三处一致纪律：EptIqTables（400/200/160 行逐值）= 模型卡 §3 = 单测 T1–T12。
 * ⚠️ 键值内部字符串与 JS 完全一致（focal/multilobar、no/yes），仅 UI 标签汉化。
 * ⚠️ 显示 = 值×100 一位小数（原站 toFixed(1) 口径）；IQ 钳位 41–140，DQ 钳位 1–80。
 */
abstract class Cloppenborg2022Base : AssessmentModel {

    abstract val table: Map<String, Double>
    abstract val tableName: String
    abstract val outcomeLabel: String

    /** 子类给出键 */
    abstract fun makeKey(values: List<Double>): String

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        val key = makeKey(values)
        val p = table[key]
            ?: throw IllegalArgumentException("查表无此组合（$tableName key=$key）——请核对输入")
        val pct = Math.round(p * 1000.0) / 10.0  // 原站 Math.round(100*prob*10)/10
        return AssessmentResult(
            value = p,
            displayValue = "$pct%",
            bandLabel = outcomeLabel,
            bandExplanation = "术后 2 年 $outcomeLabel 的查表概率（发布网格逐值，非连续插值）。",
            valueLabel = "术后 2 年$outcomeLabel（查表）",
            details = listOf(
                "模型" to "Cloppenborg 2022 认知预后（PMID 34795046，Neurology）",
                "性能" to "AUC IQ≥85 0.90（0.84–0.96）/ IQ≥70 0.92（0.87–0.97）/ DQ≥50 0.846（0.779–0.913）",
                "验证" to "IQ 模型 Utrecht 外部验证准确率 82%/84%（n=102）；DQ 10-fold 76%",
                "输出口径" to "查表发布概率，键组合精确匹配（无插值）",
                "适用人群" to "拟行或已行癫痫切除术的儿童；IQ 可测用 IQ 条目，婴幼儿/重度障碍用 DQ 条目",
            ),
        )
    }

    /** 共用钳位 */
    protected fun clampIq(v: Double) = v.toInt().coerceIn(41, 140)
    protected fun clampDq(v: Double) = v.toInt().coerceIn(1, 80)
    protected fun surgeryKey(v: Double) = if (v == 1.0) "multilobar" else "focal"
}

/** 术后 2 年 IQ≥85：key = preIQ|surgery|invasive（400 行） */
object Cloppenborg2022Iq85 : Cloppenborg2022Base() {
    override val table = EptIqTables.IQ85
    override val tableName = "IQ85"
    override val outcomeLabel = "IQ≥85"

    override val id = "cloppenborg2022_iq85"
    override val displayName = "术后 2 年 IQ≥85（儿童癫痫外科）"
    override val category = "神经系统"
    override val summary = "Cloppenborg 2022 · 术前 IQ + 术式 + 有创 EEG → 查表（AUC 0.90，Utrecht 外部验证）"
    override val audienceNotice = "拟行或已行癫痫切除术的儿童（IQ 可测者）；婴幼儿/重度障碍请用 DQ 条目"

    override val inputs = listOf(
        ModelInput("preIQ", "术前 IQ", ModelInput.InputType.CONTINUOUS, 41.0, 140.0, "分", "术前智商测试值（整数，钳位 41–140）"),
        ModelInput("surgery", "术式", ModelInput.InputType.BINARY, 0.0, 1.0, "", "", negativeLabel = "局灶切除", positiveLabel = "多脑叶/半球切除"),
        ModelInput("invasive", "有创 EEG 检查", ModelInput.InputType.BINARY, 0.0, 1.0, "", "术前/术中是否行有创脑电监测", negativeLabel = "无", positiveLabel = "有"),
    )

    override val disclaimers = COMMON_DISCLAIMERS

    override fun makeKey(values: List<Double>): String {
        val iq = clampIq(values[0])
        val surgery = surgeryKey(values[1])
        val invasive = if (values[2] == 1.0) "yes" else "no"
        return "$iq|$surgery|$invasive"
    }
}

/** 术后 2 年 IQ≥70：key = preIQ|surgery（200 行） */
object Cloppenborg2022Iq70 : Cloppenborg2022Base() {
    override val table = EptIqTables.IQ70
    override val tableName = "IQ70"
    override val outcomeLabel = "IQ≥70"

    override val id = "cloppenborg2022_iq70"
    override val displayName = "术后 2 年 IQ≥70（儿童癫痫外科）"
    override val category = "神经系统"
    override val summary = "Cloppenborg 2022 · 术前 IQ + 术式 → 查表（AUC 0.92，Utrecht 外部验证）"
    override val audienceNotice = "拟行或已行癫痫切除术的儿童（IQ 可测者）；婴幼儿/重度障碍请用 DQ 条目"

    override val inputs = listOf(
        ModelInput("preIQ", "术前 IQ", ModelInput.InputType.CONTINUOUS, 41.0, 140.0, "分", "术前智商测试值（整数，钳位 41–140）"),
        ModelInput("surgery", "术式", ModelInput.InputType.BINARY, 0.0, 1.0, "", "", negativeLabel = "局灶切除", positiveLabel = "多脑叶/半球切除"),
    )

    override val disclaimers = COMMON_DISCLAIMERS

    override fun makeKey(values: List<Double>): String {
        return "${clampIq(values[0])}|${surgeryKey(values[1])}"
    }
}

/** 术后 2 年 DQ≥50：key = preDQ|mcd（160 行） */
object Cloppenborg2022Dq50 : Cloppenborg2022Base() {
    override val table = EptIqTables.DQ50
    override val tableName = "DQ50"
    override val outcomeLabel = "DQ≥50"

    override val id = "cloppenborg2022_dq50"
    override val displayName = "术后 2 年 DQ≥50（儿童癫痫外科）"
    override val category = "神经系统"
    override val summary = "Cloppenborg 2022 · 术前 DQ + MCD 病因 → 查表（AUC 0.846，10-fold CV）"
    override val audienceNotice = "无法配合 IQ 测试的婴幼儿/重度障碍患儿；IQ 可测者请用 IQ 条目"

    override val inputs = listOf(
        ModelInput("preDQ", "术前 DQ", ModelInput.InputType.CONTINUOUS, 1.0, 80.0, "分", "术前发育商（整数，钳位 1–80）"),
        ModelInput("mcd", "MCD 病因", ModelInput.InputType.BINARY, 0.0, 1.0, "", "皮层发育不良（malformation of cortical development）病因", negativeLabel = "否", positiveLabel = "是"),
    )

    override val disclaimers = COMMON_DISCLAIMERS

    override fun makeKey(values: List<Double>): String {
        return "${clampDq(values[0])}|${if (values[1] == 1.0) "yes" else "no"}"
    }
}

/** 三条目共用限制页（卡 §6 照抄） */
private val COMMON_DISCLAIMERS = listOf(
    "开发于德国 Bethel 单中心（IQ 258/DQ 127）；IQ 模型经 Utrecht 102 例外部验证（准确率 82%/84%），DQ 模型仅 10-fold 交叉验证——DQ 条目外推性较弱",
    "预测的是术后 2 年认知达标概率；更长程轨迹不在域内",
    "术前 IQ/DQ 是最强预测因子（r=0.85/0.57）——模型本质是「术前功能+术式/病因」的联合估计，不预测个体化提升空间",
    "DQ 队列与 IQ 队列人群构成不同（DQ 组更多癫痫性脑病、半球切除、更小年龄），DQ≥50 条目勿外推至 IQ 可测儿童",
    "校准图原文已核查但未展示（声明）；AUC 0.90/0.92 经独立外部验证支持",
    "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）；查表为作者模型在输入网格上的发布概率",
    "预测结果不构成手术决策依据",
)
