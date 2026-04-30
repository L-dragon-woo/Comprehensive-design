<script setup lang="ts">
import { ArrowRight, Camera, ChevronRight, Clock, MessageCircle, Shield, Sparkles, TrendingUp } from "lucide-vue-next"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import FeatureCard from "@/components/FeatureCard.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"

const hasRecentAnalysis = true
const recentScore = 78
const lastAnalysisDate = "2026.04.30"
</script>

<template>
  <AppHeader show-logo show-notification />
  <PageContainer>
    <section class="py-8 md:py-12">
      <div class="mb-3 flex items-center gap-2">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          <Sparkles class="h-5 w-5 text-primary" />
        </div>
        <span class="text-sm font-semibold text-primary">AI 피부 분석</span>
      </div>

      <h1 class="mb-3 text-2xl font-bold leading-tight text-foreground md:text-3xl">
        사진 한 장으로<br />
        <span class="text-primary">피부 상태</span>를 확인하세요
      </h1>

      <p class="mb-8 text-base leading-relaxed text-muted-foreground">
        AI가 피부 고민을 분석하고<br class="md:hidden" />
        맞춤 솔루션을 제안해 드려요
      </p>

      <RouterLink to="/capture" class="block">
        <BaseButton size="lg" class="h-14 w-full rounded-2xl text-base font-semibold shadow-lg shadow-primary/25 hover:scale-[1.02] active:scale-[0.98]">
          <Camera class="h-5 w-5" />
          피부 분석 시작하기
          <ArrowRight class="h-4 w-4" />
        </BaseButton>
      </RouterLink>
    </section>

    <section v-if="hasRecentAnalysis" class="py-6">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-foreground">최근 분석 결과</h2>
        <RouterLink to="/history" class="flex items-center gap-1 text-sm font-medium text-primary hover:underline">
          전체보기
          <ChevronRight class="h-4 w-4" />
        </RouterLink>
      </div>

      <RouterLink to="/result" class="group block">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:border-primary/30 hover:shadow-md">
          <div class="flex items-center gap-5">
            <ScoreRing :score="recentScore" :size="80" :stroke-width="6" />
            <div class="flex-1">
              <p class="mb-1 text-sm text-muted-foreground">{{ lastAnalysisDate }} 분석</p>
              <p class="mb-2 text-lg font-semibold text-foreground">종합 피부 점수</p>
              <div class="flex items-center gap-2">
                <span class="inline-flex items-center rounded-full bg-success/10 px-2 py-0.5 text-xs font-medium text-success">
                  <TrendingUp class="mr-1 h-3 w-3" />
                  +5점
                </span>
                <span class="text-xs text-muted-foreground">지난 분석 대비</span>
              </div>
            </div>
            <ChevronRight class="h-5 w-5 text-muted-foreground transition-colors group-hover:text-primary" />
          </div>
        </div>
      </RouterLink>
    </section>

    <section class="py-6">
      <h2 class="mb-4 text-lg font-semibold text-foreground">SkinAI로 할 수 있는 것</h2>
      <div class="grid grid-cols-2 gap-3">
        <FeatureCard href="/capture" :icon="Camera" title="피부 촬영" description="AI가 정확하게 분석해요" />
        <FeatureCard href="/chat" :icon="MessageCircle" icon-color="text-success" icon-bg="bg-success/10" title="AI 상담" description="궁금한 점을 물어보세요" />
        <FeatureCard href="/history" :icon="TrendingUp" icon-color="text-warning" icon-bg="bg-warning/10" title="변화 추적" description="피부 변화를 기록해요" />
        <FeatureCard href="/history" :icon="Clock" icon-color="text-muted-foreground" icon-bg="bg-muted" title="분석 기록" description="지난 결과를 확인해요" />
      </div>
    </section>

    <section class="pb-8 pt-6">
      <div class="rounded-2xl bg-secondary p-5">
        <div class="flex items-start gap-4">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-card">
            <Shield class="h-5 w-5 text-primary" />
          </div>
          <div>
            <h3 class="mb-1 text-base font-semibold text-foreground">안전한 데이터 관리</h3>
            <p class="text-sm leading-relaxed text-muted-foreground">
              사진과 분석 결과는 암호화되어 안전하게 보관되며, 오직 본인만 확인할 수 있어요.
            </p>
          </div>
        </div>
      </div>
    </section>
  </PageContainer>
  <BottomNav />
</template>
