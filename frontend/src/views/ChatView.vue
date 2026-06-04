<script setup lang="ts">
import { MapPin, Send } from "lucide-vue-next"
import { computed, nextTick, onMounted, ref } from "vue"
import AnalysisSummaryCard from "@/components/AnalysisSummaryCard.vue"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import ChatBubble from "@/components/ChatBubble.vue"
import ChatTypingIndicator from "@/components/ChatTypingIndicator.vue"
import PageContainer from "@/components/PageContainer.vue"
import { apiFetch, getMyAnalysis } from "@/lib/api"
import { deriveTreatmentsFromAnalysis, getAnalysisImage, getLastAnalysis, getLastAnalysisId, normalizeAnalysisResponse, type AnalysisResult, type ChatMessage } from "@/lib/skinai"

const analysisId = getLastAnalysisId()
const analysis = ref<AnalysisResult | null>(null)
const chatAnalysis = computed(() => analysis.value?.rawAnalysis || analysis.value)
const suggestedQuestions = [
  "추천 시술을 쉽게 설명해줘",
  "가장 먼저 상담받을 시술은 뭐야?",
  "시술 전 주의사항을 알려줘",
  "리쥬란과 피코토닝 중 무엇을 우선해야 해?",
]
const messages = ref<ChatMessage[]>([
  {
    id: "welcome",
    role: "assistant",
    content: "안녕하세요. 분석 결과를 바탕으로 시술 상담을 도와드릴게요.",
    timestamp: new Date(),
  },
])
const input = ref("")
const isLoading = ref(false)
const historyLoading = ref(false)
const showSuggestions = ref(true)
const messagesEnd = ref<HTMLElement | null>(null)
const sessionId = ref<string | null>(null)
const hasUserConsultation = computed(() => messages.value.some((message) => message.role === "user"))

function applyAiSummaryToAnalysis(content?: string) {
  if (!analysis.value || !content?.trim()) return
  analysis.value.aiSummary = content
  const treatments = deriveTreatmentsFromAnalysis(analysis.value, content)
  if (treatments.length) analysis.value.treatments = treatments
}

async function scrollToBottom() {
  await nextTick()
  messagesEnd.value?.scrollIntoView({ behavior: "smooth" })
}

async function requestAIResponse(question: string) {
  const history = messages.value
    .filter((message) => message.id !== "welcome")
    .filter((message, index, list) => !(index === list.length - 1 && message.role === "user" && message.content === question))
    .slice(-8)
    .map((message) => ({ role: message.role, content: message.content }))

  const res = await apiFetch("/api/consultations/messages", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message: question, sessionId: sessionId.value, analysis: chatAnalysis.value, analysisId: analysisId || undefined, history }),
  })
  if (!res.ok) throw new Error(`AI 상담 요청에 실패했습니다. (${res.status})`)
  const data = await res.json()
  if (typeof data.sessionId === "string") sessionId.value = data.sessionId
  return typeof data.content === "string" ? data.content : JSON.stringify(data)
}

