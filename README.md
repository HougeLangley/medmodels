# MedModels · 医学评估模型工具箱

> 把发表的医学预测模型（列线图 / 评分量表）变成临床医生口袋里的计算器。
> 论文 → 系数核验 → 锚点测试 → Android 应用——每一步可追溯、可审计。

![版本](https://img.shields.io/badge/version-0.9.1-blue) ![模型数](https://img.shields.io/badge/模型-13-green) ![单测](https://img.shields.io/badge/锚点单测-138%20全绿-brightgreen) ![平台](https://img.shields.io/badge/Android-8.0%2B-orange) ![语言](https://img.shields.io/badge/界面-中文-red)

---

## 这是什么

**MedModels** 是一个 Android 端医学预测模型容器。它把经文献评估收录的医学预测模型——Logistic 列线图、风险评分量表——逐个实现为「输入化验/体征 → 输出风险概率」的床旁计算器,按疾病门类(呼吸系统/感染相关/神经系统/重症 ICU)组织,供临床医生在查房、门诊、交接班时快速调用。

本项目**不是**医疗器械,不用于诊断或治疗决策;它是文献学习与临床分层的辅助工具(详见下方「使用限度」)。

## 内置模型(13 个,全部经六项数值核验)

### 呼吸系统(5)

| 模型 | 用途 | 输入 | 证据 |
|---|---|---|---|
| **Yang2026** RMPP 风险预测 | 难治性支原体肺炎患儿进展为 RMPP 的风险 | 热程/体温/LDH/痰栓/实变/低氧 | JoVE 2026,单中心 500 例 |
| **Chen2026** SMPP 重症预测 | 儿童 MPP 入院 24h 重症(需激素)风险 | 发热天数/气促/大环内酯无反应/IL-6/肺实变 | Front Pediatr 2026,时间外部验证 |
| **Li2025** 坏死性肺炎风险 | RMPP 进展为坏死性肺炎(NP)的风险 | 发热天数/WBC/IL-6/D-二聚体 | Ital J Pediatr 2025,536 例,验证 AUC 0.920 |
| **Servitje2026** cPPE 复杂化预测 | 儿童肺炎旁胸腔积液进展为复杂性的风险 | 年龄/胸壁凹陷/CRP≥176.5/积液厚度/纵隔移位 | Pediatr Pulmonol 2026,122 例 |
| **Na2026** PB 取栓后复发 | MPP 并发塑形性支气管炎取栓后复发风险 | 白蛋白/发热≥72h/肺不张/铸型≥2 叶 | Front Pediatr 2026,双中心 352 例 |

### 感染相关(3)

| 模型 | 用途 | 输入 | 证据 |
|---|---|---|---|
| **Li2026** RSV 重症预测 | 婴幼儿 RSV 感染重症风险 | NLR/病程/月龄/冬季/低氧/早产 | JIR 2026,2 595 例 |
| **Wong2022** 儿童重症 COVID | 住院儿童重症/危重 COVID 风险 | 婴儿/合并症/惊厥/呕吐/发热/卡他/ANC/Hb/PLT | PLOS ONE 2022,五中心四国 |
| **Knight2020** 4C 死亡评分 | 成人住院 COVID 死亡风险 | 年龄/性别/合并症数/RR/SpO₂/意识/尿素/CRP | BMJ 2020,57 924 例,被引 800+ |

### 神经系统(3)

| 模型 | 用途 | 输入 | 证据 |
|---|---|---|---|
| **Boonluksiri2015** DRE 复发评分 | 儿童癫痫缓解期停药后复发风险 | 年龄/缺陷类型/EEG/癫痫样放电 | J Epilepsy Res 2015 |
| **Geng2022** DRE 早期预测 | 儿童药物难治性癫痫早期识别 | 起病年龄/SE/局灶/发作>20 次/EEG/神经缺陷/病因 | Front Pediatr 2022,外部验证 |
| **Chen2026** SEPE 量表 | 成人耐药性癫痫预后评估 | 家族史/智力/发作控制/频率/时相/耐药时间窗 | BMC Neurol 2026 |

### 重症/ICU(2)

| 模型 | 用途 | 输入 | 证据 |
|---|---|---|---|
| **Guo2026** 新冠脓毒症死亡 | ICU 成人新冠脓毒症院内死亡风险 | 年龄/心率/CRP/淋巴%/乳酸/氧合指数 | Sci Rep 2026,MIMIC-IV 外部验证 |
| **Guo2026** 非新冠脓毒症死亡 | ICU 成人非新冠脓毒症院内死亡风险 | 年龄/心率/K/Na/LDH/PT/D-dimer/乳酸 | Sci Rep 2026,MIMIC-IV 外部验证 |

*注:上表中「证据」列为文献身份,完整性能白名单、适用人群、限制声明见应用内各模型详情页与源码注释。*

## 收录论文完整题录(12 篇 → 13 个模型)

本应用收录的每一个模型均实现自以下公开发表的医学研究。原文版权归各作者所有,本仓库仅实现其发表的算法,引用以学术惯例为准。

| # | 模型 | 论文题目 | 期刊 / DOI / PMID |
|---|---|---|---|
| 1 | Yang2026 | A Web-Based Dynamic Nomogram for Predicting Refractory *Mycoplasma pneumoniae* Pneumonia in Children | J Vis Exp 2026;(233) · 10.3791/71588 · 42612067 |
| 2 | Li2026 | A Predictive Nomogram for Severe RSV Infection in Children: A Retrospective, Single-Center Development and Validation Study | J Inflamm Res 2026;19:1652 · 10.2147/JIR.S580432 · 42007299 |
| 3 | Boonluksiri2015 | Clinical Prediction Rule of Drug Resistant Epilepsy in Children | J Epilepsy Res 2015;5(2):84–88 · 10.14581/jer.15014 · 26819940 |
| 4 | Geng2022 | Development and validation of a nomogram for the early prediction of drug resistance in children with epilepsy | Front Pediatr 2022;10:905177 · 10.3389/fped.2022.905177 · 36110106 |
| 5 | Chen2026 | The scale for estimating prognosis of epilepsy to predict in patients with epilepsy: a 506-patient cohort study | BMC Neurol 2026;26:183 · 10.1186/s12883-026-04666-2 · 41699518 |
| 6 | Wong2022 | Development and validation of a clinical predictive model for severe and critical pediatric COVID-19 infection | PLOS ONE 2022;17(10):e0275761 · 10.1371/journal.pone.0275761 · 36301941 |
| 7 | Knight2020 (4C) | Risk stratification of patients admitted to hospital with covid-19 using the ISARIC WHO Clinical Characterisation Protocol: development and validation of the 4C Mortality Score | BMJ 2020;370:m3339 · 10.1136/bmj.m3339 · 32907855 |
| 8 | Guo2026(新冠脓毒症 + 非新冠脓毒症,一文双模型) | Pathogens analysis and modeling of mortality risk in sepsis patients with COVID-19 and without COVID-19 | Sci Rep 2026 · 10.1038/s41598-026-58450-w · 42298051 |
| 9 | Na2026 | Development and validation of a nomogram for predicting recurrent plastic bronchitis in children with *Mycoplasma pneumoniae* pneumonia | Front Pediatr 2026;14:1959849 · 10.3389/fped.2026.1959849 · 42780927 |
| 10 | Li2025 | Development and validation of a nomogram for predicting necrotizing pneumonia in children with refractory *Mycoplasma pneumoniae* pneumonia | Ital J Pediatr 2025;51:150 · 10.1186/s13052-025-02006-7 · 40399899 |
| 11 | Servitje2026 | Early Identification of Children at Risk for Complicated Parapneumonic Pleural Effusion: Development of a Prediction Model | Pediatr Pulmonol 2026;61(9):e71817 · 10.1002/ppul.71817 · 42669040 |
| 12 | Chen2026 (SMPP) | Development and validation of a nomogram for stratifying severe *Mycoplasma pneumoniae* pneumonia risk in children early | Front Pediatr 2026;14:1927137 · 10.3389/fped.2026.1927137 · 42890885 |

## 为什么做这个

临床医生读文献时常见痛点:论文里的列线图很漂亮,但真到床旁要用时——列线图印刷分度读不准、手算 logistic 公式不现实、作者在线计算器常已失效。而大部分这类模型止步于论文,从未变成可用的工具。

本项目的做法:**每篇论文经六项数值核验(β=ln(OR) 自洽性、CI 反推 Wald 检验、worked example 复现、概率轴像素级拟合、患病率交叉、单位核验)后,以「模型卡」冻结常量,再用 Kotlin 实现,配锚点单元测试**——保证 App 里跑的每一个数字都能追溯到论文的某一张表、某一个图或某一句公式。

典型工作流(以 Li2025 为例,全文可考):

```
论文(Ital J Pediatr 2025;51:150)
 ├─ 原文只印了 OR(1.475/1.149/1.007/1.361),没印 β 和截距
 ├─ 列线图 worked example:发热 10 天=28 分 + WBC 17.5=22 分 + IL-6 300=18 分 + D-dimer 6=12 分 → 80 分 → p=0.82
 ├─ 概率轴 11 刻度像素级拟合:logit(p) = −6.891 + 0.10606×总分(残差 RMS 0.011)
 ├─ β 链反标定:βᵢ = 分值ᵢ × γ / xᵢ → 0.297/0.133/0.00636/0.212,全部落入印刷 OR 的 95% CI
 └─ 模型卡冻结常量 → Kotlin object → 10 个锚点单测 → 入 App
```

## 如何开发(贡献新模型)

本项目核心纪律:**模型卡 = 代码常量 = 单测锚点,三处一致,改参数先改卡**。

1. **文献评估**:找一篇候选论文,按六项核验检查系数链完整性。收录门槛:PubMed 可查、全文可得、系数完备(β+截距或可无歧义重建)、临床价值、数值核验全过
2. **建模型卡**:冻结常量(系数/输入域/风险带/性能白名单/限制条款/测试锚点 T1–Tn,五位小数)
3. **实现**:新建 `model/XxxYyyy.kt` 实现 `AssessmentModel` 接口(id/displayName/category/inputs/disclaimers/assess),注册进 `ModelRegistry` 一行
4. **锚点测试**:`test/.../XxxYyyyTest.kt`,按模型卡锚点表逐条断言(容差 ±0.005);贴阈值锚点只做概率断言不做带位断言
5. **构建装机**:`./build.sh` → `adb install -r` → 真机验收

架构极简:无导航库、无数据库、无网络权限。`MainActivity` 状态机(列表→表单→结果) + 动态表单(`ModelInput` 定义控件) + 结果页(概率/评分双模式、四带色带、验后概率块、人群横幅)。新模型 = 一个 Kotlin object + 一行注册,UI 自动适配。

### 构建方法

```bash
git clone <本仓库>
cd medmodels
./build.sh                    # 自动探测 JDK17+,组装 debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

依赖 Android SDK 36、JDK 17+。Gradle 8.14.3 自管(仓库自带 gradlew 与下载器),依赖走阿里云镜像(中国大陆网络友好,海外用户可改 `settings.gradle.kts`)。签名构建需在项目根放 `keystore.properties`(格式见 `app/build.gradle.kts` 顶部注释块逻辑,样例:storeFile/storePassword/keyAlias/keyPassword),签名材料不入库。

## 使用说明

1. 打开 App,按疾病门类浏览可折叠的模型列表
2. 点选模型 → 阅读顶部人群横幅(适用人群提示) → 按提示输入变量(注意单位 hint)
3. 提交 → 结果页:概率/评分 + 风险带色卡 + 声明行(如截距重建/β 反标定) + 性能白名单 + 限制条款
4. 左上角箭头或侧滑返回上一级

**输入纪律**:化验值注意单位(WBC ×10⁹/L、IL-6 pg/mL、D-dimer mg/L、白蛋白 g/L);开关类变量按定义时点勾选;域外值会被拦截并提示。

## 使用限度(必读)

1. **本 App 不是医疗器械**。仅用于文献学习、教学演示与临床风险分层讨论,不可作为诊断、治疗决策的唯一依据
2. **所有模型均为文献复现**,性能数字( AUC/Se/Sp) 是原队列表现,在你的人群中未必重现;校准漂移普遍存在
3. **风险带切点多为工程划定**(锚=训练患病率),原文未提供操作点性能,不可当作临床决策阈值
4. **多个模型含工程重建成分**:Na2026 截距重建(±0.3)、Li2025 β 反标定、Geng2022 截距反推——结果页有声明行,绝对概率解读需谨慎,建议以序贯比较(较基线变化)为主
5. **时代局限性**:COVID 模型基于 2020–21 毒株时代;脓毒症模型外部验证人群死亡率谱偏轻;所有单中心模型外推需谨慎
6. **不适用场景**:已确诊目标结局者(模型用于发生前)、人群外(儿童模型用于成人等)、变量时点不符(如取栓后模型用于取栓前)

## 目前缺陷(诚实清单)

- **无数据持久化**:退出即清空,无历史记录(在计划中)
- **无列线图可视化**:纯数值界面,缺少图形化风险沟通(icon arrays 在计划中)
- **单侧滑无确认**:表单页侧滑返回会直接丢弃已填数据
- **无多语言**:仅中文
- **无 CI**:测试靠本地 `./build.sh` 跑
- **模型卡与源码分离**:文档工程在私有 Obsidian 库,开源仓库只含代码与 README(社区贡献者需在 PR 中自证系数链)
- **成人模型仅 3 个**:门类覆盖不均(免疫/风湿、消化、循环等门类空白)
- **模型时点绑定**:变量取自特定临床时点(入院/取栓后),跨时点使用无警告

## 免责声明

本软件按「现状」提供,不附带任何明示或默示的保证。使用者应为受过相应训练的医务人员,自行承担临床判断责任。模型原始研究版权归各原作者,本仓库仅实现其发表之算法,引用方式以学术惯例为准。

---

## 支持开发

如果这个项目对你有帮助,欢迎请作者喝杯咖啡 ☕

![收款码](docs/donate.png)

(扫码支持,不胜感激——模型评估、系数核验与锚点测试都是慢工出细活。)
