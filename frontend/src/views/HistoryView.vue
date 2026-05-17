<script setup lang="ts">
import { ArrowUpRight, Calendar, Minus, Sparkles, TrendingDown, TrendingUp } from "lucide-vue-next"
import { computed, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { historyData, type AnalysisRecord } from "@/lib/skinai"

const filter = ref<"all" | "month" | "week">("all")
const latestRecord = historyData[0]
const filteredData = computed(() => {
  return historyData.filter((record) => {
    if (filter.value === "all") return true
    const recordDate = new Date(record.date)
    const now = new Date()
    if (filter.value === "week") {
      return recordDate >= new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    }
    return recordDate >= new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
  })
})

const chartData = [...historyData].reverse()
const maxScore = Math.max(...chartData.map((item) => item.score))
const minScore = Math.min(...chartData.map((item) => item.score))
const range = maxScore - minScore || 1
const width = 100
const height = 60
const padding = 10
const points = chartData.map((record, index) => ({
  x: padding + (index / (chartData.length - 1)) * (width - padding * 2),
  y: height - padding - ((record.score - minScore) / range) * (height - padding * 2),
}))
const pathD = points.reduce((path, point, index) => index === 0 ? `M ${point.x} ${point.y}` : `${path} L ${point.x} ${point.y}`, "")

function filterLabel(value: "all" | "month" | "week") {
  if (value === "all") return "전체"
  if (value === "month") return "이번 달"
  return "이번 주"
}

function changeClass(record: AnalysisRecord) {
  if (record.change > 0) return "bg-primary-foreground/20 text-primary-foreground"
  if (record.change < 0) return "bg-destructive/30 text-primary-foreground"
  return "bg-primary-foreground/10 text-primary-foreground/80"
}
</script>

<template>
  <AppHeader title="분석 기록" show-back show-notification />
  <PageContainer>
    <section class="py-6">
      <div class="rounded-2xl bg-gradient-to-br from-primary to-primary/80 p-5 text-primary-foreground shadow-lg">
        <div class="mb-4 flex items-start justify-between">
          <div>
            <p class="mb-1 text-sm opacity-90">최근 분석일</p>
            <p class="text-lg font-semibold">{{ latestRecord.dateFormatted }}</p>
          </div>
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary-foreground/20">
            <Calendar class="h-5 w-5" />
          </div>
        </div>
        <div class="flex items-end justify-between">
          <div>
            <p class="mb-1 text-sm opacity-90">최근 피부 점수</p>
            <div class="flex items-baseline gap-2">
              <span class="text-4xl font-bold tabular-nums">{{ latestRecord.score }}</span>
              <span class="text-lg opacity-90">점</span>
            </div>
          </div>
          <div class="text-right">
            <p class="mb-1 text-sm opacity-90">지난 분석 대비</p>
            <div :class="['inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-sm font-semibold', changeClass(latestRecord)]">
              <TrendingUp v-if="latestRecord.change > 0" class="h-4 w-4" />
              <TrendingDown v-else-if="latestRecord.change < 0" class="h-4 w-4" />
              <Minus v-else class="h-4 w-4" />
              {{ latestRecord.change > 0 ? `+${latestRecord.change}점` : latestRecord.change < 0 ? `${latestRecord.change}점` : "변동없음" }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="pb-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold text-foreground">피부 점수 변화</h3>
          <span class="text-xs text-muted-foreground">최근 5회 분석</span>
        </div>
        <div class="relative h-40">
          <svg :viewBox="`0 0 ${width} ${height}`" class="h-32 w-full" preserveAspectRatio="none">
            <defs>
              <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="var(--color-primary)" stop-opacity="0.3" />
                <stop offset="100%" stop-color="var(--color-primary)" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path :d="`${pathD} L ${points[points.length - 1].x} ${height - padding} L ${points[0].x} ${height - padding} Z`" fill="url(#scoreGradient)" />
            <path :d="pathD" fill="none" stroke="var(--color-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <circle v-for="(point, index) in points" :key="index" :cx="point.x" :cy="point.y" r="3" fill="var(--color-card)" stroke="var(--color-primary)" stroke-width="2" />
          </svg>
          <div class="mt-2 flex justify-between px-2">
            <span v-for="record in chartData" :key="record.id" class="text-xs tabular-nums text-muted-foreground">
              {{ record.dateFormatted.slice(5) }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <section class="pb-6">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-lg font-semibold text-foreground">날짜별 분석 기록</h3>
        <div class="flex items-center gap-1 rounded-xl bg-muted p-1">
          <button
            v-for="item in ['all', 'month', 'week'] as const"
            :key="item"
            :class="['rounded-lg px-3 py-1.5 text-xs font-medium transition-all', filter === item ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground']"
            @click="filter = item"
          >
            {{ filterLabel(item) }}
          </button>
        </div>
      </div>

      <div class="space-y-3">
        <div v-for="record in filteredData" :key="record.id" class="rounded-2xl border border-border bg-card p-4 shadow-sm transition-all hover:border-primary/30 hover:shadow-md">
          <div class="flex items-start gap-4">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-secondary">
              <Calendar class="h-5 w-5 text-muted-foreground" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="mb-2 flex items-center gap-2">
                <p class="text-base font-semibold text-foreground">{{ record.dateFormatted }}</p>
                <span :class="['inline-flex items-center gap-0.5 text-xs font-semibold', record.change > 0 ? 'text-success' : record.change < 0 ? 'text-destructive' : 'text-muted-foreground']">
                  <TrendingUp v-if="record.change > 0" class="h-3 w-3" />
                  <TrendingDown v-else-if="record.change < 0" class="h-3 w-3" />
                  <Minus v-else class="h-3 w-3" />
                  {{ record.change > 0 ? `+${record.change}점` : record.change < 0 ? `${record.change}점` : "변동없음" }}
                </span>
              </div>
              <div class="mb-3 flex items-baseline gap-1">
                <span class="text-2xl font-bold tabular-nums text-foreground">{{ record.score }}</span>
                <span class="text-sm text-muted-foreground">점</span>
              </div>
              <div class="mb-3">
                <p class="mb-1.5 text-xs text-muted-foreground">주요 개선 항목</p>
                <div class="flex flex-wrap gap-1.5">
                  <span v-for="item in record.improvements" :key="item" class="inline-flex items-center rounded-full bg-accent px-2.5 py-1 text-xs font-medium text-accent-foreground">
                    {{ item }}
                  </span>
                </div>
              </div>
              <RouterLink :to="`/result?id=${record.id}`">
                <BaseButton variant="outline" size="sm" class="w-full rounded-xl text-sm font-medium hover:border-primary hover:bg-primary hover:text-primary-foreground">
                  상세 보기
                  <ArrowUpRight class="h-4 w-4" />
                </BaseButton>
              </RouterLink>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredData.length === 0" class="py-12 text-center">
        <p class="text-muted-foreground">해당 기간에 분석 기록이 없어요</p>
      </div>
    </section>

    <div v-if="historyData.length === 0" class="py-20 text-center">
      <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
        <Sparkles class="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 class="mb-2 text-lg font-semibold text-foreground">아직 분석 기록이 없어요</h3>
      <p class="mb-6 text-sm text-muted-foreground">첫 번째 피부 분석을 시작해보세요</p>
      <RouterLink to="/capture">
        <BaseButton class="rounded-xl">피부 분석 시작하기</BaseButton>
      </RouterLink>
    </div>
  </PageContainer>
  <BottomNav />
</template>
