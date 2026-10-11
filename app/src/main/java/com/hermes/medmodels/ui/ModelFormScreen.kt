package com.hermes.medmodels.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.medmodels.model.AssessmentModel
import com.hermes.medmodels.model.ModelInput

/** 表单页：按模型 inputs 定义动态渲染输入控件 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelFormScreen(
    model: AssessmentModel,
    onBack: () -> Unit,
    onSubmit: (List<Double>) -> Unit,
) {
    val inputStates = remember(model.id) {
        mutableStateListOf<MutableState<Double?>>().apply { model.inputs.forEach { add(mutableStateOf<Double?>(null)) } }
    }
    val binaryStates = remember(model.id) {
        mutableStateListOf<MutableState<Boolean?>>().apply { model.inputs.forEach { add(mutableStateOf<Boolean?>(null)) } }
    }
    // CATEGORICAL：记录每组选中选项的 key（null = 未选）
    val categoricalStates = remember(model.id) {
        mutableStateListOf<MutableState<String?>>().apply { model.inputs.forEach { add(mutableStateOf<String?>(null)) } }
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(model.displayName, fontSize = 16.sp, maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 适用人群提示条（D5a：固定顶部，不可折叠）
            model.audienceNotice?.let { notice ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "👥 $notice",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // 输入区
            model.inputs.forEachIndexed { index, input ->
                when (input.type) {
                    ModelInput.InputType.CONTINUOUS -> ContinuousField(
                        input = input,
                        value = inputStates[index].value,
                        onValueChange = { inputStates[index].value = it },
                    )
                    ModelInput.InputType.BINARY -> BinaryField(
                        input = input,
                        value = binaryStates[index].value,
                        onValueChange = { binaryStates[index].value = it },
                    )
                    ModelInput.InputType.CATEGORICAL -> CategoricalField(
                        input = input,
                        selectedKey = categoricalStates[index].value,
                        onSelect = { categoricalStates[index].value = it },
                    )
                }
            }

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val values = model.inputs.mapIndexed { i, input ->
                        when (input.type) {
                            ModelInput.InputType.CONTINUOUS -> inputStates[i].value
                            ModelInput.InputType.BINARY -> binaryStates[i].value?.let { if (it) 1.0 else 0.0 }
                            ModelInput.InputType.CATEGORICAL -> categoricalStates[i].value?.let { key ->
                                input.options.firstOrNull { it.key == key }?.score
                            }
                        }
                    }
                    // 逐项校验，报第一个错
                    val firstError = model.inputs.indices.firstNotNullOfOrNull { i ->
                        model.inputs[i].validate(values[i])
                    }
                    if (firstError != null) {
                        errorMsg = firstError
                    } else {
                        errorMsg = null
                        onSubmit(values.filterNotNull())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("计算评估结果", fontSize = 16.sp)
            }

            // 免责声明
            Text(
                "⚠️ 使用限制",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            model.disclaimers.forEach {
                Text("· $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContinuousField(
    input: ModelInput,
    value: Double?,
    onValueChange: (Double?) -> Unit,
) {
    var text by remember(input.key) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            onValueChange(raw.toDoubleOrNull())
        },
        label = { Text("${input.label}（${input.unit}）") },
        supportingText = { Text("${input.hint}｜范围 ${formatRange(input)} ${input.unit}") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BinaryField(
    input: ModelInput,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
) {
    Column {
        Text(input.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(input.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == false,
                onClick = { onValueChange(false) },
                label = { Text(input.negativeLabel) },
            )
            FilterChip(
                selected = value == true,
                onClick = { onValueChange(true) },
                label = { Text(input.positiveLabel) },
            )
        }
    }
}

@Composable
private fun CategoricalField(
    input: ModelInput,
    selectedKey: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(input.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(input.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            input.options.forEach { option ->
                FilterChip(
                    selected = selectedKey == option.key,
                    onClick = { onSelect(option.key) },
                    label = {
                        Text(
                            if (option.score == 0.0) option.label
                            else "${option.label} · ${if (option.score % 1.0 == 0.0) option.score.toInt() else option.score} 分"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun formatRange(input: ModelInput): String {
    return if (input.min == input.min.toInt().toDouble() && input.max == input.max.toInt().toDouble()) {
        "${input.min.toInt()}–${input.max.toInt()}"
    } else {
        "$input.min–$input.max"
    }
}
