<script setup lang="ts">
import { BookOpen, ExternalLink, Sparkles, User } from "lucide-vue-next"
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

      <div v-if="message.references?.length" class="mt-4 border-t border-border/70 pt-3">
        <div class="mb-2 flex items-center gap-1.5 text-xs font-semibold text-primary">
          <BookOpen class="h-3.5 w-3.5" />
          논문 기반 참고자료
        </div>
        <div class="space-y-2">
          <a
            v-for="reference in message.references"
            :key="reference.url"
            :href="reference.url"
            target="_blank"
            rel="noopener noreferrer"
            class="block rounded-lg border border-border bg-card px-3 py-2 transition-colors hover:border-primary/30 hover:bg-accent"
          >
            <div class="flex items-start justify-between gap-2">
              <p class="min-w-0 text-xs font-semibold leading-snug text-foreground">{{ reference.title }}</p>
              <ExternalLink class="mt-0.5 h-3.5 w-3.5 shrink-0 text-muted-foreground" />
            </div>
            <p class="mt-1 text-[11px] font-medium text-muted-foreground">{{ reference.source }}</p>
            <p class="mt-1 text-xs leading-relaxed text-muted-foreground">{{ reference.summary }}</p>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>
