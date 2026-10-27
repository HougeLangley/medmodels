package com.hermes.medmodels.model

/**
 * 评估模型的通用抽象：所有模型实现此接口，注册进 [ModelRegistry]。
 * 新增模型 = 新写一个实现 + 注册一行，UI 自动适配（动态表单）。
 */
interface AssessmentModel {
    /** 模型唯一 id，如 "yang2026_rmpp" */
    val id: String

    /** 显示名，如 "Yang2026 · 儿童难治性支原体肺炎（RMPP）风险预测" */
    val displayName: String

    /** 疾病门类（列表页分组用），如 "呼吸系统" */
    val category: String

    /** 简述（列表页副标题） */
    val summary: String

    /** 输入变量定义（动态表单据此渲染） */
    val inputs: List<ModelInput>

    /** 使用限制/免责声明要点（结果页与表单页展示） */
    val disclaimers: List<String>

    /**
     * 适用人群提示（D5a 用户要求：全链路强调人群）。
     * 非空时：表单页顶部固定提示条（不可折叠）+ 结果页风险带下方重复一行。
     * null = 不显示（模型人群无歧义时可省略）。
     */
    val audienceNotice: String? get() = null

    /** 执行评估。输入已按 [inputs] 顺序对齐；非法输入应抛 [IllegalArgumentException] 并给中文消息。 */
    fun assess(values: List<Double>): AssessmentResult
}

/** 单个输入变量的定义 */
data class ModelInput(
    /** 代码标识符（模型卡第 7 节） */
    val key: String,
    /** 中文标签 */
    val label: String,
    /** 输入类型 */
    val type: InputType,
    /** 合法范围（连续变量）；二元/分类变量忽略 */
    val min: Double,
    val max: Double,
    /** 单位后缀，如 "天" "℃" "U/L"；二元/分类为空 */
    val unit: String,
    /** 填写提示，如 "入院前发热天数" */
    val hint: String,
    /** 二元变量的选项文案：阴性/阳性 */
    val negativeLabel: String = "无",
    val positiveLabel: String = "有",
    /** 分类变量的选项列表（CATEGORICAL 时非空，互斥单选） */
    val options: List<CategoryOption> = emptyList(),
) {
    enum class InputType { CONTINUOUS, BINARY, CATEGORICAL }

    /** 校验并归一化输入，返回错误消息（null = 通过） */
    fun validate(raw: Double?): String? {
        if (raw == null || raw.isNaN()) {
            return if (type == InputType.CATEGORICAL) "「$label」未选择" else "「$label」未填写"
        }
        if (type == InputType.CONTINUOUS) {
            if (raw < min || raw > max) return "「$label」须在 $min–$max $unit 范围内"
        } else if (type == InputType.BINARY) {
            if (raw != 0.0 && raw != 1.0) return "「$label」取值非法"
        }
        // CATEGORICAL：分值来自查表，无需范围校验
        return null
    }
}

/** 分类变量的单个选项（互斥单选组内一项） */
data class CategoryOption(
    /** 选项 key（模型卡编码表） */
    val key: String,
    /** 显示文案（含基线标注） */
    val label: String,
    /** 该选项的分值/贡献 */
    val score: Double,
)

/** 评估结果 */
data class AssessmentResult(
    /** 主结果数值（概率 0–1；评分制模型为总分） */
    val value: Double,
    /** 主结果格式化文案，如 "86.3%" 或 "12.5 分" */
    val displayValue: String,
    /** 风险档位标签，如 "高风险" */
    val bandLabel: String,
    /** 档位解释文案 */
    val bandExplanation: String,
    /** 结果页附加说明行（键值对形式展示） */
    val details: List<Pair<String, String>>,
    /** 主值下方小字标签（如 "2 年复发风险"）；null = 用默认文案 */
    val valueLabel: String? = null,
    /** 多输出模型的同屏次要输出（标签→显示值），如 AED 的 5y/10y；空 = 单输出 */
    val extraOutputs: List<Pair<String, String>> = emptyList(),
    /** 验后概率块（仅 LR 型评分模型提供；null = 不显示） */
    val posterior: PosteriorInfo? = null,
)

/** 似然比验后换算信息（工程换算，非原文数据） */
data class PosteriorInfo(
    /** 该档位阳性似然比 */
    val likelihoodRatio: Double,
    /** 默认先验概率（0–1） */
    val defaultPrior: Double = 0.30,
    /** 可选先验列表（0–1） */
    val priorChoices: List<Double> = listOf(0.10, 0.20, 0.30, 0.42),
    /** 永久脚注 */
    val footnote: String = "评分为未经验证的派生工具；概率按所选基线风险换算，仅供分层参考，非原文数据",
)
