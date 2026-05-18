<script setup lang="ts">
import { CheckCircle2, FileText, LoaderCircle, LocateFixed, MapPin, Navigation, Phone, Search, Send, ShieldCheck, SlidersHorizontal, Star } from "lucide-vue-next"
import { computed, onMounted, ref, watch } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import HospitalMap from "@/components/HospitalMap.vue"
import PageContainer from "@/components/PageContainer.vue"
import { fetchHospitals, getCurrentLocation, type Hospital, type UserLocation } from "@/lib/hospitals"
import { saveHospitalApplication } from "@/lib/skinai"

const query = ref("")
const hospitals = ref<Hospital[]>([])
const selectedHospitalId = ref("")
const userLocation = ref<UserLocation | null>(null)
const isLoadingHospitals = ref(false)
const isLocating = ref(false)
const hospitalError = ref("")
const includePhoto = ref(true)
const includeScore = ref(true)
const includeTreatments = ref(true)
const submittedHospitalName = ref("")
let searchTimer: number | undefined

const filteredHospitals = computed(() => {
  return hospitals.value
})

const selectedHospital = computed(() => {
  return hospitals.value.find((hospital) => hospital.id === selectedHospitalId.value) ?? hospitals.value[0] ?? null
})

const locationLabel = computed(() => {
  return userLocation.value ? "현재 위치 기준" : "기본 위치 기준"
})

async function loadHospitals() {
  isLoadingHospitals.value = true
  hospitalError.value = ""

  try {
    hospitals.value = await fetchHospitals({
      query: query.value,
      location: userLocation.value ?? undefined,
    })

    if (!hospitals.value.some((hospital) => hospital.id === selectedHospitalId.value)) {
      selectedHospitalId.value = hospitals.value[0]?.id ?? ""
    }
  } catch (error) {
    hospitalError.value = error instanceof Error ? error.message : "병원 목록을 불러오지 못했어요."
    hospitals.value = []
    selectedHospitalId.value = ""
  } finally {
    isLoadingHospitals.value = false
  }
}

async function useCurrentLocation() {
  isLocating.value = true
  hospitalError.value = ""

  try {
    userLocation.value = await getCurrentLocation()
    await loadHospitals()
  } catch (error) {
    hospitalError.value = error instanceof Error ? error.message : "현재 위치를 확인하지 못했어요."
  } finally {
    isLocating.value = false
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
    includedItems: [
      ...(includeTreatments.value ? ["추천 시술 목록"] : []),
      ...(includeScore.value ? ["피부 점수와 지표"] : []),
      ...(includePhoto.value ? ["촬영 이미지"] : []),
    ],
  })
}

function callHospital() {
  if (!selectedHospital.value || selectedHospital.value.phone === "전화번호 미제공") return
  window.location.href = `tel:${selectedHospital.value.phone}`
}

watch(query, () => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    void loadHospitals()
  }, 300)
})

onMounted(() => {
  void useCurrentLocation()
})
</script>

