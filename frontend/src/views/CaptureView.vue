<script setup lang="ts">
import { Camera, RotateCcw, SwitchCamera, UserCheck, UserX, X } from "lucide-vue-next"
import { onBeforeUnmount, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { apiFetch } from "@/lib/api"
import { saveLastAnalysis, saveLastAnalysisId } from "@/lib/skinai"

declare global {
  interface Window {
    FaceDetector?: new (options?: { maxDetectedFaces?: number; fastMode?: boolean }) => {
      detect(image: HTMLVideoElement | HTMLCanvasElement | ImageBitmap): Promise<{ boundingBox: DOMRectReadOnly }[]>
    }
  }
}

const router = useRouter()
const video = ref<HTMLVideoElement | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const capturedImage = ref<string | null>(null)
const error = ref("")
const loading = ref(false)
const facingMode = ref<"user" | "environment">("user")

const faceDetected = ref(false)
const faceDetectionSupported = ref(false)
const faceCheckMessage = ref("얼굴을 타원 안에 맞춰주세요")

let stream: MediaStream | null = null
let faceDetector: InstanceType<NonNullable<typeof window.FaceDetector>> | null = null
let faceCheckInterval: ReturnType<typeof setInterval> | null = null

async function initFaceDetector() {
  if (typeof window.FaceDetector !== "undefined") {
    try {
      faceDetector = new window.FaceDetector({ maxDetectedFaces: 1, fastMode: true })
      faceDetectionSupported.value = true
    } catch {
      faceDetectionSupported.value = false
    }
  }
}

async function checkFace() {
  if (!faceDetector || !video.value || video.value.readyState < 2) return
  try {
    const faces = await faceDetector.detect(video.value)
    faceDetected.value = faces.length > 0
    faceCheckMessage.value = faces.length > 0 ? "얼굴이 인식되었습니다" : "얼굴을 타원 안에 맞춰주세요"
  } catch {
    faceDetected.value = true
  }
}

async function startCamera() {
  error.value = ""
  stopCamera()
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: facingMode.value, width: { ideal: 1280 }, height: { ideal: 960 } },
      audio: false,
    })
    if (video.value) {
      video.value.srcObject = stream
    }
    if (faceDetectionSupported.value) {
      faceCheckInterval = setInterval(checkFace, 600)
    }
  } catch {
    error.value = "카메라를 사용할 수 없습니다. 권한을 허용해 주세요."
  }
}

function stopCamera() {
  if (faceCheckInterval !== null) {
    clearInterval(faceCheckInterval)
    faceCheckInterval = null
  }
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
}

function capture() {
  const v = video.value
  const c = canvas.value
  if (!v || !c) return

  if (faceDetectionSupported.value && !faceDetected.value) {
    error.value = "얼굴이 인식되지 않았습니다. 카메라 정면을 바라봐 주세요."
    return
  }

  c.width = v.videoWidth
  c.height = v.videoHeight
  c.getContext("2d")?.drawImage(v, 0, 0)
  capturedImage.value = c.toDataURL("image/jpeg", 0.92)
  error.value = ""
}

function retake() {
  capturedImage.value = null
  faceDetected.value = false
  startCamera()
}

function switchCamera() {
  facingMode.value = facingMode.value === "user" ? "environment" : "user"
  startCamera()
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
    const data = await res.json()
    const analysisId = (data as Record<string, unknown>).analysisId || (data as Record<string, unknown>).id
    if (typeof analysisId === "string" && analysisId) saveLastAnalysisId(analysisId)
    saveLastAnalysis(data)
    router.push("/result")
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석을 완료하지 못했습니다."
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await initFaceDetector()
  await startCamera()
})
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

        <!-- 얼굴 가이드 타원 오버레이 -->
        <div v-if="!capturedImage" class="pointer-events-none absolute inset-0 flex items-center justify-center">
          <div
            :class="[
              'h-72 w-52 rounded-[50%] border-4 transition-all duration-300',
              faceDetectionSupported
                ? faceDetected
                  ? 'border-green-400 shadow-[0_0_20px_rgba(74,222,128,0.4)]'
                  : 'border-white/60'
                : 'border-white/40 border-dashed',
            ]"
          />
        </div>

        <!-- 얼굴 인식 상태 배지 -->
        <div v-if="!capturedImage" class="absolute left-0 right-0 top-4 flex justify-center">
          <div
            :class="[
              'flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold backdrop-blur-sm transition-colors',
              faceDetectionSupported
                ? faceDetected
                  ? 'bg-green-500/80 text-white'
                  : 'bg-black/50 text-white/90'
                : 'bg-black/40 text-white/70',
            ]"
          >
            <component :is="faceDetected ? UserCheck : UserX" class="h-3.5 w-3.5" />
            {{ faceCheckMessage }}
          </div>
        </div>

        <!-- 카메라 테두리 -->
        <div
          :class="[
            'absolute inset-0 rounded-3xl border-4 transition-colors duration-300',
            faceDetectionSupported && faceDetected && !capturedImage ? 'border-green-400/60' : 'border-primary/50',
          ]"
        />

        <p v-if="error" class="absolute bottom-4 left-4 right-4 rounded-xl bg-destructive/90 p-3 text-center text-sm text-white">{{ error }}</p>
      </div>
      <canvas ref="canvas" class="hidden" />
    </div>

    <div class="px-5 py-6 pb-[max(24px,env(safe-area-inset-bottom))]">
      <p v-if="!capturedImage" class="mb-4 text-center text-xs text-primary-foreground/60">
        얼굴을 정면으로 바라보고 타원 안에 맞춰주세요
      </p>

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
        <!-- 갤러리 버튼 제거: 얼굴 인식 필수 정책 -->
        <div class="w-14" />

        <button
          :class="[
            'flex h-20 w-20 items-center justify-center rounded-full shadow-lg transition-all duration-300',
            faceDetectionSupported && !faceDetected
              ? 'bg-primary/40 cursor-not-allowed'
              : 'bg-primary active:scale-95',
          ]"
          :title="faceDetectionSupported && !faceDetected ? '얼굴 인식 후 촬영 가능합니다' : '촬영하기'"
          @click="capture"
        >
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
