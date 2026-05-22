<script setup lang="ts">
import { CircleDot, Droplets, Home, MapPin, MessageCircle, Sparkles, Stethoscope, Sun } from "lucide-vue-next"
import AnalysisCard from "@/components/AnalysisCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"
import { getLastAnalysis } from "@/lib/skinai"

const result = getLastAnalysis()
const icons = [Droplets, Sun, CircleDot]
</script>

<template>
  <AppHeader title="분석 결과" show-back />
  <PageContainer :has-bottom-nav="false">
    <section v-if="!result" class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h2 class="mb-2 text-lg font-semibold">분석 결과가 없습니다</h2>
      <p class="mb-6 text-sm text-muted-foreground">피부 사진을 촬영하고 분석을 시작해 주세요.</p>
      <RouterLink to="/capture"><BaseButton>분석하기</BaseButton></RouterLink>
    </section>

    <template v-else>
      <section class="py-6">
        <div class="rounded-3xl border border-border bg-card p-6 shadow-sm">
          <div class="mb-6 flex items-center justify-between">
            <div>
              <p class="mb-1 text-sm text-muted-foreground">{{ result.date || "오늘 분석" }}</p>
              <h2 class="mb-1 text-xl font-bold">{{ result.skinType || "피부 타입 분석" }}</h2>
              <p class="text-sm text-muted-foreground">AI 분석 결과를 기반으로 추천을 정리했습니다.</p>
            </div>
            <ScoreRing :score="result.overallScore || 0" :size="100" :stroke-width="7" />
          </div>
          <div class="flex flex-wrap gap-2">
            <span v-for="concern in result.concerns || []" :key="concern" class="rounded-full bg-accent px-3 py-1.5 text-xs font-medium">{{ concern }}</span>
          </div>
        </div>
      </section>

      <section class="py-4">
        <h3 class="mb-4 text-lg font-semibold">추천 시술</h3>
        <div class="space-y-3">
          <div v-for="treatment in result.treatments || []" :key="treatment.name" class="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div class="mb-3 flex items-start justify-between gap-3">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                  <Stethoscope class="h-5 w-5 text-primary" />
                </div>
                <div>
                  <h4 class="font-semibold">{{ treatment.name }}</h4>
                  <p class="text-sm text-muted-foreground">{{ treatment.reason }}</p>
                </div>
              </div>
              <span class="rounded-full bg-success/10 px-3 py-1 text-xs font-semibold text-success">{{ treatment.match }}</span>
            </div>
            <p class="rounded-xl bg-secondary px-4 py-3 text-sm">{{ treatment.note }}</p>
          </div>
        </div>
      </section>

      <section class="py-4">
        <h3 class="mb-4 text-lg font-semibold">피부 지표</h3>
        <div class="space-y-3">
          <AnalysisCard v-for="(metric, i) in result.metrics || []" :key="metric.id" :icon="icons[i % icons.length]" icon-color="text-primary" icon-bg="bg-primary/10" :title="metric.title" :subtitle="metric.description" :value="metric.score" :value-label="metric.status" />
        </div>
      </section>

      <section class="py-4">
        <h3 class="mb-4 text-lg font-semibold">관리 추천</h3>
        <div class="rounded-2xl bg-secondary p-5">
          <ul class="space-y-3">
            <li v-for="(recommendation, index) in result.recommendations || []" :key="recommendation" class="flex items-start gap-3">
              <span class="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">{{ index + 1 }}</span>
              <span class="text-sm">{{ recommendation }}</span>
            </li>
          </ul>
        </div>
      </section>

      <section class="space-y-3 py-6">
        <RouterLink to="/hospitals" class="block">
          <BaseButton size="lg" class="h-14 w-full rounded-2xl">
            <MapPin class="h-5 w-5" />
            주변 병원 찾기
          </BaseButton>
        </RouterLink>
        <RouterLink to="/chat" class="block">
          <BaseButton variant="outline" size="lg" class="h-14 w-full rounded-2xl">
            <MessageCircle class="h-5 w-5" />
            AI 상담하기
          </BaseButton>
        </RouterLink>
        <RouterLink to="/" class="block">
          <BaseButton variant="ghost" size="lg" class="h-12 w-full rounded-xl">
            <Home class="h-4 w-4" />
            홈으로
          </BaseButton>
        </RouterLink>
      </section>
    </template>
  </PageContainer>
</template>
