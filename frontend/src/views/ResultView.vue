<script setup lang="ts">
import { CircleDot, Droplets, FileDown, Home, MapPin, MessageCircle, NotebookPen, Save, Sparkles, Stethoscope, Sun } from "lucide-vue-next"
import { computed, onMounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import AiFormattedText from "@/components/AiFormattedText.vue"
import AnalysisCard from "@/components/AnalysisCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"
import { apiFetch, getCurrentUser, getMyAnalysis } from "@/lib/api"
import { openPdfPreview } from "@/lib/pdf"
import { deriveTreatmentsFromAnalysis, getAnalysisImage, getAnalysisNotes, getLastAnalysis, getLastAnalysisId, normalizeAnalysisResponse, saveAnalysisNotes, scoreBgColor, scoreColor, type AnalysisMetric, type AnalysisResult } from "@/lib/skinai"

const route = useRoute()
const result = ref<AnalysisResult | null>(null)
const loading = ref(false)
const error = ref("")
const analysisId = computed(() => (typeof route.params.id === "string" ? route.params.id : ""))
const resolvedId = computed(() => analysisId.value || getLastAnalysisId() || "")
const displayImageUrl = computed(() => {
  const storedImage = resolvedId.value ? getAnalysisImage(resolvedId.value) : null
  return storedImage || result.value?.imageDataUrl || result.value?.imageUrl || ""
})

const aiSummary = ref("")
const aiSummaryLoading = ref(false)
const selectedTreatmentName = ref("")
const selectedTreatment = computed(() => (result.value?.treatments || []).find((item) => item.name === selectedTreatmentName.value) || result.value?.treatments?.[0] || null)

const metricGroups = computed(() => {
  const metrics = result.value?.metrics ?? []
  if (!metrics.length) return []
  const order = ["종합", "색소", "주름", "균일도", "처짐"]
  const map = new Map<string, AnalysisMetric[]>()
  for (const m of metrics) {
    const cat = m.category ?? "기타"
    if (!map.has(cat)) map.set(cat, [])
    map.get(cat)!.push(m)
  }
  const groups: Array<{ category: string; metrics: AnalysisMetric[] }> = []
  for (const cat of order) {
    if (map.has(cat)) groups.push({ category: cat, metrics: map.get(cat)! })
  }
  for (const [cat, items] of map) {
    if (!order.includes(cat)) groups.push({ category: cat, metrics: items })
  }
  return groups
})

const icons = [Droplets, Sun, CircleDot]
const notes = ref("")
const notesSaved = ref(false)
let notesSaveTimer: ReturnType<typeof setTimeout> | null = null

function isGenericFallbackSummary(content?: string) {
  if (!content) return false
  return (
    (content.includes("예상 피부 나이") && content.includes("궁금한 항목을") && content.includes("구체적으로 물어보면")) ||
    content.includes("최종 레포트를 작성하려면") ||
    content.includes("지금까지 완료된 단계") ||
    content.includes("어떤 부분부터 진행하시겠어요")
  )
}

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

function downloadPdf() {
  if (!result.value) return
  openPdfPreview({
    analysis: result.value,
    user: getCurrentUser(),
    notes: notes.value,
    aiSummary: aiSummary.value || result.value.aiSummary,
    capturedImageDataUrl: displayImageUrl.value || undefined,
  })
}

function applyAiSummary(content: string) {
  aiSummary.value = content
  if (!result.value) return

  result.value.aiSummary = content
  const treatments = deriveTreatmentsFromAnalysis(result.value, content)
  if (treatments.length) result.value.treatments = treatments
  if (!selectedTreatmentName.value || !result.value.treatments?.some((item) => item.name === selectedTreatmentName.value)) {
    selectedTreatmentName.value = result.value.treatments?.[0]?.name || ""
  }
}

async function fetchAiSummary(analysis: unknown) {
  if (result.value?.aiSummary && !isGenericFallbackSummary(result.value.aiSummary)) {
    applyAiSummary(result.value.aiSummary)
    return
  }

  aiSummaryLoading.value = true
  try {
    // 1차: beauty-agent(PubMed) 엔드포인트 시도
    let content = ""
    const res = await apiFetch("/api/analyses/summary", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ analysis, gender: "female", analysisId: resolvedId.value || undefined }),
    })
    if (res.ok) {
      const data = await res.json() as Record<string, unknown>
      if (typeof data.content === "string") content = data.content.trim()
    }

    // 2차: 신규 엔드포인트 미지원 시 기존 상담 API로 fallback
    if (!content) {
      const res2 = await apiFetch("/api/consultations/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: "이 분석 결과를 바탕으로 주요 피부 문제와 추천 시술, 관리 방법을 한국어로 읽기 쉽게 요약해줘.",
          analysis,
        }),
      })
      if (res2.ok) {
        const data2 = await res2.json() as Record<string, unknown>
        if (typeof data2.content === "string") content = data2.content.trim()
      }
    }

    if (content) {
      applyAiSummary(content)
    }
  } catch {
    // 요약 실패 시 조용히 무시
  } finally {
    aiSummaryLoading.value = false
  }
}

