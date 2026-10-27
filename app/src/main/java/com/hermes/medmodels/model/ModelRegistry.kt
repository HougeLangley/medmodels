package com.hermes.medmodels.model

/**
 * 模型注册表：App 的全部模型在此注册。
 * 新增模型：实现 [AssessmentModel] 后在此加一行。
 */
object ModelRegistry {
    val all: List<AssessmentModel> = listOf(
        // 呼吸系统（病程谱：RMPP→SMPP→坏死→cPPE→PB 复发）
        Yang2026Rmpp,
        Chen2026Smpp,
        Li2025RmppNecrotizing,
        Servitje2026Cppe,
        Na2026PbRecurrence,
        // 感染相关
        Li2026RsvSevere,
        Wong2022CovidSevere,
        Knight2020FourC,
        // 神经系统（病程谱：首发复发→首诊确诊→DRE→预后→撤药→术后→认知→JME）
        Boonluksiri2015Dre,
        VanDiessen2018Fsc,
        Geng2022DreEarly,
        Chen2026Sepe,
        Lamberink2017Aed,
        Lamberink2018Tts,
        Cloppenborg2022Iq85,
        Cloppenborg2022Iq70,
        Cloppenborg2022Dq50,
        Stevelink2022JmeResistance,
        Stevelink2022JmeWithdrawal,
        // 重症/ICU
        Guo2026SepsisCovid,
        Guo2026SepsisNonCovid,
        // 新模型加在这里
    )

    fun byId(id: String): AssessmentModel? = all.firstOrNull { it.id == id }
}
