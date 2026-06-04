<script setup lang="ts">
import { CheckCircle2, ChevronDown, ChevronUp, Eye, FileText, MapPin, Navigation, Phone, Search, Send, X } from "lucide-vue-next"
import { computed, nextTick, onMounted, ref, watch } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { getCurrentUser, uploadReportFile } from "@/lib/api"
import { getPdfHtml, openPdfPreview } from "@/lib/pdf"
import { getAnalysisNotes, getLastAnalysis, getLastAnalysisId, saveHospitalApplication, type AnalysisResult } from "@/lib/skinai"

type Hospital = { id: string; name: string; roadAddress: string; address: string; phone: string; distance: string; x: string; y: string; placeUrl: string }

declare global {
  interface Window {
    kakao?: any
  }
}

const hospitals = ref<Hospital[]>([])
const query = ref("피부과")
const selectedHospitalId = ref("")
const loading = ref(false)
const error = ref("")
const submittedHospitalName = ref("")
const coords = ref<{ x: string; y: string } | null>(null)

const showSubmitModal = ref(false)
const submitNote = ref("")
const includedReportItems = ["AI 분석 결과", "AI 종합 분석", "추천 시술", "피부 지표", "관리 추천"]
const currentAnalysis = ref<AnalysisResult | null>(null)
const showMetricsDetail = ref(false)
const submittingReport = ref(false)

function openSubmitModal() {
  currentAnalysis.value = getLastAnalysis()
  submitNote.value = getLastAnalysisId() ? getAnalysisNotes(getLastAnalysisId()!) : ""
  showSubmitModal.value = true
}

function closeSubmitModal() {
  showSubmitModal.value = false
}

