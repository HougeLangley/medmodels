package com.hermes.medmodels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hermes.medmodels.model.AssessmentModel
import com.hermes.medmodels.model.AssessmentResult
import com.hermes.medmodels.model.EptAssets
import com.hermes.medmodels.model.ModelRegistry
import com.hermes.medmodels.ui.ModelFormScreen
import com.hermes.medmodels.ui.ModelListScreen
import com.hermes.medmodels.ui.ResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EptAssets.init(this)  // EPT 大表资产（JME 撤药二进制）注入 Context
        enableEdgeToEdge()
        setContent {
            MedModelsApp()
        }
    }
}

/** 导航状态机：列表 → 表单 → 结果 */
@Composable
fun MedModelsApp() {
    var currentModel by remember { mutableStateOf<AssessmentModel?>(null) }
    var result by remember { mutableStateOf<AssessmentResult?>(null) }

    val model = currentModel
    when {
        result != null && model != null -> {
            // 侧滑/系统返回：结果页 → 表单页（与左上角箭头同一路径）
            BackHandler { result = null }
            ResultScreen(
                model = model,
                result = result!!,
                onBack = { result = null },
                onRetry = { result = null },
            )
        }
        model != null -> {
            // 侧滑/系统返回：表单页 → 模型列表（与左上角箭头同一路径）
            BackHandler { currentModel = null }
            ModelFormScreen(
                model = model,
                onBack = { currentModel = null },
                onSubmit = { values ->
                    runCatching { model.assess(values) }
                        .onSuccess { result = it }
                        .onFailure { /* 校验已在表单层拦截，此处兜底 */ }
                },
            )
        }
        else -> ModelListScreen(
            models = ModelRegistry.all,
            onModelClick = { currentModel = it },
        )
    }
}
