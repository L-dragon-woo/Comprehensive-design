<script setup lang="ts">
import { Calendar, ChevronRight, RefreshCw, Sparkles } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import ScoreRing from "@/components/ScoreRing.vue"
import { getMyAnalyses, type SavedAnalysis } from "@/lib/api"
import { normalizeAnalysisResponse, scoreColor } from "@/lib/skinai"

const analyses = ref<SavedAnalysis[]>([])
const loading = ref(true)
const error = ref("")

const records = computed(() =>
  analyses.value.map((item) => {
    const result = normalizeAnalysisResponse(item.analysis)
    const date = new Date(item.createdAt)
    return {
      id: item.analysisId,
      dateFormatted: Number.isNaN(date.getTime())
        ? result.date || "분석 결과"
        : new Intl.DateTimeFormat("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" }).format(date),
      timeFormatted: Number.isNaN(date.getTime())
        ? ""
        : new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit" }).format(date),
      score: result.overallScore || 0,
      skinType: result.skinType || "",
      concerns: result.concerns || [],
    }
  }),
)

async function reload() {
  loading.value = true
  error.value = ""
  try {
    analyses.value = await getMyAnalyses()
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석 기록을 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <AppHeader title="분석 기록" show-back show-notification />
  <PageContainer>
    <div v-if="loading" class="py-20 text-center text-sm text-muted-foreground">불러오는 중...</div>

    <div v-else-if="error" class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h3 class="mb-2 text-lg font-semibold">분석 기록을 불러오지 못했습니다</h3>
      <p class="mb-4 text-sm text-muted-foreground">{{ error }}</p>
      <div class="flex justify-center gap-3">
        <BaseButton variant="outline" @click="reload"><RefreshCw class="h-4 w-4" />다시 시도</BaseButton>
        <RouterLink to="/capture"><BaseButton>분석 시작</BaseButton></RouterLink>
      </div>
    </div>

    <section v-else-if="records.length" class="py-6">
      <p class="mb-4 text-sm text-muted-foreground">총 {{ records.length }}개의 분석 기록</p>
      <div class="space-y-3">
        <RouterLink v-for="record in records" :key="record.id" :to="`/result/${record.id}`" class="block">
          <div class="rounded-2xl border border-border bg-card p-4 shadow-sm transition-all active:scale-[0.99]">
            <div class="flex items-center gap-4">
              <ScoreRing :score="record.score" :size="64" :stroke-width="5" />
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <Calendar class="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                  <p class="text-sm text-muted-foreground">{{ record.dateFormatted }} {{ record.timeFormatted }}</p>
                </div>
                <p class="mt-1 font-semibold">{{ record.skinType || "피부 타입 분석" }}</p>
                <p :class="['text-2xl font-bold', scoreColor(record.score)]">{{ record.score }}점</p>
                <div class="mt-2 flex flex-wrap gap-1.5">
                  <span v-for="item in record.concerns.slice(0, 3)" :key="item" class="rounded-full bg-accent px-2.5 py-1 text-xs">{{ item }}</span>
                </div>
              </div>
              <ChevronRight class="h-5 w-5 shrink-0 text-muted-foreground" />
            </div>
          </div>
        </RouterLink>
      </div>
    </section>

    <div v-else class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h3 class="mb-2 text-lg font-semibold">아직 분석 기록이 없습니다</h3>
      <p class="mb-6 text-sm text-muted-foreground">첫 피부 분석을 시작해 보세요.</p>
      <RouterLink to="/capture"><BaseButton>분석 시작</BaseButton></RouterLink>
    </div>
  </PageContainer>
  <BottomNav />
</template>
