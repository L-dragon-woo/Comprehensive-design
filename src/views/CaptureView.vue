<script setup lang="ts">
import { Camera, Check, ImagePlus, RotateCcw, SwitchCamera, X, Zap } from "lucide-vue-next"
import { ref } from "vue"
import { useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"

const router = useRouter()
const capturedImage = ref<string | null>(null)
const isCapturing = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const captureGuides = [
  { icon: Zap, text: "밝은 조명" },
  { icon: Check, text: "정면 응시" },
  { icon: Check, text: "민낯 권장" },
]

function handleCapture() {
  isCapturing.value = true
  window.setTimeout(() => {
    capturedImage.value = "/placeholder.jpg"
    isCapturing.value = false
  }, 500)
}

function handleFileSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    capturedImage.value = String(reader.result)
  }
  reader.readAsDataURL(file)
}
</script>

<template>
  <div class="fixed inset-0 flex flex-col bg-foreground/95">
    <div class="relative z-10 flex h-14 items-center justify-between px-4 pt-[max(0px,env(safe-area-inset-top))]">
      <BaseButton variant="ghost" size="icon" class="rounded-full text-primary-foreground/80 hover:bg-primary-foreground/10 hover:text-primary-foreground" @click="router.push('/')">
        <X class="h-6 w-6" />
        <span class="sr-only">닫기</span>
      </BaseButton>
      <h1 class="text-base font-semibold text-primary-foreground">피부 촬영</h1>
      <div class="w-10" />
    </div>

    <div class="flex flex-1 items-center justify-center px-5">
      <div v-if="capturedImage" class="relative aspect-[3/4] w-full max-w-sm overflow-hidden rounded-3xl">
        <img :src="capturedImage" alt="촬영된 이미지" class="h-full w-full object-cover" />
        <div class="absolute inset-0 rounded-3xl border-4 border-primary/50" />
      </div>
      <div v-else class="relative flex aspect-[3/4] w-full max-w-sm items-center justify-center rounded-3xl border-2 border-dashed border-primary-foreground/30 bg-primary-foreground/5">
        <svg viewBox="0 0 200 250" class="h-60 w-48 text-primary-foreground/30" fill="none" stroke="currentColor" stroke-width="2">
          <ellipse cx="100" cy="120" rx="70" ry="90" />
          <path d="M60 100 Q100 95 140 100" />
          <circle cx="70" cy="100" r="8" />
          <circle cx="130" cy="100" r="8" />
          <path d="M95 120 L95 140 L105 145" />
          <path d="M75 165 Q100 180 125 165" />
        </svg>
        <div v-if="isCapturing" class="absolute inset-0 flex items-center justify-center rounded-3xl bg-primary-foreground/20 backdrop-blur-sm">
          <div class="h-16 w-16 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
        <p class="absolute bottom-6 left-0 right-0 text-center text-sm text-primary-foreground/60">얼굴을 가이드 안에 맞춰주세요</p>
      </div>
    </div>

    <div class="px-5 py-6 pb-[max(24px,env(safe-area-inset-bottom))]">
      <div v-if="!capturedImage" class="mb-6 flex items-center justify-center gap-6">
        <div v-for="guide in captureGuides" :key="guide.text" class="flex items-center gap-1.5 text-xs text-primary-foreground/70">
          <component :is="guide.icon" class="h-3.5 w-3.5" />
          <span>{{ guide.text }}</span>
        </div>
      </div>

      <div v-if="capturedImage" class="mx-auto flex max-w-sm items-center gap-3">
        <BaseButton variant="outline" size="lg" class="h-14 flex-1 rounded-2xl border-primary-foreground/30 text-primary-foreground hover:bg-primary-foreground/10" @click="capturedImage = null">
          <RotateCcw class="h-5 w-5" />
          다시 촬영
        </BaseButton>
        <BaseButton size="lg" class="h-14 flex-1 rounded-2xl text-base font-semibold shadow-lg shadow-primary/30" @click="router.push('/loading')">
          분석하기
        </BaseButton>
      </div>

      <div v-else class="flex items-center justify-center gap-6">
        <BaseButton variant="ghost" size="icon" class="h-14 w-14 rounded-full bg-primary-foreground/10 text-primary-foreground hover:bg-primary-foreground/20" @click="fileInput?.click()">
          <ImagePlus class="h-6 w-6" />
          <span class="sr-only">갤러리에서 선택</span>
        </BaseButton>
        <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect" />
        <button
          :disabled="isCapturing"
          :class="['flex h-20 w-20 items-center justify-center rounded-full bg-primary shadow-lg shadow-primary/40 transition-all hover:scale-105 active:scale-95 focus:outline-none focus:ring-4 focus:ring-primary/30', isCapturing && 'scale-95 opacity-50']"
          @click="handleCapture"
        >
          <span class="flex h-16 w-16 items-center justify-center rounded-full border-4 border-primary-foreground/90">
            <Camera class="h-7 w-7 text-primary-foreground" />
          </span>
          <span class="sr-only">촬영하기</span>
        </button>
        <BaseButton variant="ghost" size="icon" class="h-14 w-14 rounded-full bg-primary-foreground/10 text-primary-foreground hover:bg-primary-foreground/20">
          <SwitchCamera class="h-6 w-6" />
          <span class="sr-only">카메라 전환</span>
        </BaseButton>
      </div>
    </div>
  </div>
</template>
