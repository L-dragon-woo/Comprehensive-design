<script setup lang="ts">
const props = withDefaults(defineProps<{
  variant?: "default" | "outline" | "ghost"
  size?: "default" | "lg" | "icon" | "sm"
  disabled?: boolean
  type?: "button" | "submit"
}>(), {
  variant: "default",
  size: "default",
  type: "button",
})

const emit = defineEmits<{ click: [MouseEvent] }>()

const variantClass = {
  default: "bg-primary text-primary-foreground hover:bg-primary/90 shadow-sm",
  outline: "border border-border bg-transparent hover:bg-secondary text-foreground",
  ghost: "bg-transparent hover:bg-secondary text-foreground shadow-none",
}[props.variant]

const sizeClass = {
  sm: "h-9 px-3 text-sm",
  default: "h-10 px-4",
  lg: "h-14 px-5 text-base",
  icon: "h-10 w-10 p-0",
}[props.size]
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="[
      'inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-all focus:outline-none focus:ring-2 focus:ring-primary/25 disabled:pointer-events-none disabled:opacity-50',
      variantClass,
      sizeClass,
    ]"
    @click="emit('click', $event)"
  >
    <slot />
  </button>
</template>
