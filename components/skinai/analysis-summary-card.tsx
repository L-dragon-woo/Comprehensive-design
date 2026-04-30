"use client"

import { Droplets, Sun, CircleDot } from "lucide-react"
import { cn } from "@/lib/utils"
import { getScoreColor, getScoreBgColor } from "@/lib/skinai-types"

interface AnalysisSummaryCardProps {
  overallScore: number
  skinType: string
  mainConcern: string
  hydration: number
  sebum: number
  pores: number
  className?: string
}

export function AnalysisSummaryCard({
  overallScore,
  skinType,
  mainConcern,
  hydration,
  sebum,
  pores,
  className,
}: AnalysisSummaryCardProps) {
  const details = [
    { label: "수분", score: hydration, icon: Droplets },
    { label: "유분", score: sebum, icon: Sun },
    { label: "모공", score: pores, icon: CircleDot },
  ]

  return (
    <div className={cn("bg-card rounded-2xl p-4 shadow-sm border border-border", className)}>
      <div className="flex items-center gap-3 mb-4">
        <div
          className={cn(
            "flex items-center justify-center w-14 h-14 rounded-full",
            getScoreBgColor(overallScore)
          )}
        >
          <span
            className={cn(
              "text-xl font-bold",
              getScoreColor(overallScore)
            )}
          >
            {overallScore}
          </span>
        </div>
        <div>
          <p className="text-sm font-medium text-foreground">
            {skinType} 피부
          </p>
          <p className="text-xs text-muted-foreground">
            {mainConcern}
          </p>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-3">
        {details.map((item) => (
          <div
            key={item.label}
            className="flex flex-col items-center gap-1 p-2 rounded-xl bg-secondary"
          >
            <item.icon className="h-4 w-4 text-muted-foreground" />
            <span className="text-xs text-muted-foreground">
              {item.label}
            </span>
            <span
              className={cn(
                "text-sm font-semibold",
                getScoreColor(item.score)
              )}
            >
              {item.score}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
