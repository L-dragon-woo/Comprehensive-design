"use client"

import { Sparkles, User } from "lucide-react"
import { cn } from "@/lib/utils"
import type { ChatMessage } from "@/lib/skinai-types"

interface ChatBubbleProps {
  message: ChatMessage
}

export function ChatBubble({ message }: ChatBubbleProps) {
  const isAssistant = message.role === "assistant"

  return (
    <div
      className={cn(
        "flex items-end gap-2",
        !isAssistant && "flex-row-reverse"
      )}
    >
      {/* Avatar */}
      {isAssistant ? (
        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 shrink-0 mb-1">
          <Sparkles className="h-4 w-4 text-primary" />
        </div>
      ) : (
        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-muted shrink-0 mb-1">
          <User className="h-4 w-4 text-muted-foreground" />
        </div>
      )}

      {/* Message Bubble */}
      <div
        className={cn(
          "max-w-[80%] rounded-2xl px-4 py-3",
          isAssistant
            ? "bg-secondary text-foreground rounded-bl-sm"
            : "bg-primary text-primary-foreground rounded-br-sm"
        )}
      >
        <p className="text-sm whitespace-pre-wrap leading-relaxed">
          {message.content}
        </p>
      </div>
    </div>
  )
}

export function ChatTypingIndicator() {
  return (
    <div className="flex items-end gap-2">
      <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 shrink-0 mb-1">
        <Sparkles className="h-4 w-4 text-primary" />
      </div>
      <div className="bg-secondary rounded-2xl rounded-bl-sm px-4 py-3">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce [animation-delay:-0.3s]" />
          <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce [animation-delay:-0.15s]" />
          <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce" />
        </div>
      </div>
    </div>
  )
}
