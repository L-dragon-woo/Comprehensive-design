<script setup lang="ts">
import { Camera, ImagePlus, RotateCcw, SwitchCamera, X } from "lucide-vue-next"
import { onBeforeUnmount, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { apiFetch } from "@/lib/api"
import { normalizeAnalysisResponse, saveLastAnalysis } from "@/lib/skinai"

const router = useRouter()
const video = ref<HTMLVideoElement | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const capturedImage = ref<string | null>(null)
const error = ref("")
const loading = ref(false)
const facingMode = ref<"user" | "environment">("user")
let stream: MediaStream | null = null

async function startCamera() {
  error.value = ""
  stopCamera()
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: facingMode.value }, audio: false })
    if (video.value) video.value.srcObject = stream
  } catch {
    error.value = "카메라를 사용할 수 없습니다. 갤러리에서 이미지를 선택해 주세요."
  }
}

function stopCamera() {
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
}

function capture() {
  const v = video.value
  const c = canvas.value
  if (!v || !c) return
  c.width = v.videoWidth
  c.height = v.videoHeight
  c.getContext("2d")?.drawImage(v, 0, 0)
  capturedImage.value = c.toDataURL("image/jpeg", 0.92)
}

function retake() {
  capturedImage.value = null
  startCamera()
}

function switchCamera() {
  facingMode.value = facingMode.value === "user" ? "environment" : "user"
  startCamera()
}

function handleFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    capturedImage.value = String(reader.result)
    stopCamera()
  }
  reader.readAsDataURL(file)
}

async function analyze() {
  if (!capturedImage.value) return
  loading.value = true
  const blob = await (await fetch(capturedImage.value)).blob()
  const formData = new FormData()
  formData.append("image", blob, "capture.jpg")
  formData.append("gender", "female")
  try {
    const res = await apiFetch("/api/analyses", { method: "POST", body: formData })
    if (!res.ok) throw new Error(`분석 요청에 실패했습니다. (${res.status})`)
    saveLastAnalysis(normalizeAnalysisResponse(await res.json()))
    router.push("/result")
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석을 완료하지 못했습니다."
  } finally {
    loading.value = false
  }
}

onMounted(startCamera)
onBeforeUnmount(stopCamera)
</script>

<template>
  <div class="fixed inset-0 flex flex-col bg-foreground/95">
    <div class="relative z-10 flex h-14 items-center justify-between px-4 pt-[max(0px,env(safe-area-inset-top))]">
      <BaseButton variant="ghost" size="icon" class="rounded-full text-primary-foreground/80 hover:bg-primary-foreground/10" @click="router.push('/')">
        <X class="h-6 w-6" />
        <span class="sr-only">닫기</span>
      </BaseButton>
      <h1 class="text-base font-semibold text-primary-foreground">피부 촬영</h1>
      <div class="w-10" />
    </div>

    <div class="flex flex-1 items-center justify-center px-5">
      <div class="relative aspect-[3/4] w-full max-w-sm overflow-hidden rounded-3xl bg-black">
        <img v-if="capturedImage" :src="capturedImage" alt="촬영 이미지" class="h-full w-full object-cover" />
        <video v-else ref="video" autoplay playsinline muted class="h-full w-full object-cover" />
        <div class="absolute inset-0 rounded-3xl border-4 border-primary/50" />
        <p v-if="error" class="absolute bottom-4 left-4 right-4 rounded-xl bg-destructive/90 p-3 text-center text-sm text-white">{{ error }}</p>
      </div>
      <canvas ref="canvas" class="hidden" />
    </div>

    <div class="px-5 py-6 pb-[max(24px,env(safe-area-inset-bottom))]">
      <div v-if="capturedImage" class="mx-auto flex max-w-sm items-center gap-3">
        <BaseButton variant="outline" size="lg" class="h-14 flex-1 rounded-2xl border-primary-foreground/30 text-primary-foreground hover:bg-primary-foreground/10" @click="retake">
          <RotateCcw class="h-5 w-5" />
          다시 촬영
        </BaseButton>
        <BaseButton size="lg" class="h-14 flex-1 rounded-2xl" :disabled="loading" @click="analyze">
          {{ loading ? "분석 중..." : "분석하기" }}
        </BaseButton>
      </div>
      <div v-else class="flex items-center justify-center gap-6">
        <BaseButton variant="ghost" size="icon" class="h-14 w-14 rounded-full bg-primary-foreground/10 text-primary-foreground" @click="fileInput?.click()">
          <ImagePlus class="h-6 w-6" />
          <span class="sr-only">갤러리에서 선택</span>
        </BaseButton>
        <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect" />
        <button class="flex h-20 w-20 items-center justify-center rounded-full bg-primary shadow-lg" @click="capture">
          <span class="flex h-16 w-16 items-center justify-center rounded-full border-4 border-primary-foreground/90">
            <Camera class="h-7 w-7 text-primary-foreground" />
          </span>
          <span class="sr-only">촬영하기</span>
        </button>
        <BaseButton variant="ghost" size="icon" class="h-14 w-14 rounded-full bg-primary-foreground/10 text-primary-foreground" @click="switchCamera">
          <SwitchCamera class="h-6 w-6" />
          <span class="sr-only">카메라 전환</span>
        </BaseButton>
      </div>
    </div>
  </div>
</template>
