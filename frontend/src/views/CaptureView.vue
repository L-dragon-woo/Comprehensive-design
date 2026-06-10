<script setup lang="ts">
import { Camera, RotateCcw, SwitchCamera, UserCheck, UserX, X } from "lucide-vue-next"
import { computed, onBeforeUnmount, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { analyzePresignedImage, apiFetch, createImagePresignedUpload, readErrorReason, uploadToPresignedUrl } from "@/lib/api"
import { saveAnalysisImage, saveLastAnalysis, saveLastAnalysisId } from "@/lib/skinai"

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
const loadingProgress = ref(0)
const loadingMessage = ref("이미지를 준비하는 중입니다")
const loadingTipIndex = ref(0)
const facingMode = ref<"user" | "environment">("user")
const isMirroredCamera = computed(() => facingMode.value === "user")

const faceDetected = ref(false)
const faceDetectionSupported = ref(false)
const faceCheckMessage = ref("얼굴을 타원 안에 맞춰주세요")
const faceWarningShown = ref(false)
const cameraReady = ref(false)

let stream: MediaStream | null = null
let faceDetector: InstanceType<NonNullable<typeof window.FaceDetector>> | null = null
let faceCheckInterval: ReturnType<typeof setInterval> | null = null
let loadingProgressTimer: ReturnType<typeof setInterval> | null = null
let loadingTipTimer: ReturnType<typeof setInterval> | null = null
let loadingProgressCap = 88

const loadingTips = [
  "자외선 차단제는 흐린 날에도 바르는 것이 색소 관리에 도움이 됩니다.",
  "세안 후 3분 안에 보습제를 바르면 피부 수분 손실을 줄일 수 있습니다.",
  "레티놀이나 고기능 성분은 처음부터 매일 쓰기보다 주 2~3회로 시작하는 것이 좋습니다.",
  "피부 장벽이 예민한 날에는 각질 관리보다 보습과 진정에 집중해 주세요.",
  "눈가와 입가는 얇은 부위라 문지르기보다 가볍게 두드려 바르는 습관이 좋습니다.",
  "시술 계획은 AI 분석을 참고하되 실제 피부 상태는 대면 상담으로 확인하는 것이 안전합니다.",
]

function clearLoadingTimers() {
  if (loadingProgressTimer) clearInterval(loadingProgressTimer)
  if (loadingTipTimer) clearInterval(loadingTipTimer)
  loadingProgressTimer = null
  loadingTipTimer = null
}

function startLoading(message: string) {
  clearLoadingTimers()
  loading.value = true
  loadingProgress.value = 4
  loadingProgressCap = 88
  loadingMessage.value = message
  loadingTipIndex.value = 0

  loadingProgressTimer = setInterval(() => {
    if (loadingProgress.value >= loadingProgressCap) return
    const increment = loadingProgress.value < 40 ? 3 : loadingProgress.value < 75 ? 2 : 1
    loadingProgress.value = Math.min(loadingProgressCap, loadingProgress.value + increment)
  }, 420)

  loadingTipTimer = setInterval(() => {
    loadingTipIndex.value = (loadingTipIndex.value + 1) % loadingTips.length
  }, 3600)
}

function updateLoading(message: string, cap: number) {
  loadingMessage.value = message
  loadingProgressCap = cap
  loadingProgress.value = Math.max(loadingProgress.value, Math.min(cap - 6, loadingProgress.value + 8))
}

async function completeLoading() {
  loadingMessage.value = "분석 결과를 정리했습니다"
  loadingProgressCap = 100
  loadingProgress.value = 100
  clearLoadingTimers()
  await new Promise((resolve) => setTimeout(resolve, 350))
}

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
  cameraReady.value = false
  stopCamera()
  if (!navigator.mediaDevices?.getUserMedia) {
    error.value = "이 브라우저에서는 카메라를 사용할 수 없습니다."
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: facingMode.value, width: { ideal: 1280 }, height: { ideal: 960 } },
      audio: false,
    })
    if (video.value) {
      video.value.srcObject = stream
      await video.value.play().catch(() => undefined)
    }
    cameraReady.value = true
    faceCheckMessage.value = faceDetectionSupported.value ? "얼굴을 타원 안에 맞춰주세요" : "카메라가 준비되었습니다"
    if (faceDetectionSupported.value) {
      faceCheckInterval = setInterval(checkFace, 600)
    }
  } catch (e) {
    const name = e instanceof DOMException ? e.name : ""
    if (name === "NotAllowedError" || name === "SecurityError") {
      error.value = "카메라 권한이 차단되어 있습니다. 브라우저 주소창의 카메라 권한을 허용해 주세요."
    } else if (name === "NotFoundError" || name === "DevicesNotFoundError") {
      error.value = "사용 가능한 카메라를 찾지 못했습니다."
    } else if (name === "NotReadableError" || name === "TrackStartError") {
      error.value = "다른 앱이 카메라를 사용 중일 수 있습니다. 화상회의 앱이나 카메라 앱을 닫고 다시 시도해 주세요."
    } else if (name === "OverconstrainedError" || name === "ConstraintNotSatisfiedError") {
      error.value = "요청한 카메라 설정을 사용할 수 없습니다. 카메라 전환 버튼을 눌러 다시 시도해 주세요."
    } else {
      error.value = "카메라를 시작하지 못했습니다. 새로고침 후 다시 시도해 주세요."
    }
  }
}

