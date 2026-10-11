package com.hermes.medmodels.model

/**
 * Knight2020 4C Mortality Score（成人住院新冠死亡风险）。
 *
 * 来源：Knight SR, et al. ISARIC4C. BMJ 2020;370:m3339; PMID 32907855（勘误 m4334 已并入）。
 * 算法：原始值输入 → 代码内分档 → 整数计分（0–21）→ 四带判读。
 * 证据：A+（被引 808）；时序验证 22 361 例；多国外部验证生态；校准 slope 1.0/CITL 0/Brier 0.171。
 *
 * 模型卡：Obsidian 学习笔记/医学评估模型/01g-模型卡-Knight2020_4C
 *
 * ⚠️ 三处一致纪律：本文件分档函数 = 模型卡第 4 节 = Knight2020FourCTest 锚点（T1–T14）。
 * ⚠️ 分档逻辑只允许发生在本 assess() 内（卡 §7.1）；17 岁以下拒绝消息指向 Wong2022（卡 §7.5）。
 */
object Knight2020FourC : AssessmentModel {

    // ===== 四带常量（模型卡第 4 节，闭区间）=====
    const val LOW_MAX = 3      // 0–3 低危
    const val MID_MAX = 8      // 4–8 中危
    const val HIGH_MAX = 14    // 9–14 高危；≥15 极高危

    override val id = "knight2020_4c_mortality"
    override val displayName = "4C（Knight2020）· 成人住院新冠死亡风险评分（2020 原始株时代）"
    override val category = "感染相关"
    override val summary = "成人住院新冠 8 变量死亡评分（AUROC 0.767，时序验证 2.2 万例，被引 808）"

    override val audienceNotice = "2020 年英国原始株·疫苗前时代队列；组内死亡率为当时绝对值，当代相对排序仍稳健；仅限成人住院患者"

    private val comorbOptions = listOf(
        CategoryOption("zero", "无合并症", 0.0),
        CategoryOption("one", "1 项", 1.0),
        CategoryOption("twoPlus", "≥2 项（Charlson 或肥胖）", 2.0),
    )

