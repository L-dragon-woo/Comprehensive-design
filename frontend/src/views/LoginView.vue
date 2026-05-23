<script setup lang="ts">
import { computed, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { login, register } from "@/lib/api"

const router = useRouter()
const route = useRoute()
const mode = ref<"login" | "register">("login")
const username = ref("")
const displayName = ref("")
const password = ref("")
const passwordConfirm = ref("")
const error = ref("")
const loading = ref(false)

const title = computed(() => (mode.value === "login" ? "SkinAI 로그인" : "SkinAI 회원가입"))
const submitLabel = computed(() => {
  if (loading.value) return mode.value === "login" ? "로그인 중..." : "가입 중..."
  return mode.value === "login" ? "로그인" : "회원가입"
})

function switchMode(nextMode: "login" | "register") {
  mode.value = nextMode
  error.value = ""
}

function validateRegister() {
  if (!/^[a-zA-Z0-9._-]{3,30}$/.test(username.value.trim())) {
    error.value = "아이디는 영문, 숫자, 점, 밑줄, 하이픈으로 3-30자 입력해 주세요."
    return false
  }
  if (password.value.length < 8) {
    error.value = "비밀번호는 8자 이상 입력해 주세요."
    return false
  }
  if (password.value !== passwordConfirm.value) {
    error.value = "비밀번호 확인이 일치하지 않습니다."
    return false
  }
  return true
}

async function submit() {
  error.value = ""
  if (mode.value === "register" && !validateRegister()) return
  loading.value = true
  try {
    if (mode.value === "login") {
      await login(username.value, password.value)
    } else {
      await register(username.value, password.value, displayName.value)
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
          {{ mode === "login" ? "계정으로 로그인하고 피부 분석과 AI 상담을 이어가세요." : "새 계정을 만들고 상담 기록을 안전하게 저장하세요." }}
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
        <span class="mb-1 block text-sm font-medium">아이디</span>
        <input
          v-model="username"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          autocomplete="username"
          required
        />
      </label>

      <label v-if="mode === 'register'" class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">이름</span>
        <input
          v-model="displayName"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          autocomplete="name"
          maxlength="40"
          placeholder="화면에 표시될 이름"
        />
      </label>

      <label class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">비밀번호</span>
        <input
          v-model="password"
          type="password"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
          required
        />
      </label>

      <label v-if="mode === 'register'" class="mb-4 block">
        <span class="mb-1 block text-sm font-medium">비밀번호 확인</span>
        <input
          v-model="passwordConfirm"
          type="password"
          class="h-12 w-full rounded-lg bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20"
          autocomplete="new-password"
          required
        />
      </label>

      <p v-if="error" class="mb-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>

      <BaseButton type="submit" size="lg" class="w-full" :disabled="loading">{{ submitLabel }}</BaseButton>
    </form>
  </main>
</template>
