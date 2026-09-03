<template>
  <div class="app-shell">
    <header class="hero">
      <nav>
        <span class="brand-mark">TH</span>
        <span class="brand-text">Text Hospital · Balance Lab</span>
        <a href="#workspace">在线演示</a>
      </nav>
      <div class="hero-content">
        <div>
          <p class="hero-kicker">文本数据医院 · 独立公开展示版</p>
          <h1>看见类别偏斜，<br /><span>用可复现治理验证变化。</span></h1>
          <p class="hero-copy">
            面向文本标注数据的均衡性诊断与 Random Resampling 演示。
            数据仅在内存中处理，不使用数据库，也不会写入服务器磁盘。
          </p>
          <a class="primary-link" href="#workspace">开始诊断</a>
        </div>
        <div class="formula-card">
          <span class="eyebrow">DIAGNOSTIC SIGNALS</span>
          <div><strong>H(X)</strong><span>Shannon Entropy</span></div>
          <div><strong>H<sub>norm</sub></strong><span>Normalized Entropy</span></div>
          <div><strong>IR</strong><span>max count / min positive count</span></div>
          <div><strong>G</strong><span>Gini coefficient</span></div>
        </div>
      </div>
    </header>

    <main>
      <section class="intro-grid">
        <article>
          <span class="step-number">01</span>
          <h2>诊断</h2>
          <p>统计实体与关系类型，分别计算熵、归一化熵、IR 和 Gini。</p>
        </article>
        <article>
          <span class="step-number">02</span>
          <h2>判断</h2>
          <p>通过可配置的 AND / OR 阈值策略判断是否达到显著不均衡条件。</p>
        </article>
        <article>
          <span class="step-number">03</span>
          <h2>治理</h2>
          <p>复制含目标类型的完整样本，用 randomSeed 保证选择过程可复现。</p>
        </article>
      </section>

      <section id="workspace" class="workspace">
        <div class="section-heading">
          <div>
            <span class="eyebrow">INTERACTIVE WORKSPACE</span>
            <h2>数据输入与阈值</h2>
          </div>
          <div class="sample-actions">
            <button class="secondary" :disabled="busy" @click="loadSynthetic('imbalanced')">加载不均衡示例</button>
            <button class="ghost" :disabled="busy" @click="loadSynthetic('balanced')">加载均衡示例</button>
          </div>
        </div>

        <div class="input-layout">
          <div class="editor-card">
            <div class="editor-toolbar">
              <span>dataset.json</span>
              <label class="file-button">
                选择 JSON 文件
                <input type="file" accept="application/json,.json" @change="readFile" />
              </label>
            </div>
            <textarea v-model="datasetText" spellcheck="false" placeholder="粘贴 JSON 数组或加载人工合成示例"></textarea>
          </div>

          <aside class="settings-card">
            <span class="eyebrow">THRESHOLDS</span>
            <label>
              <span>IR 阈值</span>
              <input v-model.number="thresholds.imbalanceRatioThreshold" type="number" min="0" step="0.1" />
            </label>
            <label>
              <span>Gini 阈值</span>
              <input v-model.number="thresholds.giniThreshold" type="number" min="0" max="1" step="0.05" />
            </label>
            <label>
              <span>组合策略</span>
              <select v-model="thresholds.thresholdMode">
                <option value="AND">AND：两个指标都达到</option>
                <option value="OR">OR：任一指标达到</option>
              </select>
            </label>
            <button class="primary" :disabled="busy || !datasetText.trim()" @click="analyze">
              {{ analyzing ? '正在分析…' : '运行均衡性诊断' }}
            </button>
            <p class="privacy-note">请求体不会被持久化。刷新页面即清空当前结果。</p>
          </aside>
        </div>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      </section>

      <section v-if="analysis" class="results-section">
        <div class="section-heading">
          <div>
            <span class="eyebrow">DIAGNOSTIC RESULT</span>
            <h2>{{ analysis.sampleCount }} 条样本的诊断结果</h2>
          </div>
          <span class="result-note">Normalized Entropy 仅作分布参考</span>
        </div>

        <div class="metric-panels">
          <MetricPanel title="实体类型" :metric="analysis.entity" />
          <MetricPanel title="关系类型" :metric="analysis.relation" />
        </div>

        <div class="chart-grid">
          <DistributionChart
            title="实体类别数量"
            :before-counts="governance ? governance.before.entity.countsAscending : analysis.entity.countsAscending"
            :after-counts="governance ? governance.after.entity.countsAscending : null"
          />
          <DistributionChart
            title="关系类别数量"
            :before-counts="governance ? governance.before.relation.countsAscending : analysis.relation.countsAscending"
            :after-counts="governance ? governance.after.relation.countsAscending : null"
          />
        </div>

        <div class="optional-grid">
          <article v-for="item in optionalDistributions" :key="item.title" class="optional-card">
            <span class="eyebrow">OPTIONAL DISTRIBUTION</span>
            <h3>{{ item.title }}</h3>
            <strong>{{ distributionStatus(item.value.status) }}</strong>
            <p>{{ item.value.reason }} · 已映射 {{ item.value.mappedSampleCount }}/{{ item.value.sampleCount }}</p>
          </article>
        </div>
      </section>

      <section v-if="analysis" class="governance-section">
        <div class="section-heading">
          <div>
            <span class="eyebrow">RANDOM RESAMPLING</span>
            <h2>设置治理目标</h2>
          </div>
        </div>

        <div class="governance-layout">
          <div class="governance-form">
            <label>
              <span>治理对象</span>
              <select v-model="governanceForm.targetKind">
                <option value="ENTITY">实体类型</option>
                <option value="RELATION">关系类型</option>
              </select>
            </label>
            <label>
              <span>目标类型</span>
              <select v-model="governanceForm.targetType">
                <option v-for="item in targetOptions" :key="item.type" :value="item.type">
                  {{ item.type }}（当前 {{ item.count }}）
                </option>
              </select>
            </label>
            <label>
              <span>目标最终出现次数</span>
              <input v-model.number="governanceForm.targetCount" type="number" min="1" step="1" />
            </label>
            <label>
              <span>randomSeed</span>
              <input v-model="governanceForm.randomSeed" type="number" min="0" step="1" placeholder="留空则由后端生成" />
            </label>
            <button class="primary" :disabled="governing || !governanceForm.targetType" @click="govern">
              {{ governing ? '正在治理…' : '执行 Random Resampling' }}
            </button>
          </div>

          <div class="governance-explain">
            <h3>治理规则</h3>
            <ol>
              <li>筛选包含目标类型的候选样本。</li>
              <li>按 randomSeed 驱动伪随机选择。</li>
              <li>深拷贝完整样本并生成唯一 ID。</li>
              <li>重新统计所有指标，展示 Before / After。</li>
            </ol>
            <p>一条样本可能含多个目标标注，因此最终次数可能略高于目标值。</p>
          </div>
        </div>
      </section>

      <section v-if="governance" class="comparison-section">
        <div class="section-heading">
          <div>
            <span class="eyebrow">BEFORE / AFTER</span>
            <h2>治理结果对比</h2>
          </div>
          <button class="secondary" @click="downloadGovernedJson">下载治理后 JSON</button>
        </div>

        <div class="summary-strip">
          <div><span>状态</span><strong>{{ governance.status }}</strong></div>
          <div><span>治理前目标次数</span><strong>{{ governance.beforeTargetCount }}</strong></div>
          <div><span>治理后目标次数</span><strong>{{ governance.actualTargetCount }}</strong></div>
          <div><span>新增样本</span><strong>{{ governance.copiedSampleCount }}</strong></div>
          <div><span>Overshoot</span><strong>{{ governance.overshoot }}</strong></div>
          <div><span>randomSeed</span><strong>{{ governance.randomSeed }}</strong></div>
        </div>

        <div class="comparison-table-wrap">
          <table>
            <thead>
              <tr><th>对象</th><th>指标</th><th>Before</th><th>After</th></tr>
            </thead>
            <tbody>
              <template v-for="kind in ['entity', 'relation']" :key="kind">
                <tr v-for="metric in comparisonMetrics" :key="`${kind}-${metric.key}`">
                  <td>{{ kind === 'entity' ? '实体' : '关系' }}</td>
                  <td>{{ metric.label }}</td>
                  <td>{{ displayMetric(governance.before[kind][metric.key]) }}</td>
                  <td>{{ displayMetric(governance.after[kind][metric.key]) }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <p class="warning-note">{{ governance.warning }}</p>
      </section>

      <section class="schema-section">
        <div>
          <span class="eyebrow">INPUT CONTRACT</span>
          <h2>最小数据格式</h2>
          <p><code>ner</code> 元组第 4 项为实体类型，<code>relations</code> 元组第 5 项为关系类型。</p>
        </div>
        <pre><code>[
  {
    "id": "sample-01",
    "ner": [[0, 4, "text", "ENTITY_TYPE"]],
    "relations": [[0, 4, 8, 12, "RELATION_TYPE"]],
    "source": "optional-source",
    "materialSystem": "optional-system"
  }
]</code></pre>
      </section>
    </main>

    <footer>
      <strong>Text Hospital · Balance Public</strong>
      <span>多人协作项目中个人负责模块的独立、脱敏展示版本。</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, reactive, ref, watch } from 'vue'
