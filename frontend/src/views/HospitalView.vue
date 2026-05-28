<script setup lang="ts">
import { CheckCircle2, MapPin, Navigation, Phone, Search, Send } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { saveHospitalApplication } from "@/lib/skinai"

type Hospital = { id: string; name: string; roadAddress: string; address: string; phone: string; distance: string; x: string; y: string; placeUrl: string }

const hospitals = ref<Hospital[]>([])
const query = ref("피부과")
const selectedHospitalId = ref("")
const loading = ref(false)
const error = ref("")
const submittedHospitalName = ref("")
const coords = ref<{ x: string; y: string } | null>(null)
const selectedHospital = computed(() => hospitals.value.find((hospital) => hospital.id === selectedHospitalId.value) || hospitals.value[0])
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
const hospitalMapBounds = computed(() => {
  if (!mappedHospitals.value.length) return null
  const lngs = mappedHospitals.value.map((item) => item.lng)
  const lats = mappedHospitals.value.map((item) => item.lat)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const lngPadding = Math.max((maxLng - minLng) * 0.18, 0.006)
  const latPadding = Math.max((maxLat - minLat) * 0.18, 0.006)
  return {
    minLng: minLng - lngPadding,
    maxLng: maxLng + lngPadding,
    minLat: minLat - latPadding,
    maxLat: maxLat + latPadding,
  }
})
const hospitalMapUrl = computed(() => {
  if (!hospitalMapBounds.value) return ""
  const { minLng, maxLng, minLat, maxLat } = hospitalMapBounds.value
  const bbox = [minLng, minLat, maxLng, maxLat].join(",")
  const params = new URLSearchParams({
    bbox,
    layer: "mapnik",
  })
  return `https://www.openstreetmap.org/export/embed.html?${params}`
})
const hospitalMapMarkers = computed(() => {
  if (!hospitalMapBounds.value) return []
  const { minLng, maxLng, minLat, maxLat } = hospitalMapBounds.value
  const lngRange = maxLng - minLng || 1
  const latRange = maxLat - minLat || 1
  return mappedHospitals.value.map(({ hospital, lat, lng }) => ({
    hospital,
    left: ((lng - minLng) / lngRange) * 100,
    top: ((maxLat - lat) / latRange) * 100,
  }))
})
const selectedHospitalDirectionsUrl = computed(() => {
  if (!selectedHospitalLocation.value || !selectedHospital.value) return selectedHospital.value?.placeUrl || "#"
  const { lat, lng } = selectedHospitalLocation.value
  return `https://www.openstreetmap.org/directions?to=${lat},${lng}#map=17/${lat}/${lng}`
})

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
    selectedHospitalId.value = hospitals.value[0]?.id || ""
  } catch (e) {
    error.value = e instanceof Error ? e.message : "병원 정보를 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
}

function submitReport() {
  if (!selectedHospital.value) return
  submittedHospitalName.value = selectedHospital.value.name
  saveHospitalApplication({
    id: Date.now().toString(),
    hospitalName: selectedHospital.value.name,
    submittedAt: new Date().toISOString(),
    status: "submitted",
    includedItems: ["AI 분석 결과", "추천 시술", "피부 지표"],
  })
}

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
        <div v-if="hospitalMapUrl" class="relative h-80 overflow-hidden border-t border-border bg-muted">
          <iframe
            :key="hospitalMapUrl"
            :src="hospitalMapUrl"
            title="검색된 병원 위치 지도"
            class="h-full w-full border-0"
            loading="lazy"
            referrerpolicy="no-referrer-when-downgrade"
          />
          <button
            v-for="marker in hospitalMapMarkers"
            :key="marker.hospital.id"
            type="button"
            :class="['absolute z-10 flex -translate-x-1/2 -translate-y-full flex-col items-center transition-transform hover:scale-105 focus:outline-none focus:ring-2 focus:ring-primary/30', selectedHospitalId === marker.hospital.id ? 'scale-110' : '']"
            :style="{ left: `${marker.left}%`, top: `${marker.top}%` }"
            :aria-label="`${marker.hospital.name} 상세정보 보기`"
            @click="selectedHospitalId = marker.hospital.id"
          >
            <span :class="['rounded-full p-2 text-primary-foreground shadow-lg ring-4 ring-background', selectedHospitalId === marker.hospital.id ? 'bg-primary' : 'bg-foreground']">
              <MapPin class="h-5 w-5" />
            </span>
            <span class="mt-1 max-w-32 truncate rounded-full bg-background/95 px-2 py-1 text-xs font-semibold text-foreground shadow-sm">{{ marker.hospital.name }}</span>
          </button>
          <div v-if="selectedHospital" class="absolute bottom-3 left-3 right-3 z-20 rounded-xl border border-border bg-background/95 p-4 shadow-lg backdrop-blur">
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

    <section class="pb-4">
      <div v-if="loading" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">검색 중...</div>
      <div v-else class="space-y-3">
        <button v-for="hospital in hospitals" :key="hospital.id" :class="['w-full rounded-2xl border bg-card p-5 text-left shadow-sm transition-all', selectedHospitalId === hospital.id ? 'border-primary ring-2 ring-primary/15' : 'border-border hover:border-primary/30']" @click="selectedHospitalId = hospital.id">
          <div class="mb-3 flex items-start justify-between gap-3">
            <div>
              <h4 class="text-base font-semibold text-foreground">{{ hospital.name }}</h4>
              <p class="mt-1 text-sm text-muted-foreground">{{ hospital.roadAddress || hospital.address }}</p>
            </div>
            <span v-if="hospital.distance" class="shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">{{ hospital.distance }}m</span>
          </div>
          <p class="text-sm text-muted-foreground">{{ hospital.phone || "전화번호 없음" }}</p>
        </button>
      </div>
      <div v-if="!loading && hospitals.length === 0" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">검색 결과가 없습니다.</div>
    </section>

    <section v-if="selectedHospital" class="space-y-3 py-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h3 class="mb-2 text-base font-semibold">결과지 제출</h3>
        <p class="mb-4 text-sm text-muted-foreground">{{ selectedHospital.name }}에 분석 결과를 제출할 수 있습니다.</p>
        <div class="flex gap-3">
          <a :href="selectedHospital.phone ? `tel:${selectedHospital.phone}` : selectedHospital.placeUrl" class="flex-1">
            <BaseButton variant="outline" size="lg" class="h-12 w-full rounded-xl">
              <Phone class="h-4 w-4" />
              전화
            </BaseButton>
          </a>
          <BaseButton size="lg" class="h-12 flex-1 rounded-xl" @click="submitReport">
            <Send class="h-4 w-4" />
            제출
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
</template>