onMounted(async () => {
  const cached = getLastAnalysis()
  if (!analysisId.value) {
    // Re-normalize so updated metric extraction logic applies to cached data
    result.value = cached?.rawAnalysis
      ? {
          ...normalizeAnalysisResponse(cached.rawAnalysis),
          imageDataUrl: cached.imageDataUrl,
          imageUrl: cached.imageUrl,
          aiSummary: cached.aiSummary,
        }
      : cached
    loadNotes()
    if (result.value?.aiSummary) applyAiSummary(result.value.aiSummary)
    if (result.value?.rawAnalysis) fetchAiSummary(result.value.rawAnalysis)
    return
  }

  loading.value = true
  try {
    const saved = await getMyAnalysis(analysisId.value)
    const normalized = normalizeAnalysisResponse(saved.analysis)
    result.value = {
      ...normalized,
      imageDataUrl: getAnalysisImage(analysisId.value) || cached?.imageDataUrl || undefined,
      imageUrl: normalized.imageUrl || cached?.imageUrl,
    }
    loadNotes()
    if (result.value.aiSummary) applyAiSummary(result.value.aiSummary)
    if (result.value?.rawAnalysis) fetchAiSummary(result.value.rawAnalysis)
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
          <div class="mb-5">
            <div class="min-w-0">
              <p class="mb-1 text-sm text-muted-foreground">{{ result.date || "오늘 분석" }}</p>
              <h2 class="mb-1 text-xl font-bold">{{ result.skinType || "피부 타입 분석" }}</h2>
              <p class="text-sm text-muted-foreground">AI 분석 결과를 기반으로 추천을 정리했습니다.</p>
            </div>
          </div>
          <div class="mb-5 flex items-center justify-center gap-6 rounded-2xl bg-secondary p-4">
            <div class="flex flex-col items-center gap-2">
              <span class="text-xs font-semibold text-muted-foreground">종합 점수</span>
              <ScoreRing :score="result.overallScore || 0" :size="96" :stroke-width="7" />
            </div>
            <div class="flex flex-col items-center gap-2">
              <span class="text-xs font-semibold text-muted-foreground">분석 얼굴 사진</span>
              <img v-if="displayImageUrl" :src="displayImageUrl" alt="분석에 사용한 얼굴 사진" class="h-28 w-24 rounded-2xl object-cover ring-1 ring-border" />
              <div v-else class="flex h-28 w-24 items-center justify-center rounded-2xl border border-dashed border-border bg-card px-3 text-center text-xs text-muted-foreground">
                얼굴 사진을 불러올 수 없습니다
              </div>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <span v-for="concern in result.concerns || []" :key="concern" class="rounded-full bg-accent px-3 py-1.5 text-xs font-medium">{{ concern }}</span>
          </div>
        </div>
      </section>

      <section v-if="result.treatments?.length" class="py-4">
        <div class="mb-3 flex items-center justify-between gap-3">
          <h3 class="text-lg font-semibold">추천 시술</h3>
          <span class="text-xs text-muted-foreground">점수가 낮은 지표 기준</span>
        </div>
        <div class="mb-4 flex flex-wrap gap-2">
          <button
            v-for="treatment in result.treatments || []"
            :key="treatment.name"
            type="button"
            :class="[
              'inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold transition-colors',
              selectedTreatment?.name === treatment.name ? 'border-primary bg-primary text-primary-foreground' : 'border-border bg-card text-foreground hover:border-primary/50',
            ]"
            @click="selectedTreatmentName = treatment.name"
          >
            <Stethoscope class="h-3.5 w-3.5" />
            {{ treatment.name }}
            <span v-if="treatment.score !== undefined" :class="selectedTreatment?.name === treatment.name ? 'text-primary-foreground/80' : 'text-muted-foreground'">{{ treatment.score }}점</span>
          </button>
        </div>
        <div v-if="selectedTreatment" class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div class="mb-3 flex items-start justify-between gap-3">
            <div>
              <h4 class="font-semibold">{{ selectedTreatment.name }}</h4>
              <p class="mt-1 text-sm text-muted-foreground">{{ selectedTreatment.reason }}</p>
            </div>
            <span class="shrink-0 rounded-full bg-success/10 px-3 py-1 text-xs font-semibold text-success">{{ selectedTreatment.match }}</span>
          </div>
          <p v-if="selectedTreatment.basis" class="mb-2 rounded-xl bg-primary/5 px-4 py-3 text-sm font-medium text-primary">{{ selectedTreatment.basis }} 근거</p>
          <p class="rounded-xl bg-secondary px-4 py-3 text-sm">{{ selectedTreatment.note }}</p>
        </div>
      </section>

      <!-- AI 종합 분석 요약 -->
      <section v-if="aiSummaryLoading || aiSummary" class="py-4">
        <h3 class="mb-4 flex items-center gap-2 text-lg font-semibold">
          <Sparkles class="h-5 w-5 text-primary" />
          AI 종합 분석
        </h3>
        <div class="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-sm">
          <div v-if="aiSummaryLoading" class="flex items-center gap-3 text-sm text-muted-foreground">
            <div class="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            AI가 분석 결과를 정리하는 중...
          </div>
          <AiFormattedText v-else :content="aiSummary" />
        </div>
      </section>

      <!-- 피부 지표 상세 -->
      <section class="py-4">
        <h3 class="mb-4 text-lg font-semibold">피부 지표 상세</h3>
        <div v-if="metricGroups.length" class="space-y-5">
          <div v-for="group in metricGroups" :key="group.category">
            <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{{ group.category }}</p>
            <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
              <div v-for="(metric, idx) in group.metrics" :key="metric.id" :class="['px-4 py-3', idx > 0 && 'border-t border-border']">
                <div class="mb-1.5 flex items-center justify-between gap-2">
                  <span class="truncate text-sm font-medium">{{ metric.title }}</span>
                  <span :class="['shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold', scoreBgColor(metric.score), scoreColor(metric.score)]">
                    {{ metric.id === 'age' ? `${metric.score}세` : `${metric.score}점 · ${metric.status}` }}
                  </span>
                </div>
                <div v-if="metric.id !== 'age'" class="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                  <div :class="['h-full rounded-full transition-all duration-700', metric.score >= 80 ? 'bg-success' : metric.score >= 60 ? 'bg-primary' : metric.score >= 40 ? 'bg-warning' : 'bg-destructive']" :style="{ width: `${metric.score}%` }" />
                </div>
                <p v-if="metric.description" class="mt-1 text-xs text-muted-foreground">{{ metric.description }}</p>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="space-y-3">
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
        <BaseButton variant="outline" size="lg" class="h-14 w-full rounded-2xl" @click="downloadPdf">
          <FileDown class="h-5 w-5" />
          결과지 PDF 저장
        </BaseButton>
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
