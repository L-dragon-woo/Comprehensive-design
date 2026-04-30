<script setup lang="ts">
import { CheckCircle2, Sparkles } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"

const router = useRouter()
const currentStep = ref(0)
const completedSteps = ref<number[]>([])
const analysisSteps = [
  { id: 1, label: "피부 영역 감지 중...", duration: 1500 },
  { id: 2, label: "피부 톤 분석 중...", duration: 1200 },
  { id: 3, label: "모공 및 결 분석 중...", duration: 1300 },
  { id: 4, label: "피부 고민 파악 중...", duration: 1000 },
  { id: 5, label: "맞춤 솔루션 생성 중...", duration: 800 },
]
const progress = computed(() => (completedSteps.value.length / analysisSteps.length) * 100)

onMounted(() => {
  let index = 0
  const processStep = () => {
    if (index < analysisSteps.length) {
      currentStep.value = index
      window.setTimeout(() => {
        completedSteps.value.push(index)
        index += 1
        processStep()
      }, analysisSteps[index].duration)
    } else {
      window.setTimeout(() => router.push("/result"), 500)
    }
  }
  processStep()
})
</script>

<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-background px-5 py-12">
    <div class="w-full max-w-sm">
      <div class="relative mx-auto mb-10 w-fit">
        <div class="flex h-28 w-28 items-center justify-center rounded-full bg-primary/10">
          <div class="flex h-20 w-20 animate-pulse items-center justify-center rounded-full bg-primary/20">
            <Sparkles class="h-10 w-10 text-primary" />
          </div>
        </div>
        <svg class="absolute inset-0 h-28 w-28 animate-spin" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="46" fill="none" stroke="currentColor" stroke-width="2" stroke-dasharray="60 200" class="text-primary/30" />
        </svg>
      </div>

      <h1 class="mb-2 text-center text-xl font-bold text-foreground">AI가 피부를 분석하고 있어요</h1>
      <p class="mb-10 text-center text-sm text-muted-foreground">잠시만 기다려주세요</p>

      <div class="mb-8 w-full">
        <div class="h-2 overflow-hidden rounded-full bg-muted">
          <div class="h-full rounded-full bg-primary transition-all duration-500 ease-out" :style="{ width: `${progress}%` }" />
        </div>
        <p class="mt-3 text-center text-sm tabular-nums text-muted-foreground">{{ Math.round(progress) }}% 완료</p>
      </div>

      <div class="space-y-3">
        <div
          v-for="(step, index) in analysisSteps"
          :key="step.id"
          :class="[
            'flex items-center gap-3 rounded-xl px-4 py-3 transition-all duration-300',
            completedSteps.includes(index) && 'bg-success/10',
            currentStep === index && !completedSteps.includes(index) && 'bg-primary/10',
            !completedSteps.includes(index) && currentStep !== index && 'opacity-40',
          ]"
        >
          <div :class="['flex h-6 w-6 shrink-0 items-center justify-center rounded-full transition-all', completedSteps.includes(index) && 'bg-success', currentStep === index && !completedSteps.includes(index) && 'animate-pulse bg-primary']">
            <CheckCircle2 v-if="completedSteps.includes(index)" class="h-4 w-4 text-success-foreground" />
            <span v-else-if="currentStep === index" class="h-2 w-2 rounded-full bg-primary-foreground" />
            <span v-else class="h-2 w-2 rounded-full bg-muted-foreground/30" />
          </div>
          <span :class="['text-sm font-medium transition-colors', completedSteps.includes(index) && 'text-success', currentStep === index && !completedSteps.includes(index) && 'text-primary', !completedSteps.includes(index) && currentStep !== index && 'text-muted-foreground']">
            {{ step.label }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