function previewPdf() {
  if (!currentAnalysis.value) return
  openPdfPreview({
    analysis: currentAnalysis.value,
    user: getCurrentUser(),
    notes: submitNote.value,
    capturedImageDataUrl: currentAnalysis.value.imageDataUrl,
  })
}
const kakaoMapAppKey = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY || import.meta.env.VITE_KAKAO_MAP_APP_KEY || ""
const kakaoMapContainer = ref<HTMLElement | null>(null)
const kakaoMapError = ref("")
let kakaoMap: any = null
let kakaoMarkers: any[] = []
let kakaoMapSdkPromise: Promise<any> | null = null
const selectedHospital = computed(() => hospitals.value.find((hospital) => hospital.id === selectedHospitalId.value) || null)
const mappedHospitals = computed(() =>
  hospitals.value
    .map((hospital) => {
      const lng = Number(hospital.x)
      const lat = Number(hospital.y)
      return Number.isFinite(lng) && Number.isFinite(lat) ? { hospital, lat, lng } : null
    })
    .filter((item): item is { hospital: Hospital; lat: number; lng: number } => Boolean(item)),
)
const selectedHospitalLocation = computed(() => {
  if (!selectedHospital.value) return null
  const lng = Number(selectedHospital.value.x)
  const lat = Number(selectedHospital.value.y)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return { lat, lng }
})
const selectedHospitalDirectionsUrl = computed(() => {
  const placeUrl = selectedHospital.value?.placeUrl?.trim()
  if (!placeUrl || !/^https?:\/\//i.test(placeUrl)) return "#"

  try {
    const url = new URL(placeUrl)
    return url.protocol === "http:" || url.protocol === "https:" ? placeUrl : "#"
  } catch {
    return "#"
  }
})
const selectedHospitalKakaoSearchUrl = computed(() => {
  const keyword = selectedHospital.value?.name || query.value || "피부과"
  return `https://map.kakao.com/link/search/${encodeURIComponent(keyword)}`
})

function loadKakaoMapSdk() {
  if (!kakaoMapAppKey) return Promise.reject(new Error("missing-kakao-map-key"))
  if (window.kakao?.maps) {
    return new Promise<any>((resolve) => window.kakao?.maps.load(() => resolve(window.kakao?.maps)))
  }
  if (kakaoMapSdkPromise) return kakaoMapSdkPromise

  kakaoMapSdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script")
    const timeoutId = window.setTimeout(() => {
      kakaoMapSdkPromise = null
      script.remove()
      reject(new Error("kakao-map-load-failed"))
    }, 8000)
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(kakaoMapAppKey)}&autoload=false`
    script.async = true
    script.onload = () => {
      window.clearTimeout(timeoutId)
      if (!window.kakao?.maps) {
        kakaoMapSdkPromise = null
        reject(new Error("kakao-map-load-failed"))
        return
      }
      window.kakao.maps.load(() => {
        if (!window.kakao?.maps) {
          kakaoMapSdkPromise = null
          reject(new Error("kakao-map-load-failed"))
          return
        }
        resolve(window.kakao.maps)
      })
    }
    script.onerror = () => {
      window.clearTimeout(timeoutId)
      kakaoMapSdkPromise = null
      reject(new Error("kakao-map-load-failed"))
    }
    document.head.appendChild(script)
  })

  return kakaoMapSdkPromise
}

async function renderKakaoMap() {
  if (!kakaoMapContainer.value || !mappedHospitals.value.length) return
  kakaoMapError.value = ""

  try {
    const maps = await loadKakaoMapSdk()
    const first = selectedHospitalLocation.value || mappedHospitals.value[0]
    const center = new maps.LatLng(first.lat, first.lng)
    kakaoMap = new maps.Map(kakaoMapContainer.value, { center, level: 4 })
    const bounds = new maps.LatLngBounds()

    kakaoMarkers.forEach((marker) => marker.setMap(null))
    kakaoMarkers = mappedHospitals.value.map(({ hospital, lat, lng }) => {
      const position = new maps.LatLng(lat, lng)
      bounds.extend(position)
      const marker = new maps.Marker({ map: kakaoMap, position, title: hospital.name })
      maps.event.addListener(marker, "click", () => {
        selectedHospitalId.value = hospital.id
      })
      return marker
    })

    if (mappedHospitals.value.length > 1) kakaoMap.setBounds(bounds)
    else kakaoMap.setLevel(3)
  } catch (e) {
    kakaoMapError.value =
      e instanceof Error && e.message === "missing-kakao-map-key"
        ? "카카오맵 JavaScript 키가 설정되지 않았습니다. frontend/.env에 VITE_KAKAO_MAP_APP_KEY를 추가해 주세요."
        : "카카오맵을 불러오지 못했습니다. 카카오 개발자 콘솔 Web 플랫폼에 http://localhost:5173 도메인이 등록되어 있는지 확인해 주세요."
  }
}

function focusSelectedHospitalOnMap() {
  if (!kakaoMap || !window.kakao?.maps || !selectedHospitalLocation.value) return
  const { lat, lng } = selectedHospitalLocation.value
  kakaoMap.panTo(new window.kakao.maps.LatLng(lat, lng))
}

async function loadHospitals(useLocation = false) {
  loading.value = true
  error.value = ""
  try {
    if (useLocation && navigator.geolocation) {
      await new Promise<void>((resolve) =>
        navigator.geolocation.getCurrentPosition(
          (position) => {
            coords.value = { x: String(position.coords.longitude), y: String(position.coords.latitude) }
            resolve()
          },
          () => resolve(),
        ),
      )
    }
    const params = new URLSearchParams({ query: query.value || "피부과" })
    if (coords.value) {
      params.set("x", coords.value.x)
      params.set("y", coords.value.y)
      params.set("radius", "5000")
    }
    const res = await fetch(`/api/hospitals/search?${params}`)
    if (!res.ok) throw new Error(res.status === 503 ? "카카오 REST API 키가 설정되지 않았습니다." : `병원 검색에 실패했습니다. (${res.status})`)
    hospitals.value = await res.json()
    selectedHospitalId.value = ""
    submittedHospitalName.value = ""
  } catch (e) {
    error.value = e instanceof Error ? e.message : "병원 정보를 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
}

async function submitReport() {
  if (!selectedHospital.value || !currentAnalysis.value || submittingReport.value) return
  submittingReport.value = true
  const analysisId = getLastAnalysisId() || undefined
  let pdfUrl: string | undefined
  let pdfKey: string | undefined
  let reportStorageStatus: "stored" | "failed" = "failed"

  try {
    const html = getPdfHtml({
      analysis: currentAnalysis.value,
      user: getCurrentUser(),
      notes: submitNote.value,
      capturedImageDataUrl: currentAnalysis.value.imageDataUrl,
    })
    const reportBlob = new Blob([html], { type: "text/html;charset=UTF-8" })
    const storedReport = await uploadReportFile(reportBlob, `skinai-report-${analysisId || Date.now()}.html`, analysisId)
    pdfUrl = storedReport.url || undefined
    pdfKey = storedReport.key || undefined
    reportStorageStatus = storedReport.storageStatus === "stored" ? "stored" : "failed"
  } catch (e) {
    error.value = e instanceof Error ? e.message : "report upload failed"
    submittingReport.value = false
    return
  }

  error.value = ""
  submittedHospitalName.value = selectedHospital.value.name
  saveHospitalApplication({
    id: Date.now().toString(),
    hospitalName: selectedHospital.value.name,
    submittedAt: new Date().toISOString(),
    status: "submitted",
    includedItems: [...includedReportItems],
    analysisId,
    analysisSnapshot: currentAnalysis.value || undefined,
    submissionNote: submitNote.value || undefined,
    pdfUrl,
    pdfKey,
    reportStorageStatus,
  })
  submittingReport.value = false
  closeSubmitModal()
}

watch(
  mappedHospitals,
  async () => {
    await nextTick()
    renderKakaoMap()
  },
  { flush: "post" },
)

watch(selectedHospitalId, () => focusSelectedHospitalOnMap())

onMounted(() => loadHospitals(true))
</script>

<template>
  <AppHeader title="주변 병원 찾기" show-back />
  <PageContainer>
    <section class="py-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 class="mb-2 text-lg font-semibold">피부과 검색</h2>
        <p class="mb-4 text-sm text-muted-foreground">카카오 로컬 API로 주변 병원을 검색합니다.</p>
        <div class="flex gap-2">
          <div class="relative flex-1">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input v-model="query" type="text" placeholder="피부과 병원 이름" class="h-12 w-full rounded-xl bg-input pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" @keydown.enter="loadHospitals(false)" />
          </div>
          <BaseButton class="h-12" @click="loadHospitals(false)">검색</BaseButton>
        </div>
        <button class="mt-3 flex items-center gap-1 text-sm font-medium text-primary" @click="loadHospitals(true)">
          <Navigation class="h-4 w-4" />
          현재 위치 기준 검색
        </button>
      </div>
    </section>

    <p v-if="error" class="mb-4 rounded-xl bg-destructive/10 p-4 text-sm text-destructive">{{ error }}</p>

    <section v-if="hospitals.length" class="pb-4">
      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
        <div class="flex items-start justify-between gap-3 p-5">
          <div>
            <p class="text-xs font-semibold text-primary">검색된 병원 위치</p>
            <h3 class="mt-1 text-base font-semibold text-foreground">{{ selectedHospital?.name || "병원을 선택해 주세요" }}</h3>
            <p class="mt-1 text-sm text-muted-foreground">{{ selectedHospital ? selectedHospital.roadAddress || selectedHospital.address : "지도 마커를 누르면 상세정보가 표시됩니다." }}</p>
          </div>
          <a v-if="selectedHospital" :href="selectedHospitalDirectionsUrl" target="_blank" rel="noreferrer" class="shrink-0 rounded-full bg-primary/10 p-2 text-primary" aria-label="지도에서 열기">
            <MapPin class="h-5 w-5" />
          </a>
        </div>
        <div v-if="mappedHospitals.length" class="relative h-80 overflow-hidden border-t border-border bg-muted">
          <div ref="kakaoMapContainer" class="h-full w-full" />
          <div v-if="kakaoMapError" class="absolute inset-0 z-10 flex items-center justify-center bg-background/95 px-6 text-center text-sm text-muted-foreground">
            <div class="max-w-xs">
              <p>{{ kakaoMapError }}</p>
              <a :href="selectedHospitalDirectionsUrl !== '#' ? selectedHospitalDirectionsUrl : selectedHospitalKakaoSearchUrl" target="_blank" rel="noreferrer" class="mt-4 inline-flex h-10 items-center justify-center rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground">
                카카오맵에서 열기
              </a>
            </div>
          </div>
          <div v-if="selectedHospital && !kakaoMapError" class="absolute bottom-3 left-3 right-3 z-20 rounded-xl border border-border bg-background/95 p-4 shadow-lg backdrop-blur">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <h4 class="truncate text-sm font-semibold text-foreground">{{ selectedHospital.name }}</h4>
                <p class="mt-1 line-clamp-2 text-xs text-muted-foreground">{{ selectedHospital.roadAddress || selectedHospital.address }}</p>
                <p class="mt-2 text-xs text-muted-foreground">{{ selectedHospital.phone || "전화번호 없음" }}</p>
              </div>
              <span v-if="selectedHospital.distance" class="shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">{{ selectedHospital.distance }}m</span>
            </div>
            <a :href="selectedHospitalDirectionsUrl" target="_blank" rel="noreferrer" class="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-primary">
              <MapPin class="h-3.5 w-3.5" />
              지도에서 열기
            </a>
          </div>
        </div>
        <div v-else class="flex h-40 items-center justify-center border-t border-border px-5 text-center text-sm text-muted-foreground">이 병원들은 지도에 표시할 좌표가 없습니다.</div>
      </div>
    </section>

    <section v-if="loading || (!loading && hospitals.length === 0)" class="pb-4">
      <div v-if="loading" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">검색 중...</div>
      <div v-else class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">검색 결과가 없습니다.</div>
    </section>

    <section v-if="selectedHospital" class="space-y-3 py-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h3 class="mb-2 text-base font-semibold">결과지 제출</h3>
        <p class="mb-4 text-sm text-muted-foreground">{{ selectedHospital.name }}에 AI 분석 결과를 제출합니다.</p>
        <div class="flex gap-3">
          <a :href="selectedHospital.phone ? `tel:${selectedHospital.phone}` : selectedHospital.placeUrl" class="flex-1">
            <BaseButton variant="outline" size="lg" class="h-12 w-full rounded-xl">
              <Phone class="h-4 w-4" />
              전화
            </BaseButton>
          </a>
          <BaseButton size="lg" class="h-12 flex-1 rounded-xl" @click="openSubmitModal">
            <Eye class="h-4 w-4" />
            미리보기 및 제출
          </BaseButton>
        </div>
      </div>
      <div v-if="submittedHospitalName" class="rounded-2xl border border-success/20 bg-success/10 p-4">
        <div class="flex items-start gap-3">
          <CheckCircle2 class="mt-0.5 h-5 w-5 shrink-0 text-success" />
          <p class="text-sm font-medium">{{ submittedHospitalName }}에 결과지를 제출했습니다.</p>
        </div>
      </div>
    </section>
  </PageContainer>
  <BottomNav />

  <!-- 제출 미리보기 모달 -->
  <Teleport to="body">
    <div v-if="showSubmitModal" class="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="closeSubmitModal" />
      <div class="relative z-10 w-full max-w-lg overflow-hidden rounded-t-3xl bg-background sm:rounded-3xl">
        <!-- 모달 헤더 -->
        <div class="flex items-center justify-between border-b border-border px-5 py-4">
          <h2 class="text-lg font-bold">결과지 제출</h2>
          <button class="flex h-8 w-8 items-center justify-center rounded-full bg-muted" @click="closeSubmitModal">
            <X class="h-4 w-4" />
          </button>
        </div>

        <div class="max-h-[70vh] overflow-y-auto">
          <div class="space-y-5 p-5">
            <!-- 병원 정보 -->
            <div class="rounded-2xl bg-primary/5 p-4">
              <p class="text-xs font-semibold text-primary">제출 병원</p>
              <p class="mt-1 font-semibold">{{ selectedHospital?.name }}</p>
              <p class="text-sm text-muted-foreground">{{ selectedHospital?.roadAddress || selectedHospital?.address }}</p>
            </div>

            <!-- 분석 결과 미리보기 -->
            <div v-if="currentAnalysis">
              <div class="mb-3 flex items-center justify-between">
                <h3 class="font-semibold">분석 결과 미리보기</h3>
                <button class="flex items-center gap-1 text-sm text-primary" @click="showMetricsDetail = !showMetricsDetail">
                  <component :is="showMetricsDetail ? ChevronUp : ChevronDown" class="h-4 w-4" />
                  {{ showMetricsDetail ? "접기" : "상세보기" }}
                </button>
              </div>
              <div class="rounded-2xl border border-border bg-card p-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                    <span class="text-xl font-bold text-primary">{{ currentAnalysis.overallScore || 0 }}</span>
                  </div>
                  <div>
                    <p class="font-semibold">{{ currentAnalysis.skinType }}</p>
                    <div class="mt-1 flex flex-wrap gap-1">
                      <span v-for="c in currentAnalysis.concerns?.slice(0, 3)" :key="c" class="rounded-full bg-accent px-2 py-0.5 text-xs">{{ c }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="showMetricsDetail && currentAnalysis.metrics?.length" class="mt-4 space-y-2 border-t border-border pt-4">
                  <div v-for="m in currentAnalysis.metrics" :key="m.id" class="flex items-center justify-between text-sm">
                    <span class="text-muted-foreground">{{ m.title }}</span>
                    <span class="font-semibold">{{ m.score }}점</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              <FileText class="mx-auto mb-2 h-8 w-8 text-muted-foreground" />
              <p>분석 결과가 없습니다. 먼저 피부 분석을 진행해 주세요.</p>
            </div>

            <!-- 메모 -->
            <div>
              <h3 class="mb-3 font-semibold">병원에 전달할 메모 <span class="font-normal text-muted-foreground text-sm">(선택)</span></h3>
              <textarea
                v-model="submitNote"
                placeholder="증상, 고민, 요청 사항 등을 입력하세요"
                class="w-full resize-none rounded-xl border border-border bg-input px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                rows="3"
              />
            </div>
          </div>
        </div>

        <!-- 모달 하단 버튼 -->
        <div class="border-t border-border p-5">
          <div class="flex gap-3">
            <BaseButton v-if="currentAnalysis" variant="outline" class="h-12 flex-1 rounded-xl" @click="previewPdf">
              <FileText class="h-4 w-4" />
              PDF 미리보기
            </BaseButton>
            <BaseButton class="h-12 flex-1 rounded-xl" :disabled="!currentAnalysis || submittingReport" @click="submitReport">
              <Send class="h-4 w-4" />
              제출하기
            </BaseButton>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
