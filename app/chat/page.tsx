"use client"

import { useState, useRef, useEffect } from "react"
import { Send, Sparkles, User, Droplets, Sun, CircleDot } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Header } from "@/components/skinai/header"
import { PageContainer } from "@/components/skinai/page-container"
import { BottomNav } from "@/components/skinai/bottom-nav"
import { AnalysisSummaryCard } from "@/components/skinai/analysis-summary-card"
import { ChatBubble, ChatTypingIndicator } from "@/components/skinai/chat-bubble"
import { cn } from "@/lib/utils"
import type { ChatMessage } from "@/lib/skinai-types"

const suggestedQuestions = [
  "지금 피부 상태를 쉽게 설명해줘",
  "가장 먼저 관리해야 할 부분은 뭐야?",
  "오늘부터 할 수 있는 관리법 알려줘",
  "내 피부 점수를 올리려면 어떻게 해야 해?",
]

const initialMessages: ChatMessage[] = [
  {
    id: "1",
    role: "assistant",
    content:
      "안녕하세요! SkinAI 피부 상담사예요.\n\n분석 결과를 확인했어요. 전체적으로 72점으로 양호한 편이지만, T존 유분 관리와 볼 수분 보충에 조금 더 신경 쓰시면 좋겠어요.\n\n궁금한 점이 있으시면 편하게 물어봐 주세요!",
    timestamp: new Date(),
  },
]

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages)
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [showSuggestions, setShowSuggestions] = useState(true)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = async (messageText?: string) => {
    const text = messageText || input.trim()
    if (!text || isLoading) return

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: "user",
      content: text,
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput("")
    setIsLoading(true)
    setShowSuggestions(false)

    // Simulate AI response
    setTimeout(() => {
      const aiResponse: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: getAIResponse(text),
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, aiResponse])
      setIsLoading(false)
    }, 1500)
  }

  const getAIResponse = (question: string): string => {
    if (question.includes("쉽게 설명") || question.includes("피부 상태")) {
      return "지금 피부 상태를 간단히 정리해드릴게요.\n\n현재 피부는 복합성으로, T존(이마, 코)은 피지 분비가 활발하고 볼과 턱 부분은 수분이 부족한 상태예요.\n\n쉽게 말해서 '기름기 있는 곳은 번들거리고, 건조한 곳은 당기는' 전형적인 복합성 피부 특징을 보이고 있어요. 전체 점수 72점은 관리만 잘 하면 금방 좋아질 수 있는 상태랍니다!"
    }
    if (question.includes("먼저 관리") || question.includes("우선")) {
      return "분석 결과를 보면, 가장 먼저 관리해야 할 부분은 수분 밸런스예요.\n\n현재 수분 점수가 65점으로 가장 낮은데요, 이게 해결되면 유분 분비도 자연스럽게 줄어들 수 있어요.\n\n피부가 건조하면 오히려 더 많은 유분을 분비하거든요. 그래서 수분 공급을 먼저 집중적으로 해주시는 게 효과적이에요!"
    }
    if (question.includes("오늘부터") || question.includes("관리법")) {
      return "오늘부터 바로 시작할 수 있는 관리법 알려드릴게요!\n\n1. 세안 후 3분 이내 수분 토너 바르기\n   - 건조한 볼 부분은 토너를 2-3번 덧발라주세요\n\n2. T존은 가볍게, 볼은 촉촉하게\n   - 같은 제품도 부위별로 양 조절하기\n\n3. 자기 전 수분 마스크팩 (주 2-3회)\n   - 10분만 투자해도 다음날 피부결이 달라요\n\n작은 습관부터 시작해보세요!"
    }
    if (question.includes("점수") || question.includes("올리")) {
      return "피부 점수를 올리는 방법 알려드릴게요!\n\n현재 72점에서 80점 이상으로 올리려면:\n\n1. 수분 점수 높이기 (65→75)\n   - 히알루론산 세럼 추가\n   - 하루 물 2L 마시기\n\n2. 유분 밸런스 맞추기 (78→72)\n   - 가벼운 수분크림으로 교체\n   - 주 1회 클레이 마스크\n\n3. 모공 관리 유지 (70→75)\n   - BHA 토너 주 2회 사용\n\n꾸준히 2주만 관리하면 확실한 변화를 느끼실 수 있을 거예요!"
    }
    return "좋은 질문이에요!\n\n분석 결과에 따르면 현재 복합성 피부로, 수분 관리와 유분 밸런스가 핵심이에요.\n\n더 구체적으로 알고 싶으신 부분이 있다면 말씀해주세요. 제품 추천, 루틴 구성, 특정 피부 고민 등 무엇이든 도와드릴게요!"
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <>
      <Header title="AI 피부 상담" showBack />
      <PageContainer hasBottomNav noPadding>
        <div className="flex flex-col h-[calc(100vh-56px-80px-max(0px,env(safe-area-inset-top))-max(0px,env(safe-area-inset-bottom)))]">
          {/* Messages Area */}
          <div className="flex-1 overflow-y-auto">
            <div className="px-5 py-4 space-y-4">
              {/* 분석 결과 요약 카드 */}
              <AnalysisSummaryCard
                overallScore={72}
                skinType="복합성"
                mainConcern="T존 유분, 볼 건조"
                hydration={65}
                sebum={78}
                pores={70}
              />

              {/* Chat Messages */}
              {messages.map((message) => (
                <ChatBubble key={message.id} message={message} />
              ))}

              {/* Loading Indicator */}
              {isLoading && <ChatTypingIndicator />}

              <div ref={messagesEndRef} />
            </div>
          </div>

          {/* Suggested Questions */}
          {showSuggestions && (
            <div className="px-5 py-3 border-t border-border bg-card/80 backdrop-blur-sm">
              <p className="text-xs text-muted-foreground mb-3 font-medium">
                추천 질문
              </p>
              <div className="flex flex-col gap-2">
                {suggestedQuestions.map((question, index) => (
                  <button
                    key={index}
                    onClick={() => handleSend(question)}
                    className="w-full text-left px-4 py-3 text-sm bg-secondary text-secondary-foreground rounded-xl hover:bg-accent transition-colors border border-transparent hover:border-primary/20"
                  >
                    {question}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input Area */}
          <div className="px-5 py-4 border-t border-border bg-card">
            <div className="flex items-center gap-3">
              <div className="flex-1 relative">
                <input
                  ref={inputRef}
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="궁금한 점을 물어보세요"
                  className="w-full h-12 pl-4 pr-4 bg-input rounded-xl text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 transition-shadow"
                />
              </div>
              <Button
                size="icon"
                onClick={() => handleSend()}
                disabled={!input.trim() || isLoading}
                className="w-12 h-12 rounded-xl shrink-0"
              >
                <Send className="h-5 w-5" />
                <span className="sr-only">보내기</span>
              </Button>
            </div>
          </div>
        </div>
      </PageContainer>
      <BottomNav />
    </>
  )
}
