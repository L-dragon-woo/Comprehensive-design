<script setup lang="ts">
import { CircleDot, Droplets, Home, MapPin, MessageCircle, NotebookPen, Save, Sparkles, Stethoscope, Sun } from "lucide-vue-next"
import { computed, onMounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import AnalysisCard from "@/components/AnalysisCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"
import { getMyAnalysis } from "@/lib/api"
import { getAnalysisNotes, getLastAnalysis, getLastAnalysisId, normalizeAnalysisResponse, saveAnalysisNotes, type AnalysisResult } from "@/lib/skinai"

const route = useRoute()
const result = ref<AnalysisResult | null>(null)
const loading = ref(false)
const error = ref("")
const icons = [Droplets, Sun, CircleDot]
const analysisId = computed(() => (typeof route.params.id === "string" ? route.params.id : ""))
const resolvedId = computed(() => analysisId.value || getLastAnalysisId() || "")

const notes = ref("")
const notesSaved = ref(false)
let notesSaveTimer: ReturnType<typeof setTimeout> | null = null

function loadNotes() {
  if (resolvedId.value) notes.value = getAnalysisNotes(resolvedId.value)
}

function saveNotes() {
  if (!resolvedId.value) return
  saveAnalysisNotes(resolvedId.value, notes.value)
  notesSaved.value = true
  if (notesSaveTimer) clearTimeout(notesSaveTimer)
  notesSaveTimer = setTimeout(() => { notesSaved.value = false }, 2000)
}

watch(resolvedId, loadNotes)

onMounted(async () => {
  if (!analysisId.value) {
    result.value = getLastAnalysis()
    loadNotes()
    return
  }

  loading.value = true
  try {
    const saved = await getMyAnalysis(analysisId.value)
    result.value = normalizeAnalysisResponse(saved.analysis)
    loadNotes()
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석 결과를 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppHeader title="분석 결과" show-back />
  <PageContainer :has-bottom-nav="false">
    <section v-if="loading" class="py-20 text-center text-sm text-muted-foreground">불러오는 중...</section>

    <section v-else-if="error" class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h2 class="mb-2 text-lg font-semibold">분석 결과를 불러오지 못했습니다</h2>
      <p class="mb-6 text-sm text-muted-foreground">{{ error }}</p>
      <RouterLink to="/history"><BaseButton>기록으로 돌아가기</BaseButton></RouterLink>
    </section>

    <section v-else-if="!result" class="py-20 text-center">
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

      <section class="py-4">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold flex items-center gap-2">
            <NotebookPen class="h-5 w-5 text-primary" />
            내 메모
          </h3>
          <button
            v-if="notes"
            :class="['flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-sm font-medium transition-colors', notesSaved ? 'bg-success/10 text-success' : 'bg-primary/10 text-primary']"
            @click="saveNotes"
          >
            <Save class="h-3.5 w-3.5" />
            {{ notesSaved ? "저장됨" : "저장" }}
          </button>
        </div>
        <div class="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
          <textarea
            v-model="notes"
            placeholder="분석 결과에 대한 메모를 입력하세요 (예: 시술 후기, 상담 내용, 개인 관리 기록...)"
            class="w-full resize-none bg-transparent px-5 py-4 text-sm focus:outline-none placeholder:text-muted-foreground"
            rows="4"
            @blur="saveNotes"
          />
        </div>
        <p v-if="!resolvedId" class="mt-2 text-xs text-muted-foreground">메모를 저장하려면 로그인 후 분석을 진행해 주세요.</p>
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
