<script setup lang="ts">
import { CircleDot, Droplets, Sun } from "lucide-vue-next"
import { scoreBgColor, scoreColor } from "@/lib/skinai"

const props = defineProps<{
  overallScore: number
  skinType: string
  mainConcern: string
  hydration: number
  sebum: number
  pores: number
}>()

const details = [
  { label: "수분", score: props.hydration, icon: Droplets },
  { label: "유분", score: props.sebum, icon: Sun },
  { label: "모공", score: props.pores, icon: CircleDot },
]
</script>

<template>
  <div class="rounded-2xl border border-border bg-card p-4 shadow-sm">
    <div class="mb-4 flex items-center gap-3">
      <div :class="['flex h-14 w-14 items-center justify-center rounded-full', scoreBgColor(overallScore)]">
        <span :class="['text-xl font-bold', scoreColor(overallScore)]">{{ overallScore }}</span>
      </div>
      <div>
        <p class="text-sm font-medium text-foreground">{{ skinType }} 피부</p>
        <p class="text-xs text-muted-foreground">{{ mainConcern }}</p>
      </div>
    </div>
    <div class="grid grid-cols-3 gap-3">
      <div v-for="item in details" :key="item.label" class="flex flex-col items-center gap-1 rounded-xl bg-secondary p-2">
        <component :is="item.icon" class="h-4 w-4 text-muted-foreground" />
        <span class="text-xs text-muted-foreground">{{ item.label }}</span>
        <span :class="['text-sm font-semibold', scoreColor(item.score)]">{{ item.score }}</span>
      </div>
    </div>
  </div>
</template>
