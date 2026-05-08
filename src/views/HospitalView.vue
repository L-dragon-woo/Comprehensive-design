<script setup lang="ts">
import { CheckCircle2, FileText, MapPin, Navigation, Phone, Search, Send, ShieldCheck, SlidersHorizontal, Star } from "lucide-vue-next"
import { computed, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"

type Hospital = {
  id: string
  name: string
  distance: string
  rating: number
  address: string
  specialties: string[]
  matchedTreatments: string[]
  waitTime: string
  phone: string
}

const hospitals: Hospital[] = [
  {
    id: "1",
    name: "서울스킨 피부과의원",
    distance: "0.8km",
    rating: 4.8,
    address: "서울 강남구 테헤란로 142",
    specialties: ["리쥬란", "피코토닝", "스킨부스터"],
    matchedTreatments: ["리쥬란 힐러", "피코토닝"],
    waitTime: "오늘 상담 가능",
    phone: "02-1234-5678",
  },
  {
    id: "2",
    name: "메디톤 클리닉",
    distance: "1.4km",
    rating: 4.6,
    address: "서울 서초구 강남대로 311",
    specialties: ["색소 레이저", "아쿠아필", "모공관리"],
    matchedTreatments: ["피코토닝", "아쿠아필"],
    waitTime: "내일 오전 가능",
    phone: "02-2345-6789",
  },
  {
    id: "3",
    name: "더맑은 피부의원",
    distance: "2.1km",
    rating: 4.7,
    address: "서울 강남구 도산대로 85",
    specialties: ["피부결", "홍조", "보습관리"],
    matchedTreatments: ["리쥬란 힐러"],
    waitTime: "이번 주 상담 가능",
    phone: "02-3456-7890",
  },
]

const query = ref("")
const selectedHospitalId = ref(hospitals[0].id)
const includePhoto = ref(true)
const includeScore = ref(true)
const includeTreatments = ref(true)
const submittedHospitalName = ref("")

const filteredHospitals = computed(() => {
  const keyword = query.value.trim()
  if (!keyword) return hospitals
  return hospitals.filter((hospital) => {
    return [hospital.name, hospital.address, ...hospital.specialties, ...hospital.matchedTreatments].some((item) => item.includes(keyword))
  })
})

const selectedHospital = computed(() => {
  return hospitals.find((hospital) => hospital.id === selectedHospitalId.value) ?? hospitals[0]
})

function submitReport() {
  submittedHospitalName.value = selectedHospital.value.name
}
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

    <section class="pb-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-lg font-semibold text-foreground">추천 병원</h3>
        <button class="flex items-center gap-1 text-sm font-medium text-primary">
          <Navigation class="h-4 w-4" />
          현재 위치 기준
        </button>
      </div>

      <div class="space-y-3">
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
            <span class="flex items-center gap-1">
              <Star class="h-4 w-4 fill-warning text-warning" />
              {{ hospital.rating }}
            </span>
            <span>{{ hospital.waitTime }}</span>
          </div>

          <div class="mb-3 flex flex-wrap gap-1.5">
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

      <div v-if="filteredHospitals.length === 0" class="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
        검색 결과가 없어요.
      </div>
    </section>

    <section class="space-y-3 py-6">
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
          <BaseButton variant="outline" size="lg" class="h-12 flex-1 rounded-xl">
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
