<script setup lang="ts">
import { ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import BaseButton from "@/components/BaseButton.vue"
import { login } from "@/lib/api"

const router = useRouter()
const route = useRoute()
const username = ref("")
const password = ref("")
const error = ref("")
const loading = ref(false)

async function submit() {
  error.value = ""
  loading.value = true
  try {
    await login(username.value, password.value)
    router.push(String(route.query.redirect || "/"))
  } catch (e) {
    error.value = e instanceof Error ? e.message : "로그인에 실패했습니다."
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-background px-5">
    <form class="w-full max-w-sm rounded-3xl border border-border bg-card p-6 shadow-sm" @submit.prevent="submit">
      <h1 class="mb-2 text-2xl font-bold text-foreground">SkinAI 로그인</h1>
      <p class="mb-6 text-sm text-muted-foreground">관리자 계정으로 로그인하면 피부 분석과 병원 검색 기능을 사용할 수 있습니다.</p>
      <label class="mb-3 block">
        <span class="mb-1 block text-sm font-medium">아이디</span>
        <input v-model="username" class="h-12 w-full rounded-xl bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20" autocomplete="username" required />
      </label>
      <label class="mb-4 block">
        <span class="mb-1 block text-sm font-medium">비밀번호</span>
        <input v-model="password" type="password" class="h-12 w-full rounded-xl bg-input px-4 focus:outline-none focus:ring-2 focus:ring-primary/20" autocomplete="current-password" required />
      </label>
      <p v-if="error" class="mb-4 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>
      <BaseButton type="submit" size="lg" class="w-full" :disabled="loading">{{ loading ? "로그인 중..." : "로그인" }}</BaseButton>
    </form>
  </main>
</template>
