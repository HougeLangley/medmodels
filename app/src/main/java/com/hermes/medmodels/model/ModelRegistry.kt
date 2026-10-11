package com.hermes.medmodels.model

/**
 * 模型注册表：App 的全部模型在此注册。
 * 新增模型：实现 [AssessmentModel] 后在此加一行。
 */
object ModelRegistry {
    val all: List<AssessmentModel> = listOf(
        Yang2026Rmpp,
        Li2026RsvSevere,
        Boonluksiri2015Dre,
        Geng2022DreEarly,
        Chen2026Sepe,
        Wong2022CovidSevere,
        Knight2020FourC,
        Guo2026SepsisCovid,
        Guo2026SepsisNonCovid,
        Na2026PbRecurrence,
        Li2025RmppNecrotizing,
        Servitje2026Cppe,
        // 新模型加在这里
    )

    fun byId(id: String): AssessmentModel? = all.firstOrNull { it.id == id }
}