    override val inputs = listOf(
        ModelInput("age", "年龄", ModelInput.InputType.CONTINUOUS, 18.0, 120.0, "岁", "仅限成人（≥18）；儿童请用 Wong2022"),
        ModelInput("respiratoryRate", "呼吸频率", ModelInput.InputType.CONTINUOUS, 8.0, 60.0, "次/分", "入院时呼吸频率"),
        ModelInput("oxygenSaturation", "血氧饱和度", ModelInput.InputType.CONTINUOUS, 50.0, 100.0, "%", "室内空气下测量（未吸氧）；吸氧下测量会高估安全性"),
        ModelInput("urea", "尿素", ModelInput.InputType.CONTINUOUS, 0.5, 100.0, "mmol/L", "单位 mmol/L。国内「尿素」多直接报 mmol/L（直接填）；若报告为尿素氮 BUN(mg/dL)，尿素≈BUN×0.357；若为尿素氮(mmol/L)，尿素≈尿素氮×2.14"),
        ModelInput("crp", "C 反应蛋白", ModelInput.InputType.CONTINUOUS, 0.0, 600.0, "mg/L", "勘误后单位 mg/L"),
        ModelInput("sex", "性别（出生）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "男 1 / 女 0", negativeLabel = "女", positiveLabel = "男"),
        ModelInput("consciousnessImpaired", "意识受损（GCS<15）", ModelInput.InputType.BINARY, 0.0, 1.0, "", "格拉斯哥昏迷评分 <15"),
        ModelInput(
            key = "comorbidityCount", label = "合并症数（改良 Charlson+肥胖）", type = ModelInput.InputType.CATEGORICAL,
            min = 0.0, max = 2.0, unit = "", hint = "按改良 Charlson 与肥胖计数",
            options = comorbOptions,
        ),
    )

    override val disclaimers = listOf(
        "适用：成人（≥18 岁）住院确诊 COVID 的院内死亡风险分层（入院 24h 内数据）",
        "儿童（<18 岁）请使用 Wong2022 儿童重症 COVID 模型；门诊患者与非 COVID 感染不适用",
        "四组死亡率为 2020 验证队列绝对值；当代流行株+疫苗时代绝对值必已漂移（分层相对排序稳健，多国外部验证持续支持）",
        "吸氧患者：SpO₂ 须取室内空气值，否则高估安全性",
        "操作点语义：≤3 分排除死亡（Se 99.7% / NPV 98.8%）；≥15 分确认高危（Sp 89.8% / PPV 61.5%）",
        "尿素与 CRP 为勘误（BMJ m4334）后版本：切点 <7 mmol/L、CRP 单位 mg/L",
        "评分结果不构成治疗决策依据",
    )

    // ===== 分档函数（模型卡第 4 节，边界语义显式；T6–T10 锁死）=====
    private fun agePoints(age: Int): Int = when {
        age < 50 -> 0; age < 60 -> 2; age < 70 -> 4; age < 80 -> 6; else -> 7
    }
    private fun rrPoints(rr: Int): Int = when {
        rr < 20 -> 0; rr < 30 -> 1; else -> 2
    }
    private fun spo2Points(spo2: Int): Int = if (spo2 < 92) 2 else 0
    private fun ureaPoints(urea: Double): Int = when {
        urea < 7.0 -> 0; urea <= 14.0 -> 1; else -> 3
    }
    private fun crpPoints(crp: Double): Int = when {
        crp < 50.0 -> 0; crp < 100.0 -> 1; else -> 2
    }

    override fun assess(values: List<Double>): AssessmentResult {
        require(values.size == inputs.size) { "输入数量不符：期望 ${inputs.size}，实得 ${values.size}" }
        // 年龄域前置校验（18 岁以下消息指向儿童模型）
        val age = values[0]
        if (age < 18) throw IllegalArgumentException("「年龄」仅限成人（≥18 岁）；儿童新冠请使用 Wong2022 模型")
        inputs.forEachIndexed { i, input ->
            input.validate(values[i])?.let { throw IllegalArgumentException(it) }
        }
        val rr = values[1]; val spo2 = values[2]; val urea = values[3]; val crp = values[4]
        val male = values[5]; val impaired = values[6]; val comorb = values[7]

        val total = agePoints(age.toInt()) + rrPoints(rr.toInt()) + spo2Points(spo2.toInt()) +
            ureaPoints(urea) + crpPoints(crp) +
            male.toInt() + (impaired * 2).toInt() + comorb.toInt()

        val band: String; val mort: String; val opLine: String
        when {
            total <= LOW_MAX -> { band = "低风险"; mort = "1.2%"; opLine = "排除线：≤3 分排除死亡（Se 99.7% / NPV 98.8%）" }
            total <= MID_MAX -> { band = "中风险"; mort = "9.9%"; opLine = "" }
            total <= HIGH_MAX -> { band = "高风险"; mort = "31.4%"; opLine = "" }
            else -> { band = "极高风险"; mort = "61.5%"; opLine = "确认线：≥15 分确认高危（Sp 89.8% / PPV 61.5%）" }
        }
        val details = mutableListOf(
            "模型" to "4C Knight2020（PMID 32907855）",
            "组内死亡率（2020 验证队列绝对值，当代相对排序仍稳健）" to mort,
        )
        if (opLine.isNotEmpty()) details.add("操作点" to opLine)
        details.add("证据等级" to "时序验证 22 361 例（AUROC 0.767）；多国外部验证")
        details.add("校准" to "slope 1.0 / CITL 0 / Brier 0.171")

        return AssessmentResult(
            value = total.toDouble(),
            displayValue = "$total 分",
            bandLabel = band,
            bandExplanation = "总分 $total（0–21）。验证队列组内死亡率 $mort（2020 原始株时代绝对值，当代相对排序仍稳健）。" +
                if (opLine.isNotEmpty()) opLine else "",
            details = details,
        )
    }
}
