<template>
  <article class="chart-card">
    <div class="chart-title">
      <div>
        <span class="eyebrow">CATEGORY DISTRIBUTION</span>
        <h3>{{ title }}</h3>
      </div>
      <span v-if="afterCounts" class="compare-label">Before / After</span>
    </div>
    <div ref="chartElement" class="chart"></div>
  </article>
</template>

<script setup>
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

echarts.use([BarChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  title: { type: String, required: true },
  beforeCounts: { type: Array, default: () => [] },
  afterCounts: { type: Array, default: null }
})

const chartElement = ref(null)
let chart

function asMap(items) {
  return new Map((items || []).map(item => [item.type, item.count]))
}

function render() {
  if (!chartElement.value) return
  if (!chart) chart = echarts.init(chartElement.value)
  const before = asMap(props.beforeCounts)
  const after = props.afterCounts ? asMap(props.afterCounts) : null
  const labels = [...new Set([...before.keys(), ...(after ? after.keys() : [])])].sort()
  const series = [{
    name: after ? 'Before' : 'Count',
    type: 'bar',
    data: labels.map(label => before.get(label) || 0),
    itemStyle: { color: '#2563eb', borderRadius: [5, 5, 0, 0] }
  }]
  if (after) {
    series.push({
      name: 'After',
      type: 'bar',
      data: labels.map(label => after.get(label) || 0),
      itemStyle: { color: '#10b981', borderRadius: [5, 5, 0, 0] }
    })
  }
  chart.setOption({
    animationDuration: 450,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: after ? { top: 0 } : { show: false },
    grid: { top: after ? 42 : 20, left: 44, right: 18, bottom: 64 },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 5 ? 25 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series
  }, true)
}

function resize() {
  chart?.resize()
}

onMounted(() => {
  nextTick(render)
  window.addEventListener('resize', resize)
})
watch(() => [props.beforeCounts, props.afterCounts], () => nextTick(render), { deep: true })
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
</script>
