package com.hermes.medmodels.model

/**
 * Stevelink 2022 JME 治疗结局（撤药复发 + 耐药风险，一卡双条目）。
 *
 * 来源：Stevelink R, et al. eClinicalMedicine 2022;53:101732; PMID 36467455（OA）。
 * 算法源：epilepsypredictiontools @ 6444da144e（Apache-2.0）
 *   jme-withdrawal-calc.js（20400×2 int 扁平整表）/ jme-resistance-calc.js（1024 行 Map）。
 *
 * 模型卡：Obsidian 医学评估模型/01q-模型卡-Stevelink2022JME
 *
 * ⚠️ 三处一致纪律：表数据（资产/Map 逐值）= 模型卡 §3 = 单测 T1–T12。
 * ⚠️ 撤药表为二进制资产（app/src/main/assets/jme_withdrawal_p2|p5.bin，int32 LE，
 *    EptAssets 加载）；禁插值禁重建；显示取整百分数（原站 Math.round 口径）。
 */

/** JME 撤药复发（2y/5y 双输出同屏） */
object Stevelink2022JmeWithdrawal : AssessmentModel {

    private const val TABLE_LEN = 20400
    private val p2: IntArray by lazy { EptAssets.loadInts("jme_withdrawal_p2.bin", TABLE_LEN) }
    private val p5: IntArray by lazy { EptAssets.loadInts("jme_withdrawal_p5.bin", TABLE_LEN) }

    override val id = "stevelink2022_jme_withdrawal"
    override val displayName = "JME 撤药复发风险"
    override val category = "神经系统"
    override val summary = "Stevelink 2022 · IPD meta 24 队列（撤药亚组 368 例）；2y/5y 复发查表"

    override val audienceNotice = "确诊 JME 且当前无发作、考虑减药者；撤药决策须医生主导"

    override val inputs = listOf(
        ModelInput("asm", "减药时 AED 数量", ModelInput.InputType.CONTINUOUS, 1.0, 4.0, "种", "正在服用的抗发作药物种数（整数 1–4，原站钳位）"),
        ModelInput("sf", "无发作间期", ModelInput.InputType.CONTINUOUS, 0.0, 50.0, "年", "末次发作至开始减药的无发作年限（整数 0–50，钳位）"),
        ModelInput("age", "开始减药时年龄", ModelInput.InputType.CONTINUOUS, 1.0, 100.0, "岁", "整数岁（钳位 1–100）"),
    )

    override val disclaimers = listOf(
        "适用：确诊 JME、当前无发作、考虑减药者",
        "IPD meta 分析（24 队列）；区分度中等（C 0.70）——个体层面不确定性大，供医患共同决策参考",
        "撤药模型仅适用于当前无发作的 JME 患者；撤药决策须医生主导，模型不回答「应否撤药」",
        "队列年代/地域混合（欧美亚多中心）；当代新药（如左乙拉坦普及）背景下外推需注意",
        "原文观察值：>30 岁撤药 2 年复发 44%（33–53%）vs <30 岁 68%（61–73%）",
        "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        val asm = values[0].toInt().coerceIn(1, 4)
        val sf = values[1].toInt().coerceIn(0, 50)
        val age = values[2].toInt().coerceIn(1, 100)
        val idx = (asm - 1) * 5100 + sf * 100 + (age - 1)
        val pct2 = Math.round(p2[idx] / 100.0)   // P/10000 → ×100 → Math.round（原站口径）
        val pct5 = Math.round(p5[idx] / 100.0)
        return AssessmentResult(
            value = p2[idx] / 10000.0,
            displayValue = "$pct2%",
            bandLabel = "2 年复发风险",
            bandExplanation = "JME 撤药后 2 年发作复发风险（查表取整口径）。无发作间期 $sf 年、减药时 $asm 种 AED、年龄 $age 岁。",
            valueLabel = "撤药后 2 年复发（查表取整）",
            extraOutputs = listOf("5 年复发风险" to "$pct5%"),
            details = listOf(
                "模型" to "Stevelink 2022 JME 撤药（PMID 36467455，eClinicalMedicine）",
                "性能" to "C-stat 0.70（0.68–0.73，IECV）；校准图 good（Fig 4B）",
                "队列" to "IPD meta 24 队列（撤药亚组 368 例）",
                "输出口径" to "20400 点网格查表取整（原站发布口径）",
            ),
        )
    }
}

/** JME 耐药风险（9 变量 Map 查表） */
object Stevelink2022JmeResistance : AssessmentModel {

