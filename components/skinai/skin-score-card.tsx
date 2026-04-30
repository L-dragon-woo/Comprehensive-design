"use client"

import { cn } from "@/lib/utils"

interface SkinScoreCardProps {
  score: number
  label: string
  description?: string
  className?: string
}

export function SkinScoreCard({
  score,
  label,
  description,
  className,
}: SkinScoreCardProps) {
  const getScoreColor = (score: number) => {
    if (score >= 80) return "text-success"
    if (score >= 60) return "text-primary"
    if (score >= 40) return "text-warning"
    return "text-destructive"
  }

  const getScoreBg = (score: number) => {
    if (score >= 80) return "bg-success/10"
    if (score >= 60) return "bg-primary/10"
    if (score >= 40) return "bg-warning/10"
    return "bg-destructive/10"
  }

  return (
    <div
      className={cn(
        "bg-card rounded-2xl p-5 shadow-sm border border-border",
        className
      )}
    >
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <p className="text-sm text-muted-foreground font-medium">{label}</p>
          {description && (
            <p className="text-xs text-muted-foreground">{description}</p>
          )}
        </div>
        <div
          className={cn(
            "flex items-center justify-center w-16 h-16 rounded-full",
            getScoreBg(score)
          )}
        >
          <span className={cn("text-2xl font-bold", getScoreColor(score))}>
            {score}
          </span>
        </div>
      </div>
    </div>
  )
}