async function handleSend(messageText?: string) {
  const text = messageText || input.value.trim()
  if (!text || isLoading.value) return
  messages.value.push({ id: Date.now().toString(), role: "user", content: text, timestamp: new Date() })
  input.value = ""
  isLoading.value = true
  showSuggestions.value = false
  await scrollToBottom()
  try {
    messages.value.push({ id: `${Date.now()}-ai`, role: "assistant", content: await requestAIResponse(text), timestamp: new Date() })
  } catch (e) {
    messages.value.push({
      id: `${Date.now()}-err`,
      role: "assistant",
      content: e instanceof Error ? e.message : "답변을 불러오지 못했습니다.",
      timestamp: new Date(),
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function parseHistoryMessage(value: unknown): ChatMessage | null {
  if (!value || typeof value !== "object") return null
  const item = value as Record<string, unknown>
  const role = item.role === "user" || item.role === "assistant" ? item.role : null
  const content = typeof item.content === "string" ? item.content.trim() : ""
  if (!role || !content) return null
  const createdAt = typeof item.createdAt === "string" ? item.createdAt : ""
  const timestamp = createdAt ? new Date(createdAt) : new Date()
  return {
    id: typeof item.id === "string" ? item.id : `${createdAt || Date.now()}-${role}`,
    role,
    content,
    timestamp: Number.isNaN(timestamp.getTime()) ? new Date() : timestamp,
  }
}

async function loadConsultationHistory() {
  historyLoading.value = true
  try {
    const query = analysisId ? `?analysisId=${encodeURIComponent(analysisId)}` : ""
    const res = await apiFetch(`/api/consultations/messages${query}`)
    if (!res.ok) return
    const data = await res.json() as Record<string, unknown>
    if (typeof data.sessionId === "string" && data.sessionId.trim()) sessionId.value = data.sessionId
    const restored = Array.isArray(data.messages) ? data.messages.map(parseHistoryMessage).filter((message): message is ChatMessage => Boolean(message)) : []
    if (restored.length) {
      messages.value = restored
      showSuggestions.value = !restored.some((message) => message.role === "user")
      await scrollToBottom()
    }
  } catch {
    // Keep the initial welcome message when stored chat history is unavailable.
  } finally {
    historyLoading.value = false
  }
}

async function loadAnalysisContext() {
  const cached = getLastAnalysis()
  if (cached?.rawAnalysis) {
    analysis.value = {
      ...normalizeAnalysisResponse(cached.rawAnalysis),
      imageDataUrl: cached.imageDataUrl,
      aiSummary: cached.aiSummary,
    }
    applyAiSummaryToAnalysis(cached.aiSummary)
  } else {
    analysis.value = cached
    applyAiSummaryToAnalysis(cached?.aiSummary)
  }

  if (!analysisId) return

  try {
    const saved = await getMyAnalysis(analysisId)
    const normalized = normalizeAnalysisResponse(saved.analysis)
    const summary = normalized.aiSummary || analysis.value?.aiSummary
    analysis.value = {
      ...normalized,
      aiSummary: summary,
      imageDataUrl: getAnalysisImage(analysisId) || analysis.value?.imageDataUrl,
    }
    applyAiSummaryToAnalysis(summary)
  } catch {
    // Keep the locally cached analysis if the saved analysis cannot be loaded.
  }
}

onMounted(async () => {
  await loadAnalysisContext()
  await loadConsultationHistory()
})
</script>

<template>
  <AppHeader title="AI 시술 상담" show-back />
  <PageContainer :has-bottom-nav="true" no-padding>
    <div class="flex h-[calc(100vh-56px-80px-max(0px,env(safe-area-inset-top))-max(0px,env(safe-area-inset-bottom)))] flex-col">
      <div class="flex-1 overflow-y-auto">
        <div class="space-y-4 px-5 py-4">
          <AnalysisSummaryCard
            v-if="analysis"
            :overall-score="analysis.overallScore || 0"
            :skin-type="analysis.skinType || '피부 타입 분석'"
            :main-concern="(analysis.concerns || []).join(', ')"
            :metrics="analysis.metrics"
            :concerns="analysis.concerns"
            :treatments="analysis.treatments"
            :image-data-url="analysis.imageDataUrl"
          />
          <ChatBubble v-for="message in messages" :key="message.id" :message="message" />
          <ChatTypingIndicator v-if="historyLoading" />
          <ChatTypingIndicator v-if="isLoading" />
          <div ref="messagesEnd" />
        </div>
      </div>

      <div v-if="showSuggestions" class="border-t border-border bg-card/80 px-5 py-3">
        <p class="mb-3 text-xs font-medium text-muted-foreground">추천 질문</p>
        <div class="flex flex-col gap-2">
          <button v-for="question in suggestedQuestions" :key="question" class="w-full rounded-xl bg-secondary px-4 py-3 text-left text-sm" @click="handleSend(question)">
            {{ question }}
          </button>
        </div>
      </div>

      <div class="border-t border-border bg-card px-5 py-4">
        <RouterLink v-if="hasUserConsultation" to="/hospitals" class="mb-4 block">
          <BaseButton size="lg" class="h-12 w-full rounded-xl">
            <MapPin class="h-4 w-4" />
            병원 찾기
          </BaseButton>
        </RouterLink>
        <div class="flex items-center gap-3">
          <input v-model="input" type="text" placeholder="시술 궁금증을 물어보세요" class="h-12 flex-1 rounded-xl bg-input px-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" @keydown="handleKeydown" />
          <BaseButton size="icon" class="h-12 w-12" :disabled="!input.trim() || isLoading" @click="handleSend()">
            <Send class="h-5 w-5" />
          </BaseButton>
        </div>
      </div>
    </div>
  </PageContainer>
  <BottomNav />
</template>
