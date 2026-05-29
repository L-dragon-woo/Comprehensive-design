<script setup lang="ts">
import { Eye, EyeOff } from "lucide-vue-next"
import { computed, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { login, register, type ProfilePayload } from "@/lib/api"

const router = useRouter()
const route = useRoute()
const mode = ref<"login" | "register">("login")
const username = ref("")
const displayName = ref("")
const password = ref("")
const passwordConfirm = ref("")
const gender = ref("")
const age = ref<number | null>(null)
const skinTreatmentHistory = ref("")
const hasAllergy = ref(false)
const allergyDetails = ref("")
const hasDisease = ref(false)
const diseaseDetails = ref("")
const error = ref("")
const loading = ref(false)
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const isRegister = computed(() => mode.value === "register")
const usernameInputType = computed(() => (isRegister.value ? "email" : "text"))
const usernameAutocomplete = computed(() => (isRegister.value ? "email" : "username"))
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

const title = computed(() => (mode.value === "login" ? "SkinAI 로그인" : "SkinAI 회원가입"))
const submitLabel = computed(() => {
  if (loading.value) return mode.value === "login" ? "로그인 중..." : "가입 중..."
  return mode.value === "login" ? "로그인" : "회원가입"
})

function switchMode(nextMode: "login" | "register") {
  mode.value = nextMode
  error.value = ""
}

function profilePayload(): ProfilePayload {
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

function validateRegister() {
  if (!emailPattern.test(username.value.trim())) {
    error.value = "이메일 형식으로 입력해 주세요."
    return false
  }
  if (password.value.length < 4) {
    error.value = "비밀번호는 4자 이상 입력해 주세요."
    return false
  }
  if (password.value !== passwordConfirm.value) {
    error.value = "비밀번호 확인이 일치하지 않습니다."
    return false
  }
  if (age.value !== null && (age.value < 0 || age.value > 130)) {
    error.value = "나이는 0-130 사이로 입력해 주세요."
    return false
  }
  return true
}

async function submit() {
  error.value = ""
  if (isRegister.value && !validateRegister()) return
  loading.value = true
  try {
    if (!isRegister.value) {
      await login(username.value, password.value)
    } else {
      await register(username.value, password.value, profilePayload())
    }
    router.push(String(route.query.redirect || "/"))
  } catch (e) {
    error.value = e instanceof Error ? e.message : "요청을 처리하지 못했습니다."
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-background px-5 py-8">
    <form class="w-full max-w-sm rounded-lg border border-border bg-card p-6 shadow-sm" @submit.prevent="submit">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-foreground">{{ title }}</h1>
        <p class="mt-2 text-sm text-muted-foreground">
          {{ mode === "login" ? "계정으로 로그인하고 피부 분석과 AI 상담을 이어가세요." : "피부 상태와 건강 정보를 함께 저장해 더 맞춤형으로 관리합니다." }}
        </p>
      </div>

      <div class="mb-5 grid grid-cols-2 rounded-lg bg-secondary p-1">
        <button
          type="button"
          class="h-10 rounded-md text-sm font-medium transition-colors"
          :class="mode === 'login' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
          @click="switchMode('login')"
        >
          로그인
        </button>
        <button
          type="button"
          class="h-10 rounded-md text-sm font-medium transition-colors"
          :class="mode === 'register' ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
          @click="switchMode('register')"
        >
          회원가입
        </button>
      </div>

      <label class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">이메일</span>
        <input
          v-model="username"
          :type="usernameInputType"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          :autocomplete="usernameAutocomplete"
          required
        />
      </label>

      <label v-show="isRegister" class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">이름</span>
        <input
          v-model="displayName"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          autocomplete="name"
          maxlength="40"
          placeholder="화면에 표시할 이름"
        />
      </label>

      <div v-show="isRegister" class="mb-3 grid grid-cols-2 gap-3">
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
            placeholder="예: 24"
          />
        </label>
      </div>

      <label v-show="isRegister" class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">받은 피부 시술/관리 이력</span>
        <textarea
          v-model="skinTreatmentHistory"
          class="min-h-24 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
          maxlength="5000"
          placeholder="예: 레이저, 필링, 여드름 치료, 피부과 처방 등"
        />
      </label>

      <div v-show="isRegister" class="mb-3 space-y-3">
        <label class="flex items-center gap-2 text-sm font-medium">
          <input v-model="hasAllergy" type="checkbox" class="h-4 w-4" />
          알러지가 있어요
        </label>
        <textarea
          v-if="hasAllergy"
          v-model="allergyDetails"
          class="min-h-20 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
          maxlength="500"
          placeholder="알러지 종류나 반응을 입력해 주세요."
        />
        <label class="flex items-center gap-2 text-sm font-medium">
          <input v-model="hasDisease" type="checkbox" class="h-4 w-4" />
          질병 또는 복용 중인 약이 있어요
        </label>
        <textarea
          v-if="hasDisease"
          v-model="diseaseDetails"
          class="min-h-20 w-full rounded-lg bg-input px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/20"
          maxlength="500"
          placeholder="질병명, 복용약, 주의사항을 입력해 주세요."
        />
      </div>

      <label class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">비밀번호</span>
        <div class="relative">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="h-12 w-full rounded-lg bg-input px-4 pr-12 focus:outline-none focus:ring-2 focus:ring-primary/20"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            required
          />
          <button
            type="button"
            class="absolute right-3 top-1/2 grid h-9 w-9 -translate-y-1/2 place-items-center rounded-md text-muted-foreground hover:text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
            @click="showPassword = !showPassword"
          >
            <EyeOff v-if="showPassword" class="h-5 w-5" />
            <Eye v-else class="h-5 w-5" />
          </button>
        </div>
      </label>

      <label v-show="isRegister" class="mb-4 block">
        <span class="mb-1 block text-sm font-medium">비밀번호 확인</span>
        <div class="relative">
          <input
            v-model="passwordConfirm"
            :type="showPasswordConfirm ? 'text' : 'password'"
            class="h-12 w-full rounded-lg bg-input px-4 pr-12 focus:outline-none focus:ring-2 focus:ring-primary/20"
            autocomplete="new-password"
            :required="isRegister"
          />
          <button
            type="button"
            class="absolute right-3 top-1/2 grid h-9 w-9 -translate-y-1/2 place-items-center rounded-md text-muted-foreground hover:text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            :aria-label="showPasswordConfirm ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'"
            @click="showPasswordConfirm = !showPasswordConfirm"
          >
            <EyeOff v-if="showPasswordConfirm" class="h-5 w-5" />
            <Eye v-else class="h-5 w-5" />
          </button>
        </div>
      </label>

      <p v-if="error" class="mb-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>

      <BaseButton type="submit" size="lg" class="w-full" :disabled="loading">{{ submitLabel }}</BaseButton>
    </form>
  </main>
</template>

<style scoped>
input[type="password"]::-ms-reveal,
input[type="password"]::-ms-clear {
  display: none;
}
</style>
