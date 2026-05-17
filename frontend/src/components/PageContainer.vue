<script setup lang="ts">
withDefaults(defineProps<{
  hasBottomNav?: boolean
  hasHeader?: boolean
  noPadding?: boolean
  maxWidth?: "sm" | "md" | "lg" | "xl"
}>(), {
  hasBottomNav: true,
  hasHeader: true,
  noPadding: false,
  maxWidth: "lg",
})

const maxWidthClasses = {
  sm: "max-w-sm",
  md: "max-w-md",
  lg: "max-w-lg",
  xl: "max-w-2xl",
}
</script>

<template>
  <main
    :class="[
      'min-h-screen bg-background',
      hasHeader && 'pt-[calc(56px+max(0px,env(safe-area-inset-top)))]',
      hasBottomNav && 'pb-[calc(80px+max(0px,env(safe-area-inset-bottom)))]',
      !noPadding && 'px-5 md:px-6',
    ]"
  >
    <div :class="['mx-auto', maxWidthClasses[maxWidth]]">
      <slot />
    </div>
  </main>
</template>
