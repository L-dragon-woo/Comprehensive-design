"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Home, Camera, MessageCircle, Clock } from "lucide-react"
import { cn } from "@/lib/utils"

const navItems = [
  {
    href: "/",
    label: "홈",
    icon: Home,
  },
  {
    href: "/capture",
    label: "분석하기",
    icon: Camera,
  },
  {
    href: "/chat",
    label: "AI 상담",
    icon: MessageCircle,
  },
  {
    href: "/history",
    label: "기록",
    icon: Clock,
  },
]

export function BottomNav() {
  const pathname = usePathname()

  // 촬영, 로딩, 결과 페이지에서는 하단 네비게이션 숨김
  const hideOnPages = ["/capture", "/loading", "/result"]
  if (hideOnPages.includes(pathname)) return null

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-card/95 backdrop-blur-lg border-t border-border">
      <div className="mx-auto max-w-lg">
        <div className="flex items-center justify-around py-2 px-4 pb-[max(0.5rem,env(safe-area-inset-bottom))]">
          {navItems.map((item) => {
            const isActive = pathname === item.href
            const Icon = item.icon

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex flex-col items-center justify-center gap-1 min-w-[64px] py-2 rounded-xl transition-all duration-200",
                  isActive
                    ? "text-primary"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <div className={cn(
                  "relative flex items-center justify-center w-8 h-8 rounded-full transition-all duration-200",
                  isActive && "bg-primary/10"
                )}>
                  <Icon
                    className={cn(
                      "h-5 w-5 transition-transform duration-200",
                      isActive && "scale-110"
                    )}
                    strokeWidth={isActive ? 2.5 : 2}
                  />
                </div>
                <span className={cn(
                  "text-[11px] font-medium",
                  isActive && "font-semibold text-primary"
                )}>
                  {item.label}
                </span>
              </Link>
            )
          })}
        </div>
      </div>
    </nav>
  )
}
