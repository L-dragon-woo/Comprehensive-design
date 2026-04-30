<script setup lang="ts">
import { CircleDot, Download, Droplets, Home, MessageCircle, Share2, Sparkles, Sun } from "lucide-vue-next"
import AnalysisCard from "@/components/AnalysisCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"

const skinAnalysisData = {
  overallScore: 78,
  date: "2026년 4월 30일",
  skinType: "복합성",
  metrics: [
    { id: "hydration", icon: Droplets, iconColor: "text-primary", iconBg: "bg-primary/10", title: "수분", score: 72, status: "보통", description: "피부 수분이 약간 부족해요" },
    { id: "sebum", icon: Sun, iconColor: "text-warning", iconBg: "bg-warning/10", title: "유분", score: 65, status: "주의", description: "T존 유분이 과다해요" },
    { id: "pores", icon: CircleDot, iconColor: "text-success", iconBg: "bg-success/10", title: "모공", score: 85, status: "좋음", description: "모공 상태가 양호해요" },
    { id: "pigmentation", icon: CircleDot, iconColor: "text-destructive", iconBg: "bg-destructive/10", title: "색소침착", score: 68, status: "보통", description: "볼 부근에 색소침착이 있어요" },
  ],
  concerns: ["T존 유분 과다", "볼 색소침착", "수분 부족"],
  recommendations: ["아침 세안 후 수분 토너 사용", "자외선 차단제 꼼꼼히 바르기", "주 2회 각질 케어 추천"],
}
</script>

<template>
  <AppHeader title="분석 결과" show-back />
  <PageContainer :has-bottom-nav="false">
    <section class="py-6">
      <div class="rounded-3xl border border-border bg-card p-6 shadow-sm">
        <div class="mb-6 flex items-center justify-between">
          <div class="flex-1">
            <p class="mb-1 text-sm text-muted-foreground">{{ skinAnalysisData.date }}</p>
            <h2 class="mb-1 text-xl font-bold text-foreground">종합 피부 점수</h2>
            <p class="text-sm text-muted-foreground">{{ skinAnalysisData.skinType }} 피부</p>
          </div>
          <ScoreRing :score="skinAnalysisData.overallScore" :size="100" :stroke-width="7" />
        </div>
        <div class="flex flex-wrap gap-2">
          <span v-for="concern in skinAnalysisData.concerns" :key="concern" class="inline-flex items-center rounded-full bg-accent px-3 py-1.5 text-xs font-medium text-accent-foreground">
            {{ concern }}
          </span>
        </div>
      </div>
    </section>

    <section class="py-4">
      <h3 class="mb-4 text-lg font-semibold text-foreground">상세 분석</h3>
      <div class="space-y-3">
        <AnalysisCard
          v-for="metric in skinAnalysisData.metrics"
          :key="metric.id"
          :icon="metric.icon"
          :icon-color="metric.iconColor"
          :icon-bg="metric.iconBg"
          :title="metric.title"
          :subtitle="metric.description"
          :value="metric.score"
          :value-label="metric.status"
        />
      </div>
    </section>

    <section class="py-4">
      <h3 class="mb-4 text-lg font-semibold text-foreground">AI 추천 케어</h3>
      <div class="rounded-2xl bg-secondary p-5">
        <div class="mb-4 flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-card">
            <Sparkles class="h-5 w-5 text-primary" />
          </div>
          <div>
            <h4 class="text-base font-semibold text-foreground">맞춤 케어 솔루션</h4>
            <p class="text-sm text-muted-foreground">분석 결과를 바탕으로 추천드려요</p>
          </div>
        </div>
        <ul class="space-y-3">
          <li v-for="(rec, index) in skinAnalysisData.recommendations" :key="rec" class="flex items-start gap-3">
            <span class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">{{ index + 1 }}</span>
            <span class="text-sm leading-relaxed text-foreground">{{ rec }}</span>
          </li>
        </ul>
      </div>
    </section>

    <section class="space-y-3 py-6">
      <RouterLink to="/chat" class="block">
        <BaseButton size="lg" class="h-14 w-full rounded-2xl text-base font-semibold shadow-lg shadow-primary/20">
          <MessageCircle class="h-5 w-5" />
          AI에게 더 물어보기
        </BaseButton>
      </RouterLink>
      <div class="flex gap-3">
        <BaseButton variant="outline" size="lg" class="h-12 flex-1 rounded-xl">
          <Download class="h-4 w-4" />
          저장하기
        </BaseButton>
        <BaseButton variant="outline" size="lg" class="h-12 flex-1 rounded-xl">
          <Share2 class="h-4 w-4" />
          공유하기
        </BaseButton>
      </div>
      <RouterLink to="/" class="block">
        <BaseButton variant="ghost" size="lg" class="h-12 w-full rounded-xl text-muted-foreground">
          <Home class="h-4 w-4" />
          홈으로 돌아가기
        </BaseButton>
      </RouterLink>
    </section>
  </PageContainer>
</template>