function stopCamera() {
  if (faceCheckInterval !== null) {
    clearInterval(faceCheckInterval)
    faceCheckInterval = null
  }
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
  cameraReady.value = false
}

function capture() {
  const v = video.value
  const c = canvas.value
  if (!v || !c) return
  if (!cameraReady.value || v.readyState < 2 || !v.videoWidth || !v.videoHeight) {
    error.value = "카메라 화면을 준비하는 중입니다. 잠시 후 다시 촬영해 주세요."
    return
  }

  if (faceDetectionSupported.value && !faceDetected.value) {
    if (!faceWarningShown.value) {
      error.value = "얼굴이 인식되지 않았습니다. 정확한 분석을 위해 얼굴을 가이드에 맞춰주세요."
      faceWarningShown.value = true
      setTimeout(() => { error.value = ""; faceWarningShown.value = false }, 2500)
      return
    }
  }

  c.width = v.videoWidth
  c.height = v.videoHeight
  const ctx = c.getContext("2d")
  if (ctx) {
    ctx.save()
    if (isMirroredCamera.value) {
      ctx.translate(c.width, 0)
      ctx.scale(-1, 1)
    }
    ctx.drawImage(v, 0, 0, c.width, c.height)
    ctx.restore()
  }
  capturedImage.value = c.toDataURL("image/jpeg", 0.92)
  error.value = ""
  faceWarningShown.value = false
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

async function compressImage(dataUrl: string, maxSizeMb = 5): Promise<Blob> {
  const blob = await (await fetch(dataUrl)).blob()
  if (blob.size <= maxSizeMb * 1024 * 1024) return blob
  const img = new Image()
  img.src = dataUrl
  await new Promise((resolve) => { img.onload = resolve })
  const c = document.createElement("canvas")
  const scale = Math.sqrt((maxSizeMb * 1024 * 1024) / blob.size)
  c.width = Math.round(img.width * scale)
  c.height = Math.round(img.height * scale)
  c.getContext("2d")?.drawImage(img, 0, 0, c.width, c.height)
  return await new Promise<Blob>((resolve) => c.toBlob((b) => resolve(b!), "image/jpeg", 0.85))
}

async function createResultPreview(dataUrl: string): Promise<string> {
  const img = new Image()
  img.src = dataUrl
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve()
    img.onerror = () => reject(new Error("captured image preview failed"))
  })

  const maxDimension = 720
  const scale = Math.min(1, maxDimension / Math.max(img.width, img.height))
  const preview = document.createElement("canvas")
  preview.width = Math.max(1, Math.round(img.width * scale))
  preview.height = Math.max(1, Math.round(img.height * scale))
  preview.getContext("2d")?.drawImage(img, 0, 0, preview.width, preview.height)
  return preview.toDataURL("image/jpeg", 0.82)
}

type UploadMode = "multipart" | "presigned"

function preferredAnalysisUploadMode(): UploadMode {
  const queryMode = new URLSearchParams(window.location.search).get("upload")
  const storedMode = localStorage.getItem("skinai:analysis-upload-mode")
  const envMode = import.meta.env.VITE_ANALYSIS_UPLOAD_MODE
  return queryMode === "presigned" || storedMode === "presigned" || envMode === "presigned" ? "presigned" : "multipart"
}

function elapsedSince(start: number) {
  return Math.round(performance.now() - start)
}

async function analyzeMultipartImage(blob: Blob, timingsMs: Record<string, number>) {
  const requestStart = performance.now()
  const formData = new FormData()
  formData.append("image", blob, "capture.jpg")
  formData.append("gender", "female")

  const res = await apiFetch("/api/analyses", { method: "POST", body: formData })
  timingsMs.multipartAnalyze = elapsedSince(requestStart)
  if (!res.ok) throw new Error(await readErrorReason(res))
  return await res.json() as Record<string, unknown>
}

function hasPresignedUploadUrl(upload: unknown): upload is { key: string; url: string; method: "PUT"; contentType: string; expiresAt: string } {
  if (!upload || typeof upload !== "object") return false
  const candidate = upload as Record<string, unknown>
  return typeof candidate.key === "string" && candidate.key.length > 0
    && typeof candidate.url === "string" && candidate.url.length > 0
}

