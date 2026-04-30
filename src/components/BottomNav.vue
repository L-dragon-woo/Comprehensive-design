<script setup lang="ts">
import { Camera, Clock, Home, MessageCircle } from "lucide-vue-next"
import { useRoute } from "vue-router"

const route = useRoute()
const navItems = [
  { href: "/", label: "홈", icon: Home },
  { href: "/capture", label: "분석하기", icon: Camera },
  { href: "/chat", label: "AI 상담", icon: MessageCircle },
  { href: "/history", label: "기록", icon: Clock },
]
</script>

<template>
  <nav
    v-if="!['/capture', '/loading', '/result'].includes(route.path)"
    class="fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-card/95 backdrop-blur-lg"
  >
    <div class="mx-auto max-w-lg">
      <div class="flex items-center justify-around px-4 py-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]">
        <RouterLink
          v-for="item in navItems"
          :key="item.href"
          :to="item.href"
          :class="[
            'flex min-w-16 flex-col items-center justify-center gap-1 rounded-xl py-2 transition-all',
            route.path === item.href ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
          ]"
        >
          <span :class="['flex h-8 w-8 items-center justify-center rounded-full', route.path === item.href && 'bg-primary/10']">
            <component :is="item.icon" class="h-5 w-5" :stroke-width="route.path === item.href ? 2.5 : 2" />
          </span>
          <span :class="['text-[11px] font-medium', route.path === item.href && 'font-semibold text-primary']">
            {{ item.label }}
          </span>
        </RouterLink>
      </div>
    </div>
  </nav>
</template>
