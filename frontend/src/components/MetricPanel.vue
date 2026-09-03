<template>
  <article class="metric-panel">
    <div class="metric-heading">
      <div>
        <span class="eyebrow">{{ title }}</span>
        <h3>{{ metric.categoryCount }} 个正频类别 · {{ metric.totalOccurrences }} 次标注</h3>
      </div>
      <span :class="['status-pill', statusClass]">{{ statusText }}</span>
    </div>
    <div class="metric-grid">
      <div><span>Entropy</span><strong>{{ number(metric.entropy) }}</strong></div>
      <div><span>Normalized</span><strong>{{ number(metric.normalizedEntropy) }}</strong></div>
      <div><span>IR</span><strong>{{ number(metric.imbalanceRatio) }}</strong></div>
      <div><span>Gini</span><strong>{{ number(metric.gini) }}</strong></div>
    </div>
    <p class="metric-note">
      判定策略：IR ≥ {{ metric.imbalanceRatioThreshold }} {{ metric.thresholdMode }}
      Gini ≥ {{ metric.giniThreshold }}
    </p>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  metric: { type: Object, required: true }
})

const statusText = computed(() => {
  if (!props.metric.applicable) return '不适用'
  return props.metric.significantlyImbalanced ? '显著不均衡' : '未达显著阈值'
})

const statusClass = computed(() => {
  if (!props.metric.applicable) return 'neutral'
  return props.metric.significantlyImbalanced ? 'danger' : 'good'
})

function number(value) {
  return value === null || value === undefined ? 'N/A' : Number(value).toFixed(4)
}
</script>
