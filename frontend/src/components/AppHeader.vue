<script setup lang="ts">
import { ArrowLeft, Bell, LogOut, Sparkles } from "lucide-vue-next"
import { computed, onMounted, onUnmounted, ref } from "vue"
import { useRouter } from "vue-router"
import { getCurrentUser, logout } from "@/lib/api"
import BaseButton from "./BaseButton.vue"
withDefaults(defineProps<{ title?: string; showBack?: boolean; showNotification?: boolean; showLogo?: boolean; transparent?: boolean }>(), { showBack: false, showNotification: false, showLogo: false, transparent: false })
const router = useRouter()
const tick = ref(0)
const user = computed(() => { tick.value; return getCurrentUser() })
function refresh(){ tick.value++ }
async function handleLogout(){ await logout(); router.push("/login") }
onMounted(() => window.addEventListener("skinai:auth-updated", refresh))
onUnmounted(() => window.removeEventListener("skinai:auth-updated", refresh))
</script>
<template>
  <header :class="['fixed left-0 right-0 top-0 z-50 pt-[max(0px,env(safe-area-inset-top))]', transparent ? 'bg-transparent' : 'border-b border-border bg-card/95 backdrop-blur-xl']">
    <div class="mx-auto max-w-lg lg:max-w-2xl"><div class="flex h-14 items-center justify-between px-4">
      <div class="flex items-center gap-2">
        <BaseButton v-if="showBack" variant="ghost" size="icon" class="rounded-full" @click="router.back()"><ArrowLeft class="h-5 w-5" /><span class="sr-only">뒤로 가기</span></BaseButton>
        <RouterLink v-else-if="showLogo" to="/" class="flex items-center gap-2"><span class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary"><Sparkles class="h-4 w-4 text-primary-foreground" /></span><span class="hidden text-lg font-bold text-foreground sm:inline">SkinAI</span></RouterLink>
      </div>
      <h1 v-if="title" class="absolute left-1/2 -translate-x-1/2 text-base font-semibold text-foreground">{{ title }}</h1>
      <div class="flex items-center justify-end gap-1">
        <span v-if="user" class="hidden max-w-24 truncate text-xs text-muted-foreground sm:inline">{{ user.displayName }}</span>
        <BaseButton v-if="showNotification" variant="ghost" size="icon" class="relative rounded-full"><Bell class="h-5 w-5" /><span class="sr-only">알림</span></BaseButton>
        <BaseButton v-if="user" variant="ghost" size="icon" class="rounded-full" @click="handleLogout"><LogOut class="h-5 w-5" /><span class="sr-only">로그아웃</span></BaseButton>
      </div>
    </div></div>
  </header>
</template>
