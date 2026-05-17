<script setup lang="ts">
import { Sparkles, User } from "lucide-vue-next"
import type { ChatMessage } from "@/lib/skinai"

defineProps<{ message: ChatMessage }>()
</script>

<template>
  <div :class="['flex items-end gap-2', message.role !== 'assistant' && 'flex-row-reverse']">
    <div class="mb-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full" :class="message.role === 'assistant' ? 'bg-primary/10' : 'bg-muted'">
      <Sparkles v-if="message.role === 'assistant'" class="h-4 w-4 text-primary" />
      <User v-else class="h-4 w-4 text-muted-foreground" />
    </div>
    <div :class="['max-w-[80%] rounded-2xl px-4 py-3', message.role === 'assistant' ? 'rounded-bl-sm bg-secondary text-foreground' : 'rounded-br-sm bg-primary text-primary-foreground']">
      <p class="whitespace-pre-wrap text-sm leading-relaxed">{{ message.content }}</p>
    </div>
  </div>
</template>
