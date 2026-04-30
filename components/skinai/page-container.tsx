import { cn } from "@/lib/utils"

interface PageContainerProps {
  children: React.ReactNode
  className?: string
  hasBottomNav?: boolean
  hasHeader?: boolean
  noPadding?: boolean
  maxWidth?: "sm" | "md" | "lg" | "xl"
}

const maxWidthClasses = {
  sm: "max-w-sm",
  md: "max-w-md",
  lg: "max-w-lg",
  xl: "max-w-2xl",
}

export function PageContainer({
  children,
  className,
  hasBottomNav = true,
  hasHeader = true,
  noPadding = false,
  maxWidth = "lg",
}: PageContainerProps) {
  return (
    <main
      className={cn(
        "min-h-screen bg-background",
        hasHeader && "pt-[calc(56px+max(0px,env(safe-area-inset-top)))]",
        hasBottomNav && "pb-[calc(80px+max(0px,env(safe-area-inset-bottom)))]",
        !noPadding && "px-5 md:px-6",
        className
      )}
    >
      <div className={cn("mx-auto", maxWidthClasses[maxWidth])}>
        {children}
      </div>
    </main>
  )
}
