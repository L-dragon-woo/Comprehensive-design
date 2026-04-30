<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import { scoreColor } from "@/lib/skinai"

const props = withDefaults(defineProps<{
  score: number
  size?: number
  strokeWidth?: number
  animated?: boolean
  showLabel?: boolean
}>(), {
  size: 120,
  strokeWidth: 8,
  animated: true,
  showLabel: true,
})

const displayScore = ref(props.animated ? 0 : props.score)
const progress = ref(props.animated ? 0 : props.score)
const radius = computed(() => (props.size - props.strokeWidth) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const strokeDashoffset = computed(() => circumference.value - (progress.value / 100) * circumference.value)

function animate() {
  if (!props.animated) {
    displayScore.value = props.score
    progress.value = props.score
    return
  }
  const startTime = Date.now()
  const tick = () => {
    const elapsed = Date.now() - startTime
    const amount = Math.min(elapsed / 1000, 1)
    const eased = 1 - Math.pow(1 - amount, 3)
    displayScore.value = Math.round(props.score * eased)
    progress.value = props.score * eased
    if (amount < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

onMounted(animate)
watch(() => props.score, animate)
</script>

<template>
  <div class="relative inline-flex items-center justify-center">
    <svg :width="size" :height="size" class="-rotate-90">
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" stroke="currentColor" :stroke-width="strokeWidth" class="text-muted" />
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        stroke="currentColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="strokeDashoffset"
        :class="['transition-all duration-1000 ease-out', scoreColor(score)]"
      />
    </svg>
    <div v-if="showLabel" class="absolute inset-0 flex flex-col items-center justify-center">
      <span :class="['text-3xl font-bold', scoreColor(score)]">{{ displayScore }}</span>
      <span class="text-xs text-muted-foreground">점</span>
    </div>
  </div>
</template>
