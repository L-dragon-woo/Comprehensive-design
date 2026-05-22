<script setup lang="ts">
import { Sparkles } from "lucide-vue-next"
import { onMounted } from "vue"
import { useRouter } from "vue-router"

const router = useRouter()
onMounted(() => window.setTimeout(() => router.push("/result"), 800))
</script>

<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-background px-5 py-12">
    <div class="flex h-28 w-28 items-center justify-center rounded-full bg-primary/10">
      <Sparkles class="h-10 w-10 animate-pulse text-primary" />
    </div>
    <h1 class="mt-8 text-center text-xl font-bold text-foreground">AI가 피부 분석 결과를 정리하고 있어요</h1>
  </div>
</template>
