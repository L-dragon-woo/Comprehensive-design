<script setup lang="ts">
import { Activity, CircleDot, Droplets, Stethoscope, Sun } from "lucide-vue-next"
import { computed } from "vue"
import { scoreBgColor, scoreColor, type AnalysisMetric } from "@/lib/skinai"

const props = defineProps<{
  overallScore: number
  skinType: string
  mainConcern: string
  metrics?: AnalysisMetric[]
  concerns?: string[]
  treatments?: Array<{ name: string; match: string; reason: string; note: string }>
  imageDataUrl?: string
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

// 가장 심각한(점수 낮은) 지표 top 4 (age 제외)
const topConcernMetrics = computed(() => {
  const ms = (props.metrics ?? []).filter((m) => m.id !== "age" && m.category !== "종합")
  return [...ms].sort((a, b) => a.score - b.score).slice(0, 4)
})

const summaryMetrics = computed(() => {
  if (props.metrics?.length) {
    return [
      { label: "색소", score: avgByPrefix("pigment"), icon: Droplets },
      { label: "주름", score: avgByPrefix("wrinkle"), icon: Activity },
      { label: "피부결", score: scoreById("homogenity_texture") || avgByPrefix("homogenity"), icon: CircleDot },
    ]
  }
  return [
    { label: "색소", score: props.hydration ?? 0, icon: Droplets },
    { label: "주름", score: props.sebum ?? 0, icon: Sun },
    { label: "피부결", score: props.pores ?? 0, icon: CircleDot },
  ]
})

const topTreatments = computed(() => (props.treatments ?? []).slice(0, 2))
const displayConcerns = computed(() => (props.concerns ?? []).slice(0, 4))
</script>

<template>
  <div class="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
    <!-- 헤더: 점수 + 피부 타입 -->
    <div class="flex items-center gap-4 p-4 pb-3">
      <img v-if="imageDataUrl" :src="imageDataUrl" alt="분석 얼굴 사진" class="h-14 w-14 shrink-0 rounded-2xl object-cover ring-1 ring-border" />
      <div v-else :class="['flex h-14 w-14 shrink-0 items-center justify-center rounded-full', scoreBgColor(overallScore)]">
        <span :class="['text-xl font-bold', scoreColor(overallScore)]">{{ overallScore }}</span>
      </div>
      <div class="min-w-0 flex-1">
        <p class="text-sm font-semibold text-foreground">{{ skinType }} 피부</p>
        <p class="truncate text-xs text-muted-foreground">{{ mainConcern }}</p>
      </div>
    </div>

    <!-- 관심사 태그 -->
    <div v-if="displayConcerns.length" class="flex flex-wrap gap-1.5 px-4 pb-3">
      <span v-for="c in displayConcerns" :key="c" class="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">{{ c }}</span>
    </div>

    <!-- 3대 지표 요약 -->
    <div class="grid grid-cols-3 gap-2 px-4 pb-3">
      <div v-for="item in summaryMetrics" :key="item.label" class="flex flex-col items-center gap-1 rounded-xl bg-secondary p-2">
        <component :is="item.icon" class="h-4 w-4 text-muted-foreground" />
        <span class="text-xs text-muted-foreground">{{ item.label }}</span>
        <span :class="['text-sm font-bold', scoreColor(item.score)]">{{ item.score }}</span>
      </div>
    </div>

    <!-- 심각 부위 목록 (상세 지표 있을 때) -->
    <template v-if="topConcernMetrics.length">
      <div class="border-t border-border px-4 py-3">
        <p class="mb-2 text-xs font-medium text-muted-foreground">집중 관리 부위</p>
        <div class="space-y-2">
          <div v-for="m in topConcernMetrics" :key="m.id" class="flex items-center gap-2">
            <span class="w-28 shrink-0 truncate text-xs text-foreground">{{ m.title }}</span>
            <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
              <div
                :class="['h-full rounded-full', m.score >= 80 ? 'bg-success' : m.score >= 60 ? 'bg-primary' : m.score >= 40 ? 'bg-warning' : 'bg-destructive']"
                :style="{ width: `${m.score}%` }"
              />
            </div>
            <span :class="['w-8 shrink-0 text-right text-xs font-semibold', scoreColor(m.score)]">{{ m.score }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 추천 시술 (있을 때) -->
    <template v-if="topTreatments.length">
      <div class="border-t border-border px-4 py-3">
        <p class="mb-2 text-xs font-medium text-muted-foreground">추천 시술</p>
        <div class="space-y-1.5">
          <div v-for="t in topTreatments" :key="t.name" class="flex items-center gap-2">
            <Stethoscope class="h-3.5 w-3.5 shrink-0 text-primary" />
            <span class="text-xs font-medium">{{ t.name }}</span>
            <span class="truncate text-xs text-muted-foreground">· {{ t.reason }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