<template>
  <AppHeader title="근처 병원 찾기" show-back />
  <PageContainer>
    <section class="py-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-start gap-3">
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <MapPin class="h-5 w-5 text-primary" />
          </div>
          <div>
            <h2 class="text-lg font-semibold text-foreground">분석 결과에 맞는 병원</h2>
            <p class="text-sm leading-relaxed text-muted-foreground">
              추천 시술을 상담할 수 있는 근처 병원을 찾고, 결과지를 미리 제출할 수 있어요.
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <div class="relative flex-1">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input
              v-model="query"
              type="text"
              placeholder="병원명, 시술명 검색"
              class="h-12 w-full rounded-xl bg-input pl-10 pr-4 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <BaseButton variant="outline" size="icon" class="h-12 w-12 shrink-0 rounded-xl">
            <SlidersHorizontal class="h-5 w-5" />
            <span class="sr-only">필터</span>
          </BaseButton>
        </div>
      </div>
    </section>

    <HospitalMap
      :hospitals="hospitals"
      :selected-hospital-id="selectedHospitalId"
      :user-location="userLocation"
      @select="selectedHospitalId = $event"
    />

    <section class="pb-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-lg font-semibold text-foreground">추천 병원</h3>
        <button class="flex items-center gap-1 text-sm font-medium text-primary disabled:opacity-60" :disabled="isLocating" @click="useCurrentLocation">
          <LoaderCircle v-if="isLocating" class="h-4 w-4 animate-spin" />
          <LocateFixed v-else class="h-4 w-4" />
          {{ locationLabel }}
        </button>
      </div>

      <div v-if="hospitalError" class="mb-3 rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
        {{ hospitalError }}
      </div>

      <div v-if="isLoadingHospitals" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
        <LoaderCircle class="mx-auto mb-2 h-5 w-5 animate-spin text-primary" />
        주변 병원을 불러오는 중이에요.
      </div>

      <div v-else class="space-y-3">
        <button
          v-for="hospital in filteredHospitals"
          :key="hospital.id"
          :class="[
            'w-full rounded-2xl border bg-card p-5 text-left shadow-sm transition-all',
            selectedHospitalId === hospital.id ? 'border-primary ring-2 ring-primary/15' : 'border-border hover:border-primary/30',
          ]"
          @click="selectedHospitalId = hospital.id"
        >
          <div class="mb-3 flex items-start justify-between gap-3">
            <div>
              <h4 class="text-base font-semibold text-foreground">{{ hospital.name }}</h4>
              <p class="mt-1 text-sm text-muted-foreground">{{ hospital.address }}</p>
            </div>
            <span class="shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">{{ hospital.distance }}</span>
          </div>

          <div class="mb-3 flex items-center gap-3 text-sm text-muted-foreground">
            <span v-if="hospital.rating > 0" class="flex items-center gap-1">
              <Star class="h-4 w-4 fill-warning text-warning" />
              {{ hospital.rating }}
            </span>
            <span v-else class="flex items-center gap-1">
              <Navigation class="h-4 w-4" />
              지도 검색
            </span>
            <span>{{ hospital.waitTime }}</span>
          </div>

          <div v-if="hospital.matchedTreatments.length > 0" class="mb-3 flex flex-wrap gap-1.5">
            <span v-for="treatment in hospital.matchedTreatments" :key="treatment" class="rounded-full bg-success/10 px-2.5 py-1 text-xs font-medium text-success">
              {{ treatment }} 매칭
            </span>
          </div>

          <div class="flex flex-wrap gap-1.5">
            <span v-for="specialty in hospital.specialties" :key="specialty" class="rounded-full bg-secondary px-2.5 py-1 text-xs font-medium text-secondary-foreground">
              {{ specialty }}
            </span>
          </div>
        </button>
      </div>

      <div v-if="!isLoadingHospitals && filteredHospitals.length === 0" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
        검색 결과가 없어요.
      </div>
    </section>

    <section v-if="selectedHospital" class="space-y-3 py-6">
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div class="mb-4 flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-success/10">
            <FileText class="h-5 w-5 text-success" />
          </div>
          <div>
            <h3 class="text-base font-semibold text-foreground">분석 결과지 제출</h3>
            <p class="text-sm text-muted-foreground">{{ selectedHospital.name }}에 상담 자료를 미리 전달해요.</p>
          </div>
        </div>

        <div class="mb-4 space-y-2">
          <label class="flex items-center justify-between rounded-xl bg-secondary px-4 py-3">
            <span class="text-sm font-medium text-secondary-foreground">추천 시술 목록</span>
            <input v-model="includeTreatments" type="checkbox" class="h-4 w-4 accent-primary" />
          </label>
          <label class="flex items-center justify-between rounded-xl bg-secondary px-4 py-3">
            <span class="text-sm font-medium text-secondary-foreground">피부 점수와 지표</span>
            <input v-model="includeScore" type="checkbox" class="h-4 w-4 accent-primary" />
          </label>
          <label class="flex items-center justify-between rounded-xl bg-secondary px-4 py-3">
            <span class="text-sm font-medium text-secondary-foreground">촬영 이미지</span>
            <input v-model="includePhoto" type="checkbox" class="h-4 w-4 accent-primary" />
          </label>
        </div>

        <div class="mb-4 rounded-xl bg-accent px-4 py-3">
          <div class="flex items-start gap-2">
            <ShieldCheck class="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <p class="text-sm leading-relaxed text-accent-foreground">
              제출 전 동의가 필요하며, 병원은 상담 목적 범위에서만 결과지를 확인할 수 있어요.
            </p>
          </div>
        </div>

        <div class="flex gap-3">
          <BaseButton variant="outline" size="lg" class="h-12 flex-1 rounded-xl" :disabled="selectedHospital.phone === '전화번호 미제공'" @click="callHospital">
            <Phone class="h-4 w-4" />
            전화
          </BaseButton>
          <BaseButton size="lg" class="h-12 flex-1 rounded-xl" @click="submitReport">
            <Send class="h-4 w-4" />
            제출
          </BaseButton>
        </div>
      </div>

      <div v-if="submittedHospitalName" class="rounded-2xl border border-success/20 bg-success/10 p-4">
        <div class="flex items-start gap-3">
          <CheckCircle2 class="mt-0.5 h-5 w-5 shrink-0 text-success" />
          <p class="text-sm font-medium leading-relaxed text-foreground">
            {{ submittedHospitalName }}에 분석 결과지를 제출했어요. 상담 예약 시 제출 자료 확인을 요청하세요.
          </p>
        </div>
      </div>
    </section>
  </PageContainer>
  <BottomNav />
</template>
