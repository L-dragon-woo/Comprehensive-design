<script setup lang="ts">
import type { Component } from "vue"

withDefaults(defineProps<{
  href: string
  icon: Component
  iconColor?: string
  iconBg?: string
  title: string
  description: string
}>(), {
  iconColor: "text-primary",
  iconBg: "bg-primary/10",
})
</script>

<template>
  <RouterLink :to="href" class="block rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:border-primary/20 hover:shadow-md active:scale-[0.98]">
    <div :class="['mb-4 flex h-12 w-12 items-center justify-center rounded-xl', iconBg]">
      <component :is="icon" :class="['h-6 w-6', iconColor]" />
    </div>
    <h3 class="mb-1 text-base font-semibold text-foreground">{{ title }}</h3>
    <p class="text-sm leading-relaxed text-muted-foreground">{{ description }}</p>
  </RouterLink>
</template>
