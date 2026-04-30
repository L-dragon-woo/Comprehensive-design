<script setup lang="ts">
import { Send } from "lucide-vue-next"
import { nextTick, ref } from "vue"
import AnalysisSummaryCard from "@/components/AnalysisSummaryCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import ChatBubble from "@/components/ChatBubble.vue"
import ChatTypingIndicator from "@/components/ChatTypingIndicator.vue"
import PageContainer from "@/components/PageContainer.vue"
import type { ChatMessage } from "@/lib/skinai"

const suggestedQuestions = [
  "지금 피부 상태를 쉽게 설명해줘",
  "가장 먼저 관리해야 할 부분은 뭐야?",
  "오늘부터 할 수 있는 관리법 알려줘",
  "내 피부 점수를 올리려면 어떻게 해야 해?",
]

const messages = ref<ChatMessage[]>([
  {
    id: "1",
    role: "assistant",
    content: "안녕하세요! SkinAI 피부 상담사예요.\n\n분석 결과를 확인했어요. 전체적으로 72점으로 양호한 편이지만, T존 유분 관리와 볼 수분 보충에 조금 더 신경 쓰시면 좋겠어요.\n\n궁금한 점이 있으시면 편하게 물어봐 주세요!",
    timestamp: new Date(),
  },
])
const input = ref("")
const isLoading = ref(false)
const showSuggestions = ref(true)
const messagesEnd = ref<HTMLElement | null>(null)

function getAIResponse(question: string) {
  if (question.includes("쉽게 설명") || question.includes("피부 상태")) {
    return "지금 피부 상태를 간단히 정리해드릴게요.\n\n현재 피부는 복합성으로, T존은 피지 분비가 활발하고 볼과 턱 부분은 수분이 부족한 상태예요.\n\n쉽게 말해서 '기름기 있는 곳은 번들거리고, 건조한 곳은 당기는' 전형적인 복합성 피부 특징을 보이고 있어요."
  }
  if (question.includes("먼저 관리") || question.includes("우선")) {
    return "가장 먼저 관리해야 할 부분은 수분 밸런스예요.\n\n수분이 부족하면 피부가 더 많은 유분을 분비할 수 있어서, 수분 공급을 먼저 집중적으로 해주시는 게 효과적이에요."
  }
  if (question.includes("오늘부터") || question.includes("관리법")) {
    return "오늘부터 바로 시작할 수 있는 관리법이에요.\n\n1. 세안 후 3분 이내 수분 토너 바르기\n2. T존은 가볍게, 볼은 촉촉하게 바르기\n3. 자기 전 수분 마스크팩을 주 2-3회 사용하기"
  }
  if (question.includes("점수") || question.includes("올리")) {
    return "현재 72점에서 80점 이상으로 올리려면 수분 점수 개선, 유분 밸런스 조절, 모공 관리 유지가 핵심이에요.\n\n히알루론산 세럼, 가벼운 수분크림, 주 1회 클레이 마스크부터 시작해보세요."
  }
  return "좋은 질문이에요!\n\n현재 복합성 피부로, 수분 관리와 유분 밸런스가 핵심이에요. 제품 추천, 루틴 구성, 특정 피부 고민 등 더 구체적으로 물어보셔도 좋아요."
}

async function scrollToBottom() {
  await nextTick()
  messagesEnd.value?.scrollIntoView({ behavior: "smooth" })
}

function handleSend(messageText?: string) {
  const text = messageText || input.value.trim()
  if (!text || isLoading.value) return
  messages.value.push({ id: Date.now().toString(), role: "user", content: text, timestamp: new Date() })
  input.value = ""
  isLoading.value = true
  showSuggestions.value = false
  scrollToBottom()

  window.setTimeout(() => {
    messages.value.push({ id: `${Date.now()}-ai`, role: "assistant", content: getAIResponse(text), timestamp: new Date() })
    isLoading.value = false
    scrollToBottom()
  }, 1500)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <AppHeader title="AI 피부 상담" show-back />
  <PageContainer :has-bottom-nav="true" no-padding>
    <div class="flex h-[calc(100vh-56px-80px-max(0px,env(safe-area-inset-top))-max(0px,env(safe-area-inset-bottom)))] flex-col">
      <div class="flex-1 overflow-y-auto">
        <div class="space-y-4 px-5 py-4">
          <AnalysisSummaryCard :overall-score="72" skin-type="복합성" main-concern="T존 유분, 볼 건조" :hydration="65" :sebum="78" :pores="70" />
          <ChatBubble v-for="message in messages" :key="message.id" :message="message" />
          <ChatTypingIndicator v-if="isLoading" />
          <div ref="messagesEnd" />
        </div>
      </div>

      <div v-if="showSuggestions" class="border-t border-border bg-card/80 px-5 py-3 backdrop-blur-sm">
        <p class="mb-3 text-xs font-medium text-muted-foreground">추천 질문</p>
        <div class="flex flex-col gap-2">
          <button
            v-for="question in suggestedQuestions"
            :key="question"
            class="w-full rounded-xl border border-transparent bg-secondary px-4 py-3 text-left text-sm text-secondary-foreground transition-colors hover:border-primary/20 hover:bg-accent"
            @click="handleSend(question)"
          >
            {{ question }}
          </button>
        </div>
      </div>

      <div class="border-t border-border bg-card px-5 py-4">
        <div class="flex items-center gap-3">
          <input
            v-model="input"
            type="text"
            placeholder="궁금한 점을 물어보세요"
            class="h-12 flex-1 rounded-xl bg-input px-4 text-sm transition-shadow placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            @keydown="handleKeydown"
          />
          <BaseButton size="icon" class="h-12 w-12 shrink-0 rounded-xl" :disabled="!input.trim() || isLoading" @click="handleSend()">
            <Send class="h-5 w-5" />
            <span class="sr-only">보내기</span>
          </BaseButton>
        </div>
      </div>
    </div>
  </PageContainer>
  <BottomNav />
</template>
