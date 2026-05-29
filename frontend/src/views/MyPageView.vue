<script setup lang="ts">
import { onMounted, ref } from "vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { getCurrentUser, getMyProfile, updateMyProfile, type ProfilePayload } from "@/lib/api"

const displayName = ref("")
const gender = ref("")
const age = ref<number | null>(null)
const skinTreatmentHistory = ref("")
const hasAllergy = ref(false)
const allergyDetails = ref("")
const hasDisease = ref(false)
const diseaseDetails = ref("")
const loading = ref(false)
const saving = ref(false)
const error = ref("")
const saved = ref(false)

function fill(profile: ProfilePayload) {
  displayName.value = profile.displayName || ""
  gender.value = profile.gender || ""
  age.value = profile.age ?? null
  skinTreatmentHistory.value = profile.skinTreatmentHistory || ""
  hasAllergy.value = profile.hasAllergy
  allergyDetails.value = profile.allergyDetails || ""
  hasDisease.value = profile.hasDisease
  diseaseDetails.value = profile.diseaseDetails || ""
}

function payload(): ProfilePayload {
  return {
    displayName: displayName.value,
    gender: gender.value || null,
    age: age.value,
    skinTreatmentHistory: skinTreatmentHistory.value || null,
    hasAllergy: hasAllergy.value,
    allergyDetails: hasAllergy.value ? allergyDetails.value || null : null,
    hasDisease: hasDisease.value,
    diseaseDetails: hasDisease.value ? diseaseDetails.value || null : null,
  }
}

async function save() {
  error.value = ""
  saved.value = false
  if (age.value !== null && (age.value < 0 || age.value > 130)) {
    error.value = "나이는 0-130 사이로 입력해 주세요."
    return
  }
  saving.value = true
  try {
    fill(await updateMyProfile(payload()))
    saved.value = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : "프로필을 저장하지 못했습니다."
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const cached = getCurrentUser()
  if (cached) fill(cached)
  loading.value = true
  try {
    fill(await getMyProfile())
  } catch (e) {
    error.value = e instanceof Error ? e.message : "프로필을 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppHeader title="마이페이지" show-logo />
  <PageContainer>
    <form class="space-y-5 py-6" @submit.prevent="save">
      <section class="rounded-lg border border-border bg-card p-5 shadow-sm">
        <h2 class="mb-4 text-lg font-semibold">기본 정보</h2>
        <label class="mb-3 block">
          <span class="mb-1 block text-sm font-medium">이름</span>
          <input
            v-model="displayName"
            class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
            maxlength="40"
          />
        </label>

        <div class="grid grid-cols-2 gap-3">
          <label class="block">
            <span class="mb-1 block text-sm font-medium">성별</span>
            <select v-model="gender" class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20">
              <option value="">선택 안 함</option>
              <option value="female">여성</option>
              <option value="male">남성</option>
              <option value="other">기타</option>
            </select>
          </label>
          <label class="block">
            <span class="mb-1 block text-sm font-medium">나이</span>
            <input
              v-model.number="age"
              type="number"
              min="0"
              max="130"
              class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </label>
        </div>
      </section>

      <section class="rounded-lg border border-border bg-card p-5 shadow-sm">
        <h2 class="mb-4 text-lg font-semibold">피부/건강 이력</h2>
        <label class="mb-4 block">
          <span class="mb-1 block text-sm font-medium">받은 피부 시술/관리 이력</span>
          <textarea
            v-model="skinTreatmentHistory"
            class="min-h-28 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
            maxlength="5000"
            placeholder="레이저, 필링, 여드름 치료, 피부과 처방 등을 입력해 주세요."
          />
        </label>

        <div class="space-y-4">
          <div>
            <label class="flex items-center gap-2 text-sm font-medium">
              <input v-model="hasAllergy" type="checkbox" class="h-4 w-4" />
              알러지가 있어요
            </label>
            <textarea
              v-if="hasAllergy"
              v-model="allergyDetails"
              class="mt-2 min-h-20 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
              maxlength="500"
              placeholder="알러지 종류나 반응을 입력해 주세요."
            />
          </div>

          <div>
            <label class="flex items-center gap-2 text-sm font-medium">
              <input v-model="hasDisease" type="checkbox" class="h-4 w-4" />
              질병 또는 복용 중인 약이 있어요
            </label>
            <textarea
              v-if="hasDisease"
              v-model="diseaseDetails"
              class="mt-2 min-h-20 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
              maxlength="500"
              placeholder="질병명, 복용약, 주의사항을 입력해 주세요."
            />
          </div>
        </div>
      </section>

      <p v-if="error" class="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>
      <p v-else-if="saved" class="rounded-lg bg-success/10 p-3 text-sm text-success">저장되었습니다.</p>

      <BaseButton type="submit" size="lg" class="w-full" :disabled="loading || saving">
        {{ saving ? "저장 중..." : "저장" }}
      </BaseButton>
    </form>
  </PageContainer>
  <BottomNav />
</template>