import MetricPanel from './components/MetricPanel.vue'
import { analyzeDataset, governDataset, loadExample } from './services/api'

const DistributionChart = defineAsyncComponent(() => import('./components/DistributionChart.vue'))

const datasetText = ref('')
const analysis = ref(null)
const governance = ref(null)
const analyzing = ref(false)
const governing = ref(false)
const errorMessage = ref('')

const thresholds = reactive({
  imbalanceRatioThreshold: 10,
  giniThreshold: 0.4,
  thresholdMode: 'AND'
})

const governanceForm = reactive({
  targetKind: 'ENTITY',
  targetType: '',
  targetCount: 1,
  randomSeed: '42'
})

const busy = computed(() => analyzing.value || governing.value)
const targetOptions = computed(() => {
  if (!analysis.value) return []
  const key = governanceForm.targetKind === 'ENTITY' ? 'entity' : 'relation'
  return analysis.value[key].countsAscending || []
})
const optionalDistributions = computed(() => [
  { title: '来源分布', value: analysis.value.source },
  { title: '材料体系分布', value: analysis.value.material }
])
const comparisonMetrics = [
  { key: 'normalizedEntropy', label: 'Normalized Entropy' },
  { key: 'imbalanceRatio', label: 'IR' },
  { key: 'gini', label: 'Gini' },
  { key: 'significantlyImbalanced', label: '显著不均衡' }
]

