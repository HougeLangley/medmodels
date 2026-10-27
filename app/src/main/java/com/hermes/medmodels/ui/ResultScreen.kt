package com.hermes.medmodels.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.medmodels.model.AssessmentModel
import com.hermes.medmodels.model.AssessmentResult

/** 结果页：大号主值 + 风险带 + 明细 + 免责（评分制模型附验后概率块） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    model: AssessmentModel,
    result: AssessmentResult,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val highRisk = result.bandLabel.contains("高")
    val midRisk = result.bandLabel.contains("中")
    val ultraRisk = result.bandLabel.contains("极高")
    var priorChoice by remember(result) { mutableStateOf(result.posterior?.defaultPrior ?: 0.30) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("评估结果", fontSize = 16.sp) },
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        ultraRisk -> MaterialTheme.colorScheme.errorContainer       // 极高危：深红
                        highRisk -> Color(0xFFD32F2F).copy(alpha = 0.12f)          // 高危：红
                        midRisk -> Color(0xFFFFF3E0)                               // 中危：浅橙
                        else -> MaterialTheme.colorScheme.primaryContainer         // 低危：蓝
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        result.bandLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            ultraRisk -> MaterialTheme.colorScheme.onErrorContainer
                            highRisk -> Color(0xFFB71C1C)
                            midRisk -> Color(0xFF7A4F00)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result.displayValue,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            ultraRisk -> MaterialTheme.colorScheme.onErrorContainer
                            highRisk -> Color(0xFFB71C1C)
                            midRisk -> Color(0xFF7A4F00)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result.valueLabel
                            ?: (if (result.posterior != null) "DRE 风险评分" else "预测概率（RMPP 风险）"),
                        fontSize = 12.sp,
                        color = when {
                            highRisk -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            midRisk -> Color(0xFF7A4F00).copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        },
                    )
                }
            }

            // 多输出模型的次要输出（EPT 表型：5y/10y 等同屏陈列）
            if (result.extraOutputs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    result.extraOutputs.forEach { (label, value) ->
                        Card(modifier = Modifier.weight(1f)) {
                            Column(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 验后概率块（仅评分制 LR 模型显示；D2：整数 + 先验可调 + 永久脚注）
            result.posterior?.let { post ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("验后概率估算（工程换算）", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val postProb = post.likelihoodRatio * (priorChoice / (1.0 - priorChoice)) /
                            (1.0 + post.likelihoodRatio * (priorChoice / (1.0 - priorChoice)))
                        Text(
                            "≈ ${Math.round(postProb * 100)}%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "该档 +LR ${post.likelihoodRatio} × 基线风险 ${Math.round(priorChoice * 100)}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("基线风险（DRE 先验）：", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            post.priorChoices.forEach { choice ->
                                FilterChip(
                                    selected = priorChoice == choice,
                                    onClick = { priorChoice = choice },
                                    label = { Text("${Math.round(choice * 100)}%") },
                                )
                            }
                        }
                        Text(
                            "⚠️ ${post.footnote}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // 适用人群重复行（D5a：结果页风险带下方一行）
            model.audienceNotice?.let { notice ->
                Text(
                    "👥 适用人群：$notice",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("结果解读", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(result.bandExplanation, fontSize = 13.sp)
                    result.details.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(k, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(v, fontSize = 13.sp)
                        }
                    }
                }
            }

            Text("⚠️ 使用限制", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            model.disclaimers.forEach {
                Text("· $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Replay, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重新填写")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
