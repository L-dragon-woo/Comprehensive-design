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
  "나에게 추천된 시술을 쉽게 설명해줘",
  "가장 먼저 상담받을 시술은 뭐야?",
  "시술 전 확인해야 할 주의사항 알려줘",
  "리쥬란과 피코토닝 중 뭐가 더 우선이야?",
]

const messages = ref<ChatMessage[]>([
  {
    id: "1",
    role: "assistant",
    content: "안녕하세요! SkinAI 시술 상담사예요.\n\n분석 결과를 바탕으로 리쥬란 힐러, 피코토닝, 아쿠아필 상담을 우선 추천드릴 수 있어요. 수분 부족과 피부결, 볼 부근 색소 고민을 중심으로 정리했습니다.\n\n추천 시술의 목적, 우선순위, 시술 전 주의사항이 궁금하시면 편하게 물어봐 주세요.",
    timestamp: new Date(),
  },
])
const input = ref("")
const isLoading = ref(false)
const showSuggestions = ref(true)
const messagesEnd = ref<HTMLElement | null>(null)

function getAIResponse(question: string) {
  if (question.includes("쉽게 설명") || question.includes("추천된 시술")) {
    return "추천 시술을 쉽게 정리해드릴게요.\n\n리쥬란 힐러는 건조함과 피부결 개선을 상담해볼 만하고, 피코토닝은 볼 부근 색소침착 완화 목적에 맞아요. 아쿠아필은 T존 유분과 모공 관리에 보조적으로 고려할 수 있어요.\n\n실제 시술 여부와 강도는 피부 민감도, 병력, 최근 시술 이력을 확인한 뒤 결정하는 것이 좋아요."
  }
  if (question.includes("먼저") || question.includes("우선")) {
    return "현재 결과만 보면 리쥬란 힐러 상담을 먼저 받아보는 흐름이 자연스러워요.\n\n수분 부족과 피부결 고민이 함께 보이기 때문이에요. 다만 색소침착이 가장 큰 고민이라면 피코토닝을 우선 상담하고, 유분과 모공은 아쿠아필을 보조 옵션으로 검토하는 방식이 좋습니다."
  }
  if (question.includes("주의사항") || question.includes("확인")) {
    return "상담 전에는 아래 내용을 확인해 주세요.\n\n1. 최근 2주 내 레이저나 필링 시술 여부\n2. 피부염, 여드름 염증, 알레르기 반응 여부\n3. 임신 가능성이나 복용 중인 약\n4. 멍, 붓기, 홍조처럼 회복 기간에 영향을 줄 수 있는 일정\n\n이 정보가 있어야 시술 강도와 간격을 더 현실적으로 잡을 수 있어요."
  }
  if (question.includes("리쥬란") || question.includes("피코토닝")) {
    return "둘 중 우선순위는 가장 신경 쓰이는 고민에 따라 달라요.\n\n건조함, 잔결, 피부 컨디션 회복이 고민이면 리쥬란 상담이 먼저이고, 잡티나 색소침착이 가장 눈에 띈다면 피코토닝 상담이 먼저예요.\n\n현재 분석에서는 수분 부족과 색소침착이 함께 보여서, 상담 시 두 시술의 순서와 간격을 같이 물어보는 것을 추천드려요."
  }
  return "좋은 질문이에요!\n\n현재 추천은 수분 부족, T존 유분, 볼 부근 색소침착을 기준으로 정리된 참고 결과예요. 원하는 효과, 회복 가능 기간, 예산, 통증 민감도를 알려주시면 시술 상담에서 어떤 순서로 질문하면 좋을지 더 구체적으로 정리해드릴게요."
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
  <AppHeader title="AI 시술 상담" show-back />
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
            placeholder="시술 궁금증을 물어보세요"
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