async function analyze() {
  if (!capturedImage.value) return
  const imageDataUrl = capturedImage.value
  startLoading("촬영 이미지를 분석용으로 준비하는 중입니다")
  try {
    const totalStart = performance.now()
    const blob = await compressImage(imageDataUrl)
    const resultImageDataUrl = await createResultPreview(imageDataUrl)
    const uploadMode = preferredAnalysisUploadMode()
    const timingsMs: Record<string, number> = { compress: elapsedSince(totalStart) }
    updateLoading("피부 분석 모델에 이미지를 전달하는 중입니다", 62)

    let data: Record<string, unknown>
    if (uploadMode === "presigned") {
      try {
        const presignStart = performance.now()
        const upload = await createImagePresignedUpload(blob, "capture.jpg")
        timingsMs.presign = elapsedSince(presignStart)

        if (!hasPresignedUploadUrl(upload)) {
          throw new Error("presigned upload URL is missing")
        }

        const s3UploadStart = performance.now()
        await uploadToPresignedUrl(upload, blob)
        timingsMs.s3Upload = elapsedSince(s3UploadStart)

        const analyzeStart = performance.now()
        data = await analyzePresignedImage(upload.key, "capture.jpg", "female")
        timingsMs.backendAnalyze = elapsedSince(analyzeStart)
      } catch {
        timingsMs.presignedFallback = elapsedSince(totalStart)
        data = await analyzeMultipartImage(blob, timingsMs)
      }
    } else {
      const requestStart = performance.now()
      const formData = new FormData()
      formData.append("image", blob, "capture.jpg")
      formData.append("gender", "female")

      const res = await apiFetch("/api/analyses", { method: "POST", body: formData })
      timingsMs.multipartAnalyze = elapsedSince(requestStart)
      if (!res.ok) throw new Error(`분석 요청에 실패했습니다. (${res.status})`)
      data = await res.json() as Record<string, unknown>
    }
    timingsMs.totalBeforeSummary = elapsedSince(totalStart)
    data.uploadBenchmark = {
      mode: uploadMode,
      fileBytes: blob.size,
      timingsMs,
    }
    const analysisId = (data as Record<string, unknown>).analysisId || (data as Record<string, unknown>).id
    if (typeof analysisId === "string" && analysisId) {
      saveLastAnalysisId(analysisId)
      saveAnalysisImage(analysisId, resultImageDataUrl)
    }

    updateLoading("AI 종합 분석과 PubMed 근거를 정리하는 중입니다", 96)
    try {
      const summaryRes = await apiFetch("/api/analyses/summary", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ analysis: data, gender: "female", analysisId: typeof analysisId === "string" ? analysisId : undefined }),
      })
      if (summaryRes.ok) {
        const summary = await summaryRes.json() as Record<string, unknown>
        if (typeof summary.content === "string" && summary.content.trim()) {
          data.aiSummary = summary.content.trim()
          data.aiSummaryMode = summary.mode
          const result = data.result
          if (result && typeof result === "object") {
            const nestedResult = result as Record<string, unknown>
            nestedResult.aiSummary = data.aiSummary
            nestedResult.aiSummaryMode = data.aiSummaryMode
          }
        }
      }
    } catch {
      // 요약 저장 실패는 분석 결과 진입을 막지 않습니다.
    }

    saveLastAnalysis(data, resultImageDataUrl)
    await completeLoading()
    router.push("/result")
  } catch (e) {
    error.value = e instanceof Error ? e.message : "분석을 완료하지 못했습니다."
    clearLoadingTimers()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await initFaceDetector()
  await startCamera()
})
onBeforeUnmount(() => {
  clearLoadingTimers()
  stopCamera()
})
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
        <video
          v-else
          ref="video"
          autoplay
          playsinline
          muted
          class="h-full w-full object-cover"
          :class="{ '-scale-x-100': isMirroredCamera }"
        />

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
        {{ cameraReady ? "얼굴을 정면으로 바라보고 타원 안에 맞춰주세요" : "카메라를 준비하는 중입니다" }}
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
            'flex h-20 w-20 items-center justify-center rounded-full bg-primary shadow-lg transition-all active:scale-95',
            !cameraReady && 'opacity-60',
          ]"
          :disabled="!cameraReady"
          title="촬영하기"
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

    <div v-if="loading" class="fixed inset-0 z-50 flex items-center justify-center bg-background/95 px-5 backdrop-blur-md">
      <div class="w-full max-w-sm rounded-3xl border border-border bg-card p-6 shadow-2xl">
        <div class="mb-5 flex items-center gap-4">
          <div class="relative flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
            <div class="absolute inset-2 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span class="text-sm font-bold text-primary">{{ loadingProgress }}%</span>
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-xs font-semibold text-primary">AI 피부 분석 중</p>
            <h2 class="mt-1 break-words text-base font-bold text-foreground">{{ loadingMessage }}</h2>
          </div>
        </div>

        <div class="mb-5 h-2.5 overflow-hidden rounded-full bg-secondary">
          <div
            class="h-full rounded-full bg-primary transition-all duration-500"
            :style="{ width: `${loadingProgress}%` }"
          />
        </div>

        <div class="rounded-2xl bg-secondary p-4">
          <p class="mb-1 text-xs font-semibold text-primary">피부관리 팁</p>
          <p class="break-words text-sm leading-relaxed text-foreground/80">{{ loadingTips[loadingTipIndex] }}</p>
        </div>

        <p class="mt-4 text-center text-xs leading-relaxed text-muted-foreground">
          분석 모델과 의학 근거를 함께 정리하고 있어 잠시 시간이 걸릴 수 있습니다.
        </p>
      </div>
    </div>
  </div>
</template>
