"use client"

import { ArrowLeft, Bell, Sparkles } from "lucide-react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface HeaderProps {
  title?: string
  showBack?: boolean
  showNotification?: boolean
  showLogo?: boolean
  transparent?: boolean
  className?: string
}

export function Header({
  title,
  showBack = false,
  showNotification = false,
  showLogo = false,
  transparent = false,
  className,
}: HeaderProps) {
  const router = useRouter()

  return (
    <header
      className={cn(
        "fixed top-0 left-0 right-0 z-50",
        "pt-[max(0px,env(safe-area-inset-top))]",
        transparent ? "bg-transparent" : "bg-card/95 backdrop-blur-xl border-b border-border",
        className
      )}
    >
      <div className="mx-auto max-w-lg lg:max-w-2xl">
        <div className="flex items-center justify-between h-14 px-4">
          <div className="w-10 flex items-center">
            {showBack && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => router.back()}
                className="rounded-full hover:bg-muted"
              >
                <ArrowLeft className="h-5 w-5" />
                <span className="sr-only">뒤로 가기</span>
              </Button>
            )}
            {showLogo && !showBack && (
              <Link href="/" className="flex items-center gap-2">
                <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-primary">
                  <Sparkles className="h-4 w-4 text-primary-foreground" />
                </div>
                <span className="text-lg font-bold text-foreground hidden sm:inline">SkinAI</span>
              </Link>
            )}
          </div>
          
          {title && (
            <h1 className="text-base font-semibold text-foreground absolute left-1/2 -translate-x-1/2">
              {title}
            </h1>
          )}
          
          <div className="w-10 flex items-center justify-end">
            {showNotification && (
              <Button
                variant="ghost"
                size="icon"
                className="rounded-full hover:bg-muted relative"
              >
                <Bell className="h-5 w-5" />
                <span className="absolute top-2 right-2 w-2 h-2 bg-primary rounded-full animate-pulse" />
                <span className="sr-only">알림</span>
              </Button>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}
