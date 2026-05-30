<script setup lang="ts">
import { Calendar, Sparkles } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { getMyAnalyses, type SavedAnalysis } from "@/lib/api"
import { normalizeAnalysisResponse } from "@/lib/skinai"

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
        : new Intl.DateTimeFormat("ko-KR", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date),
      score: result.overallScore || 0,
      concerns: result.concerns || [],
    }
  }),
)

onMounted(async () => {
  try {
    analyses.value = await getMyAnalyses()
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석 기록을 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppHeader title="분석 기록" show-back show-notification />
  <PageContainer>
    <div v-if="loading" class="py-20 text-center text-sm text-muted-foreground">불러오는 중...</div>

    <div v-else-if="error" class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h3 class="mb-2 text-lg font-semibold">분석 기록을 불러오지 못했습니다</h3>
      <p class="mb-6 text-sm text-muted-foreground">{{ error }}</p>
      <RouterLink to="/capture"><BaseButton>분석 시작</BaseButton></RouterLink>
    </div>

    <section v-else-if="records.length" class="py-6">
      <div class="space-y-3">
        <div v-for="record in records" :key="record.id" class="rounded-2xl border border-border bg-card p-4 shadow-sm">
          <div class="flex items-start gap-4">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-secondary">
              <Calendar class="h-5 w-5 text-muted-foreground" />
            </div>
            <div class="flex-1">
              <p class="font-semibold">{{ record.dateFormatted }}</p>
              <p class="mt-1 text-2xl font-bold">{{ record.score }}점</p>
              <div class="mt-2 flex flex-wrap gap-1.5">
                <span v-for="item in record.concerns" :key="item" class="rounded-full bg-accent px-2.5 py-1 text-xs">{{ item }}</span>
              </div>
              <RouterLink :to="`/result/${record.id}`" class="mt-3 block">
                <BaseButton variant="outline" size="sm" class="w-full">결과 보기</BaseButton>
              </RouterLink>
            </div>
          </div>
        </div>
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