    /** 族裔四值（索引→JS 键；内部字符串不汉化） */
    private val ETHNICITY_KEYS = arrayOf("Caucasian", "Asian", "Latin-American", "Other")

    override val id = "stevelink2022_jme_resistance"
    override val displayName = "JME 耐药风险"
    override val category = "神经系统"
    override val summary = "Stevelink 2022 · IPD meta 24 队列 2518 例；9 变量混合效应 logistic 查表"

    override val audienceNotice = "新确诊 JME 的用药规划参考；族裔为统计关联项（中国汉族→Asian）"

    override val inputs = listOf(
        ModelInput("ethnicity", "族裔", ModelInput.InputType.CATEGORICAL, 0.0, 0.0, "", "统计关联项，非生物学决定论；中国汉族→Asian", options = listOf(
            CategoryOption("Caucasian", "高加索裔", 0.0),
            CategoryOption("Asian", "亚裔（中国汉族选此项）", 1.0),
            CategoryOption("Latin-American", "拉丁裔", 2.0),
            CategoryOption("Other", "其他", 3.0),
        )),
        ModelInput("histfebrseiz", "热性惊厥史", ModelInput.InputType.BINARY, 0.0, 1.0, "", ""),
        ModelInput("statusepilep", "癫痫持续状态史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "", negativeLabel = "从未", positiveLabel = "曾有"),
        ModelInput("psychcomorb", "精神科共病", ModelInput.InputType.BINARY, 0.0, 1.0, "", "抑郁/焦虑等精神科诊断"),
        ModelInput("familyhistory", "癫痫家族史", ModelInput.InputType.BINARY, 0.0, 1.0, "", "一级亲属癫痫史"),
        ModelInput("threetypes", "三种发作类型并存", ModelInput.InputType.BINARY, 0.0, 1.0, "", "肌阵挛+失神+强直阵挛三类并存", negativeLabel = "否", positiveLabel = "是"),
        ModelInput("catamenial", "月经期癫痫", ModelInput.InputType.BINARY, 0.0, 1.0, "", "仅适用女性患者；男性请选「否」（原文按女性占比计）", negativeLabel = "否", positiveLabel = "是"),
        ModelInput("eegfocality", "EEG 局灶性痫样放电", ModelInput.InputType.BINARY, 0.0, 1.0, "", ""),
        ModelInput("caehistory", "儿童失神癫痫（CAE）史", ModelInput.InputType.BINARY, 0.0, 1.0, "", ""),
    )

    override val disclaimers = listOf(
        "适用：新确诊 JME 的用药规划参考",
        "IPD meta 分析（24 队列 2518 例）；区分度中等（AUC 0.70）——个体层面不确定性大，供医患共同决策参考",
        "耐药模型含族裔变量——族裔为统计关联项，非生物学决定论；中国患者映射建议：汉族→Asian",
        "月经期癫痫（catamenial）仅适用女性患者；男性请选「否」",
        "队列年代/地域混合（欧美亚多中心）；当代新药背景下外推需注意",
        "算法移植自 UMC Utrecht Epilepsy Prediction Tools（Apache-2.0）",
    )

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        val ethnicity = ETHNICITY_KEYS[values[0].toInt().coerceIn(0, 3)]
        fun yn(i: Int) = if (values[i] == 1.0) "yes" else "no"
        val status = if (values[2] == 1.0) "ever" else "never"
        val key = "$ethnicity|${yn(1)}|$status|${yn(3)}|${yn(4)}|${yn(5)}|${yn(6)}|${yn(7)}|${yn(8)}"
        val p = EptJmeResistanceTable.LOOKUP[key]
            ?: throw IllegalArgumentException("耐药查表无此键组合——请核对输入")
        val pct = Math.round(p * 100.0)
        return AssessmentResult(
            value = p,
            displayValue = "$pct%",
            bandLabel = "耐药风险",
            bandExplanation = "JME 药物抵抗（两种足量 AED 仍发作）风险（查表取整口径）。",
            valueLabel = "JME 耐药风险（查表取整）",
            details = listOf(
                "模型" to "Stevelink 2022 JME 耐药（PMID 36467455，eClinicalMedicine）",
                "性能" to "AUC 0.70（0.68–0.72，IECV）；校准图 excellent（Fig 2B）",
                "队列" to "IPD meta 24 队列 2518 例",
                "输出口径" to "1024 键组合查表取整（原站发布口径）",
            ),
        )
    }
}
