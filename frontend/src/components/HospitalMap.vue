<script setup lang="ts">
import { MapPinned } from "lucide-vue-next"
import { nextTick, onBeforeUnmount, ref, watch } from "vue"
import type { Hospital, UserLocation } from "@/lib/hospitals"

declare global {
  interface Window {
    kakao?: {
      maps: {
        load: (callback: () => void) => void
        LatLng: new (latitude: number, longitude: number) => KakaoLatLng
        LatLngBounds: new () => KakaoLatLngBounds
        Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap
        Marker: new (options: { map?: KakaoMap; position: KakaoLatLng; title?: string }) => KakaoMarker
        event: {
          addListener: (target: KakaoMarker, type: "click", handler: () => void) => void
        }
      }
    }
  }
}

interface KakaoLatLng {
  getLat: () => number
  getLng: () => number
}

interface KakaoLatLngBounds {
  extend: (latLng: KakaoLatLng) => void
}

interface KakaoMap {
  setCenter: (latLng: KakaoLatLng) => void
  setBounds: (bounds: KakaoLatLngBounds) => void
}

interface KakaoMarker {
  setMap: (map: KakaoMap | null) => void
}

const props = defineProps<{
  hospitals: Hospital[]
  selectedHospitalId: string
  userLocation: UserLocation | null
}>()

const emit = defineEmits<{ select: [string] }>()

const mapContainer = ref<HTMLElement | null>(null)
const mapError = ref("")
const isMapLoading = ref(false)
let map: KakaoMap | null = null
let markers: KakaoMarker[] = []
let scriptPromise: Promise<void> | null = null

const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY as string | undefined

function loadKakaoMapScript() {
  if (window.kakao?.maps) {
    return Promise.resolve()
  }

  if (scriptPromise) {
    return scriptPromise
  }

  if (!appKey || appKey === "your-kakao-javascript-key") {
    return Promise.reject(new Error("Kakao JavaScript 키를 설정하면 지도를 표시할 수 있어요."))
  }

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script")
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false`
    script.async = true
    script.onload = () => window.kakao?.maps.load(resolve)
    script.onerror = () => reject(new Error("지도를 불러오지 못했어요."))
    document.head.appendChild(script)
  })

  return scriptPromise
}

async function renderMap() {
  if (!mapContainer.value) return

  isMapLoading.value = true
  mapError.value = ""

  try {
    await loadKakaoMapScript()
    await nextTick()

    const kakaoMaps = window.kakao?.maps
    if (!kakaoMaps) return

    const center = props.userLocation
      ? new kakaoMaps.LatLng(props.userLocation.latitude, props.userLocation.longitude)
      : new kakaoMaps.LatLng(props.hospitals[0]?.latitude ?? 37.4979, props.hospitals[0]?.longitude ?? 127.0276)

    if (!map) {
      map = new kakaoMaps.Map(mapContainer.value, { center, level: 4 })
    } else {
      map.setCenter(center)
    }

    markers.forEach((marker) => marker.setMap(null))
    markers = []

    const bounds = new kakaoMaps.LatLngBounds()
    bounds.extend(center)

    if (props.userLocation) {
      markers.push(new kakaoMaps.Marker({ map, position: center, title: "현재 위치" }))
    }

    props.hospitals.forEach((hospital) => {
      const position = new kakaoMaps.LatLng(hospital.latitude, hospital.longitude)
      bounds.extend(position)
      const marker = new kakaoMaps.Marker({ map: map ?? undefined, position, title: hospital.name })
      kakaoMaps.event.addListener(marker, "click", () => emit("select", hospital.id))
      markers.push(marker)
    })

    if (props.hospitals.length > 0 || props.userLocation) {
      map.setBounds(bounds)
    }
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : "지도를 표시하지 못했어요."
  } finally {
    isMapLoading.value = false
  }
}

watch(
  () => [props.hospitals, props.userLocation, props.selectedHospitalId],
  () => {
    void renderMap()
  },
  { deep: true, immediate: true },
)

onBeforeUnmount(() => {
  markers.forEach((marker) => marker.setMap(null))
})
</script>

<template>
  <section class="pb-4">
    <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
      <div ref="mapContainer" class="h-64 w-full bg-secondary"></div>
      <div v-if="mapError" class="flex items-start gap-3 border-t border-border px-4 py-3 text-sm text-muted-foreground">
        <MapPinned class="mt-0.5 h-4 w-4 shrink-0 text-primary" />
        <span>{{ mapError }}</span>
      </div>
      <div v-else-if="isMapLoading" class="border-t border-border px-4 py-3 text-sm text-muted-foreground">
        지도를 불러오는 중이에요.
      </div>
      <div v-else class="flex items-center justify-between border-t border-border px-4 py-3 text-sm text-muted-foreground">
        <span>지도에서 병원 위치를 확인하세요.</span>
        <button
          v-if="selectedHospitalId"
          class="font-medium text-primary"
          type="button"
          @click="emit('select', selectedHospitalId)"
        >
          선택 병원 보기
        </button>
      </div>
    </div>
  </section>
</template>
