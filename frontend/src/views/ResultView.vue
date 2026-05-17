<script setup lang="ts">
import { CircleDot, Download, Droplets, Home, MapPin, MessageCircle, Share2, Sparkles, Stethoscope, Sun } from "lucide-vue-next"
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
  treatments: [
    { name: "리쥬란 힐러", match: "추천", reason: "볼 건조와 피부결 개선 상담에 적합해요", note: "민감도와 통증 정도를 상담하세요" },
    { name: "피코토닝", match: "상담 권장", reason: "볼 부근 색소침착 완화 목적에 맞아요", note: "자외선 노출과 기미 여부 확인이 필요해요" },
    { name: "아쿠아필", match: "보조 추천", reason: "T존 유분과 모공 관리에 도움을 줄 수 있어요", note: "건조 부위 자극 여부를 확인하세요" },
  ],
  recommendations: ["시술 전 1주일은 강한 각질 케어 피하기", "상담 시 색소침착 부위와 민감도 공유하기", "시술 후 자외선 차단과 보습 계획 세우기"],
}
</script>

<template>
  <AppHeader title="시술 추천 결과" show-back />
  <PageContainer :has-bottom-nav="false">
    <section class="py-6">
      <div class="rounded-3xl border border-border bg-card p-6 shadow-sm">
        <div class="mb-6 flex items-center justify-between">
          <div class="flex-1">
            <p class="mb-1 text-sm text-muted-foreground">{{ skinAnalysisData.date }}</p>
            <h2 class="mb-1 text-xl font-bold text-foreground">추천 적합도</h2>
            <p class="text-sm text-muted-foreground">{{ skinAnalysisData.skinType }} 피부 · 상담 전 참고 리포트</p>
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
      <h3 class="mb-4 text-lg font-semibold text-foreground">추천 시술</h3>
      <div class="space-y-3">
        <div v-for="treatment in skinAnalysisData.treatments" :key="treatment.name" class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div class="mb-3 flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <Stethoscope class="h-5 w-5 text-primary" />
              </div>
              <div>
                <h4 class="text-base font-semibold text-foreground">{{ treatment.name }}</h4>
                <p class="text-sm text-muted-foreground">{{ treatment.reason }}</p>
              </div>
            </div>
            <span class="shrink-0 rounded-full bg-success/10 px-3 py-1 text-xs font-semibold text-success">{{ treatment.match }}</span>
          </div>
          <p class="rounded-xl bg-secondary px-4 py-3 text-sm leading-relaxed text-secondary-foreground">{{ treatment.note }}</p>
        </div>
      </div>
    </section>

    <section class="py-4">
      <h3 class="mb-4 text-lg font-semibold text-foreground">피부 지표</h3>
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
      <h3 class="mb-4 text-lg font-semibold text-foreground">상담 전 체크리스트</h3>
      <div class="rounded-2xl bg-secondary p-5">
        <div class="mb-4 flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-card">
            <Sparkles class="h-5 w-5 text-primary" />
          </div>
          <div>
            <h4 class="text-base font-semibold text-foreground">시술 상담 준비</h4>
            <p class="text-sm text-muted-foreground">추천 결과를 바탕으로 확인해보세요</p>
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
      <RouterLink to="/hospitals" class="block">
        <BaseButton size="lg" class="h-14 w-full rounded-2xl text-base font-semibold shadow-lg shadow-primary/20">
          <MapPin class="h-5 w-5" />
          근처 병원에 결과지 제출하기
        </BaseButton>
      </RouterLink>
      <RouterLink to="/chat" class="block">
        <BaseButton variant="outline" size="lg" class="h-14 w-full rounded-2xl text-base font-semibold">
          <MessageCircle class="h-5 w-5" />
          추천 시술 상담하기
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