watch(() => governanceForm.targetKind, selectGovernanceDefault)

async function loadSynthetic(name) {
  clearError()
  analyzing.value = true
  try {
    const dataset = await loadExample(name)
    datasetText.value = JSON.stringify(dataset, null, 2)
    await runAnalysis(dataset)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    analyzing.value = false
  }
}

async function analyze() {
  clearError()
  analyzing.value = true
  try {
    await runAnalysis(parseDataset())
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    analyzing.value = false
  }
}

async function runAnalysis(dataset) {
  validateThresholds()
  analysis.value = await analyzeDataset(dataset, thresholds)
  governance.value = null
  selectGovernanceDefault()
}

function selectGovernanceDefault() {
  const options = targetOptions.value
  governanceForm.targetType = options.length ? options[0].type : ''
  governanceForm.targetCount = options.length ? Math.max(...options.map(item => item.count)) : 1
}

async function govern() {
  clearError()
  governing.value = true
  try {
    validateThresholds()
    const seed = governanceForm.randomSeed === '' ? null : Number(governanceForm.randomSeed)
    if (seed !== null && (!Number.isSafeInteger(seed) || seed < 0)) {
      throw new Error('randomSeed 必须是非负的 JavaScript 安全整数')
    }
    if (!Number.isInteger(governanceForm.targetCount) || governanceForm.targetCount <= 0) {
      throw new Error('目标次数必须是正整数')
    }
    governance.value = await governDataset({
      dataset: parseDataset(),
      targetKind: governanceForm.targetKind,
      targetType: governanceForm.targetType,
      targetCount: governanceForm.targetCount,
      randomSeed: seed,
      ...thresholds
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    governing.value = false
  }
}

function parseDataset() {
  try {
    return JSON.parse(datasetText.value)
  } catch (error) {
    throw new Error(`JSON 解析失败：${error.message}`)
  }
}

function validateThresholds() {
  if (!Number.isFinite(thresholds.imbalanceRatioThreshold) || thresholds.imbalanceRatioThreshold < 0) {
    throw new Error('IR 阈值必须是非负数')
  }
  if (!Number.isFinite(thresholds.giniThreshold)
      || thresholds.giniThreshold < 0 || thresholds.giniThreshold > 1) {
    throw new Error('Gini 阈值必须位于 0 到 1 之间')
  }
}

async function readFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  clearError()
  try {
    const text = await file.text()
    const parsed = JSON.parse(text)
    datasetText.value = JSON.stringify(parsed, null, 2)
  } catch (error) {
    errorMessage.value = `文件读取失败：${error.message}`
  } finally {
    event.target.value = ''
  }
}

function downloadGovernedJson() {
  const blob = new Blob([JSON.stringify(governance.value.governedDataset, null, 2)], {
    type: 'application/json;charset=utf-8'
  })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'synthetic-governed-dataset.json'
  anchor.click()
  URL.revokeObjectURL(url)
}

function displayMetric(value) {
  if (value === null || value === undefined) return 'N/A'
  if (typeof value === 'boolean') return value ? '是' : '否'
  return typeof value === 'number' ? value.toFixed(4) : value
}

function distributionStatus(status) {
  return {
    AVAILABLE: '可计算',
    NOT_APPLICABLE: '单类别，不适用',
    NOT_AVAILABLE: '未提供映射',
    UNCOMPUTABLE: '映射不完整'
  }[status] || status
}

function clearError() {
  errorMessage.value = ''
}
</script>

<style>
:root {
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  color: #14213d;
  background: #f4f7fb;
  font-synthesis: none;
}

* { box-sizing: border-box; }
html { scroll-behavior: smooth; }
body { margin: 0; min-width: 320px; }
button, input, select, textarea { font: inherit; }
button, a { transition: transform .18s ease, opacity .18s ease, background .18s ease; }
button:not(:disabled):hover, .primary-link:hover { transform: translateY(-1px); }
button:disabled { opacity: .55; cursor: not-allowed; }

.app-shell { min-height: 100vh; }
.hero { color: white; background: radial-gradient(circle at 82% 10%, #275bb0 0, transparent 38%), linear-gradient(135deg, #071a35, #0d3164 70%, #164e8d); padding: 0 6vw 76px; }
nav { max-width: 1180px; margin: 0 auto; height: 76px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid rgba(255,255,255,.13); }
nav a { margin-left: auto; color: #dbeafe; text-decoration: none; }
.brand-mark { width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; background: #2dd4bf; color: #07223f; font-weight: 900; }
.brand-text { font-weight: 700; letter-spacing: .02em; }
.hero-content { max-width: 1180px; margin: 70px auto 0; display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, .65fr); gap: 70px; align-items: center; }
.hero-kicker, .eyebrow { color: #38bdf8; font-size: 12px; font-weight: 800; letter-spacing: .16em; text-transform: uppercase; }
.hero h1 { margin: 15px 0 22px; font-size: clamp(42px, 6vw, 76px); line-height: 1.05; letter-spacing: -.045em; }
.hero h1 span { color: #5eead4; }
.hero-copy { max-width: 690px; color: #c6d8f1; font-size: 18px; line-height: 1.8; }
.primary-link { display: inline-block; margin-top: 20px; padding: 13px 22px; border-radius: 10px; background: #2dd4bf; color: #08213c; font-weight: 800; text-decoration: none; }
.formula-card { background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.16); border-radius: 22px; padding: 25px; backdrop-filter: blur(10px); }
.formula-card > div { display: flex; align-items: baseline; gap: 18px; padding: 17px 0; border-bottom: 1px solid rgba(255,255,255,.12); }
.formula-card > div:last-child { border: 0; }
.formula-card strong { min-width: 78px; color: #5eead4; font: 700 25px Georgia, serif; }
.formula-card span:not(.eyebrow) { color: #dbeafe; }

main { max-width: 1180px; margin: 0 auto; padding: 0 24px 80px; }
.intro-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; margin-top: -34px; overflow: hidden; border-radius: 18px; box-shadow: 0 18px 60px rgba(20, 33, 61, .13); }
.intro-grid article { background: white; padding: 30px; }
.step-number { color: #0f766e; font-size: 13px; font-weight: 900; }
.intro-grid h2 { margin: 9px 0; }
.intro-grid p { margin: 0; color: #62708a; line-height: 1.65; }
.workspace, .results-section, .governance-section, .comparison-section, .schema-section { padding-top: 82px; }
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.section-heading h2, .schema-section h2 { margin: 7px 0 0; font-size: clamp(28px, 4vw, 40px); letter-spacing: -.03em; }
.sample-actions { display: flex; gap: 10px; flex-wrap: wrap; }
button { border: 0; border-radius: 9px; padding: 11px 16px; cursor: pointer; font-weight: 750; }
button.primary { width: 100%; color: white; background: #0f4c81; }
button.secondary { color: white; background: #0f766e; }
button.ghost { color: #164e63; background: #dff7f3; }
.input-layout { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(270px, .45fr); gap: 20px; }
.editor-card, .settings-card, .metric-panel, .chart-card, .optional-card, .governance-layout, .comparison-table-wrap { background: white; border: 1px solid #dce4ef; border-radius: 16px; box-shadow: 0 8px 28px rgba(36, 59, 83, .06); }
.editor-card { overflow: hidden; }
.editor-toolbar { height: 50px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; background: #eef3f9; color: #52627a; font: 700 13px ui-monospace, monospace; }
.file-button { cursor: pointer; color: #0f5c9c; }
.file-button input { display: none; }
textarea { display: block; width: 100%; min-height: 390px; border: 0; outline: 0; resize: vertical; padding: 18px; color: #dbeafe; background: #0d1b2f; line-height: 1.55; font: 13px/1.55 ui-monospace, SFMono-Regular, Consolas, monospace; }
.settings-card { padding: 24px; }
.settings-card label, .governance-form label { display: grid; gap: 7px; margin: 18px 0; color: #52627a; font-size: 13px; font-weight: 700; }
input, select { width: 100%; border: 1px solid #cbd7e6; border-radius: 8px; padding: 10px 11px; color: #14213d; background: white; }
.privacy-note { color: #738299; font-size: 12px; line-height: 1.55; }
.error-message { padding: 13px 16px; color: #991b1b; background: #fee2e2; border-radius: 9px; }

.result-note, .compare-label { color: #0f766e; font-size: 13px; font-weight: 800; }
.metric-panels, .chart-grid, .optional-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.metric-panel { padding: 23px; }
.metric-heading { display: flex; align-items: start; justify-content: space-between; gap: 16px; }
.metric-heading h3, .chart-title h3, .optional-card h3 { margin: 7px 0 0; }
.status-pill { white-space: nowrap; border-radius: 99px; padding: 6px 9px; font-size: 12px; font-weight: 800; }
.status-pill.good { color: #047857; background: #d1fae5; }
.status-pill.danger { color: #b91c1c; background: #fee2e2; }
.status-pill.neutral { color: #475569; background: #e2e8f0; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 9px; margin-top: 22px; }
.metric-grid div { padding: 13px 10px; border-radius: 10px; background: #f2f6fb; }
.metric-grid span, .summary-strip span { display: block; color: #728199; font-size: 11px; margin-bottom: 5px; }
.metric-grid strong { font-size: 17px; }
.metric-note { color: #6b7890; font-size: 12px; margin: 16px 0 0; }
.chart-grid { margin-top: 20px; }
.chart-card { padding: 22px; }
.chart-title { display: flex; justify-content: space-between; }
.chart { height: 320px; margin-top: 12px; }
.optional-grid { margin-top: 20px; }
.optional-card { padding: 20px; }
.optional-card strong { display: block; margin-top: 17px; color: #0f766e; }
.optional-card p { color: #718096; margin-bottom: 0; font-size: 13px; }

.governance-layout { display: grid; grid-template-columns: 1fr 1fr; padding: 28px; gap: 54px; }
.governance-form { display: grid; grid-template-columns: repeat(2, 1fr); column-gap: 16px; }
.governance-form button { grid-column: 1 / -1; }
.governance-explain { border-left: 1px solid #dce4ef; padding-left: 40px; }
.governance-explain h3 { font-size: 24px; }
.governance-explain li, .governance-explain p { color: #607089; line-height: 1.7; }
.governance-explain li { margin-bottom: 8px; }
.summary-strip { display: grid; grid-template-columns: repeat(6, 1fr); gap: 1px; overflow: hidden; border-radius: 13px; margin-bottom: 18px; background: #dce4ef; }
.summary-strip div { background: white; padding: 18px 14px; }
.comparison-table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 13px 16px; border-bottom: 1px solid #e5eaf1; text-align: left; }
th { color: #52627a; background: #f4f7fb; font-size: 12px; }
.warning-note { color: #805c13; background: #fef3c7; border-radius: 9px; padding: 13px 16px; }
.schema-section { display: grid; grid-template-columns: .7fr 1.3fr; align-items: center; gap: 54px; }
.schema-section p { color: #607089; line-height: 1.7; }
pre { overflow-x: auto; padding: 24px; border-radius: 16px; color: #dbeafe; background: #0d1b2f; font: 13px/1.6 ui-monospace, monospace; }
footer { display: flex; justify-content: space-between; gap: 20px; padding: 26px max(24px, calc((100vw - 1132px) / 2)); color: #9db1ca; background: #071a35; font-size: 13px; }

@media (max-width: 900px) {
  .hero-content, .input-layout, .governance-layout, .schema-section { grid-template-columns: 1fr; }
  .intro-grid { grid-template-columns: 1fr; }
  .metric-panels, .chart-grid { grid-template-columns: 1fr; }
  .summary-strip { grid-template-columns: repeat(3, 1fr); }
  .governance-explain { border-left: 0; border-top: 1px solid #dce4ef; padding: 24px 0 0; }
}

@media (max-width: 600px) {
  .hero { padding-left: 22px; padding-right: 22px; }
  .hero-content { margin-top: 45px; }
  .hero h1 { font-size: 42px; }
  .brand-text { font-size: 13px; }
  main { padding-left: 14px; padding-right: 14px; }
  .section-heading { align-items: start; flex-direction: column; }
  .optional-grid, .governance-form { grid-template-columns: 1fr; }
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .summary-strip { grid-template-columns: repeat(2, 1fr); }
  footer { flex-direction: column; }
}
</style>
