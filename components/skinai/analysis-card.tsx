"use client"

import { ChevronRight } from "lucide-react"
import { cn } from "@/lib/utils"
import type { LucideIcon } from "lucide-react"

interface AnalysisCardProps {
  icon: LucideIcon
  iconColor?: string
  iconBg?: string
  title: string
  subtitle?: string
  value?: string | number
  valueLabel?: string
  onClick?: () => void
  className?: string
}

export function AnalysisCard({
  icon: Icon,
  iconColor = "text-primary",
  iconBg = "bg-primary/10",
  title,
  subtitle,
  value,
  valueLabel,
  onClick,
  className,
}: AnalysisCardProps) {
  const Component = onClick ? "button" : "div"

  return (
    <Component
      onClick={onClick}
      className={cn(
        "w-full bg-card rounded-2xl p-4 shadow-sm border border-border text-left",
        "transition-all duration-200",
        onClick && "hover:shadow-md hover:border-primary/20 active:scale-[0.98]",
        className
      )}
    >
      <div className="flex items-center gap-4">
        <div
          className={cn(
            "flex items-center justify-center w-12 h-12 rounded-xl",
            iconBg
          )}
        >
          <Icon className={cn("h-6 w-6", iconColor)} />
        </div>
        
        <div className="flex-1 min-w-0">
          <p className="text-base font-semibold text-foreground truncate">
            {title}
          </p>
          {subtitle && (
            <p className="text-sm text-muted-foreground truncate">{subtitle}</p>
          )}
        </div>
        
        {value !== undefined && (
          <div className="text-right">
            <p className="text-lg font-bold text-foreground">{value}</p>
            {valueLabel && (
              <p className="text-xs text-muted-foreground">{valueLabel}</p>
            )}
          </div>
        )}
        
        {onClick && (
          <ChevronRight className="h-5 w-5 text-muted-foreground" />
        )}
      </div>
    </Component>
  )
}
