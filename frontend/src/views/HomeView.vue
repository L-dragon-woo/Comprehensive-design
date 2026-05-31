<script setup lang="ts">
import { ArrowRight, Camera, ChevronRight, FileText, MapPin, MessageCircle, Shield, Sparkles } from "lucide-vue-next"
import { computed, onMounted, onUnmounted, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import FeatureCard from "@/components/FeatureCard.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"
import { getCurrentUser, getMyAnalyses } from "@/lib/api"
import { openPdfPreview } from "@/lib/pdf"
import { formatApplicationDate, getHospitalApplications, getLastAnalysis, normalizeAnalysisResponse, type HospitalApplication } from "@/lib/skinai"

const applications = ref<HospitalApplication[]>([])
const analysis = ref(getLastAnalysis())
const latestApplication = computed(() => applications.value[0])

function viewApplicationPdf(application: HospitalApplication) {
  if (application.analysisSnapshot) {
    openPdfPreview({
      analysis: application.analysisSnapshot,
      user: getCurrentUser(),
      notes: application.submissionNote,
      capturedImageDataUrl: application.analysisSnapshot.imageDataUrl,
    })
  } else if (analysis.value) {
    openPdfPreview({
      analysis: analysis.value,
      user: getCurrentUser(),
      capturedImageDataUrl: analysis.value.imageDataUrl,
    })
  }
}

function statusLabel(status: HospitalApplication["status"]) {
  return { submitted: "제출 완료", reviewing: "검토 중", confirmed: "확인됨" }[status] || status
}

function statusColor(status: HospitalApplication["status"]) {
  return { submitted: "text-primary bg-primary/10", reviewing: "text-warning bg-warning/10", confirmed: "text-success bg-success/10" }[status] || "text-muted-foreground bg-muted"
}

function refresh() {
  applications.value = getHospitalApplications()
  analysis.value = getLastAnalysis()
}

onMounted(async () => {
  refresh()
  try {
    const latest = (await getMyAnalyses())[0]
    if (latest) analysis.value = normalizeAnalysisResponse(latest.analysis)
  } catch {
    analysis.value = getLastAnalysis()
  }
  window.addEventListener("skinai:hospital-application-updated", refresh)
  window.addEventListener("skinai:analysis-updated", refresh)
  window.addEventListener("storage", refresh)
})
onUnmounted(() => {
  window.removeEventListener("skinai:hospital-application-updated", refresh)
  window.removeEventListener("skinai:analysis-updated", refresh)
  window.removeEventListener("storage", refresh)
})
</script>

<template>
  <AppHeader show-logo show-notification />
  <PageContainer>
    <section class="py-8 md:py-12">
      <div class="mb-3 flex items-center gap-2">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          <Sparkles class="h-5 w-5 text-primary" />
        </div>
        <span class="text-sm font-semibold text-primary">AI 피부 시술 추천</span>
      </div>
      <h1 class="mb-3 text-2xl font-bold leading-tight text-foreground md:text-3xl">
        사진 한 장으로<br />
        <span class="text-primary">피부 상태</span>를 확인하세요
      </h1>
      <p class="mb-8 text-base leading-relaxed text-muted-foreground">
        피부 이미지를 AI로 분석하고 맞춤 시술과 병원 연결까지 한 번에 확인합니다.
      </p>
      <RouterLink to="/capture" class="block">
        <BaseButton size="lg" class="h-14 w-full rounded-2xl text-base font-semibold">
          <Camera class="h-5 w-5" />
          피부 분석 시작
          <ArrowRight class="h-4 w-4" />
        </BaseButton>
      </RouterLink>
    </section>

    <section v-if="analysis" class="py-6">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold">최근 분석 결과</h2>
        <RouterLink to="/history" class="flex items-center gap-1 text-sm font-medium text-primary">
          전체보기
          <ChevronRight class="h-4 w-4" />
        </RouterLink>
      </div>
      <RouterLink to="/result" class="block">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div class="flex items-center gap-5">
            <ScoreRing :score="analysis.overallScore || 0" :size="80" :stroke-width="6" />
            <div class="flex-1">
              <p class="mb-1 text-sm text-muted-foreground">{{ analysis.date || "오늘 분석" }}</p>
              <p class="mb-2 text-lg font-semibold">{{ analysis.skinType || "피부 타입 분석" }}</p>
              <div class="flex flex-wrap gap-2">
                <span v-for="concern in analysis.concerns || []" :key="concern" class="rounded-full bg-success/10 px-2 py-0.5 text-xs text-success">
                  {{ concern }}
                </span>
              </div>
            </div>
            <ChevronRight class="h-5 w-5 text-muted-foreground" />
          </div>
        </div>
      </RouterLink>
    </section>

    <section v-if="latestApplication" class="py-6">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold">병원 신청 현황</h2>
        <RouterLink to="/hospitals" class="flex items-center gap-1 text-sm font-medium text-primary">
          병원 찾기
          <ChevronRight class="h-4 w-4" />
        </RouterLink>
      </div>
      <div class="rounded-2xl border border-primary/20 bg-card p-5 shadow-sm">
        <div class="flex items-start justify-between gap-3">
          <div class="flex-1 min-w-0">
            <p class="font-semibold truncate">{{ latestApplication.hospitalName }}</p>
            <p class="mt-1 text-sm text-muted-foreground">{{ formatApplicationDate(latestApplication.submittedAt) }} 제출</p>
            <div class="mt-2 flex flex-wrap gap-1.5">
              <span v-for="item in latestApplication.includedItems" :key="item" class="rounded-full bg-accent px-2.5 py-1 text-xs">{{ item }}</span>
            </div>
          </div>
          <span :class="['shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold', statusColor(latestApplication.status)]">
            {{ statusLabel(latestApplication.status) }}
          </span>
        </div>
        <div class="mt-4 flex gap-2">
          <RouterLink
            :to="latestApplication.analysisId ? `/result/${latestApplication.analysisId}` : '/result'"
            class="flex-1"
          >
            <BaseButton variant="outline" size="sm" class="w-full">
              <MapPin class="h-3.5 w-3.5" />
              결과 보기
            </BaseButton>
          </RouterLink>
          <BaseButton
            v-if="latestApplication.analysisSnapshot || analysis"
            variant="outline"
            size="sm"
            class="flex-1"
            @click="viewApplicationPdf(latestApplication)"
          >
            <FileText class="h-3.5 w-3.5" />
            PDF 보기
          </BaseButton>
        </div>
      </div>
    </section>

    <section class="py-6">
      <h2 class="mb-4 text-lg font-semibold">주요 기능</h2>
      <div class="grid grid-cols-2 gap-3">
        <FeatureCard href="/capture" :icon="Camera" title="피부 촬영" description="카메라로 바로 분석" />
        <FeatureCard href="/chat" :icon="MessageCircle" icon-color="text-warning" icon-bg="bg-warning/10" title="AI 상담" description="시술 궁금증 상담" />
        <FeatureCard href="/hospitals" :icon="MapPin" icon-color="text-muted-foreground" icon-bg="bg-muted" title="병원 찾기" description="주변 피부과 검색" />
      </div>
    </section>

    <section class="pb-8 pt-6">
      <div class="rounded-2xl bg-secondary p-5">
        <div class="flex items-start gap-4">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-card">
            <Shield class="h-5 w-5 text-primary" />
          </div>
          <p class="text-sm leading-relaxed text-muted-foreground">
            AI 분석은 진료를 대체하지 않습니다. 실제 시술 여부와 강도는 전문가 상담을 통해 결정하는 것이 좋습니다.
          </p>
        </div>
      </div>
    </section>
  </PageContainer>
  <BottomNav />
</template>
