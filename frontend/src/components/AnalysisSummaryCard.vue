<script setup lang="ts">
import { CircleDot, Droplets, Sun } from "lucide-vue-next"
import { computed } from "vue"
import { scoreBgColor, scoreColor, type AnalysisMetric } from "@/lib/skinai"

const props = defineProps<{
  overallScore: number
  skinType: string
  mainConcern: string
  metrics?: AnalysisMetric[]
  hydration?: number
  sebum?: number
  pores?: number
}>()

function avgByPrefix(prefix: string): number {
  const matched = (props.metrics ?? []).filter((m) => m.id.startsWith(prefix)).map((m) => m.score)
  return matched.length ? Math.round(matched.reduce((a, b) => a + b, 0) / matched.length) : 0
}

function scoreById(id: string): number {
  return (props.metrics ?? []).find((m) => m.id === id)?.score ?? 0
}

const details = computed(() => {
  if (props.metrics?.length) {
    return [
      { label: "색소", score: avgByPrefix("pigment"), icon: Droplets },
      { label: "주름", score: avgByPrefix("wrinkle"), icon: Sun },
      { label: "피부결", score: scoreById("homogenity_texture") || avgByPrefix("homogenity"), icon: CircleDot },
    ]
  }
  return [
    { label: "색소", score: props.hydration ?? 0, icon: Droplets },
    { label: "주름", score: props.sebum ?? 0, icon: Sun },
    { label: "피부결", score: props.pores ?? 0, icon: CircleDot },
  ]
})
</script>

<template>
  <div class="rounded-2xl border border-border bg-card p-4 shadow-sm">
    <div class="mb-4 flex items-center gap-3">
      <div :class="['flex h-14 w-14 items-center justify-center rounded-full', scoreBgColor(overallScore)]">
        <span :class="['text-xl font-bold', scoreColor(overallScore)]">{{ overallScore }}</span>
      </div>
      <div>
        <p class="text-sm font-medium text-foreground">{{ skinType }} 피부</p>
        <p class="text-xs text-muted-foreground">{{ mainConcern }}</p>
      </div>
    </div>
    <div class="grid grid-cols-3 gap-3">
      <div v-for="item in details" :key="item.label" class="flex flex-col items-center gap-1 rounded-xl bg-secondary p-2">
        <component :is="item.icon" class="h-4 w-4 text-muted-foreground" />
        <span class="text-xs text-muted-foreground">{{ item.label }}</span>
        <span :class="['text-sm font-semibold', scoreColor(item.score)]">{{ item.score }}</span>
      </div>
    </div>
  </div>
</template>
