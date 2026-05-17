<script setup lang="ts">
import type { Component } from "vue"

withDefaults(defineProps<{
  icon: Component
  iconColor?: string
  iconBg?: string
  title: string
  subtitle?: string
  value?: string | number
  valueLabel?: string
}>(), {
  iconColor: "text-primary",
  iconBg: "bg-primary/10",
})
</script>

<template>
  <div class="w-full rounded-2xl border border-border bg-card p-4 text-left shadow-sm">
    <div class="flex items-center gap-4">
      <div :class="['flex h-12 w-12 items-center justify-center rounded-xl', iconBg]">
        <component :is="icon" :class="['h-6 w-6', iconColor]" />
      </div>
      <div class="min-w-0 flex-1">
        <p class="truncate text-base font-semibold text-foreground">{{ title }}</p>
        <p v-if="subtitle" class="truncate text-sm text-muted-foreground">{{ subtitle }}</p>
      </div>
      <div v-if="value !== undefined" class="text-right">
        <p class="text-lg font-bold text-foreground">{{ value }}</p>
        <p v-if="valueLabel" class="text-xs text-muted-foreground">{{ valueLabel }}</p>
      </div>
    </div>
  </div>
</template>
