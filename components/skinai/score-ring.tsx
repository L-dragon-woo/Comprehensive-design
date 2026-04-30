"use client"

import { cn } from "@/lib/utils"
import { getScoreColor } from "@/lib/skinai-types"
import { useEffect, useState } from "react"

interface ScoreRingProps {
  score: number
  size?: number
  strokeWidth?: number
  animated?: boolean
  className?: string
  showLabel?: boolean
}

export function ScoreRing({
  score,
  size = 120,
  strokeWidth = 8,
  animated = true,
  className,
  showLabel = true,
}: ScoreRingProps) {
  const [displayScore, setDisplayScore] = useState(animated ? 0 : score)
  const [progress, setProgress] = useState(animated ? 0 : score)

  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const strokeDashoffset = circumference - (progress / 100) * circumference

  useEffect(() => {
    if (!animated) return

    // Animate score number
    const duration = 1000
    const startTime = Date.now()
    const startScore = 0

    const animateScore = () => {
      const elapsed = Date.now() - startTime
      const progress = Math.min(elapsed / duration, 1)
      const easeOut = 1 - Math.pow(1 - progress, 3)
      
      setDisplayScore(Math.round(startScore + (score - startScore) * easeOut))
      setProgress(score * easeOut)

      if (progress < 1) {
        requestAnimationFrame(animateScore)
      }
    }

    requestAnimationFrame(animateScore)
  }, [score, animated])

  return (
    <div className={cn("relative inline-flex items-center justify-center", className)}>
      <svg width={size} height={size} className="-rotate-90">
        {/* Background circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-muted"
        />
        {/* Progress circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          className={cn(
            "transition-all duration-1000 ease-out",
            getScoreColor(score)
          )}
        />
      </svg>
      {showLabel && (
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className={cn("text-3xl font-bold", getScoreColor(score))}>
            {displayScore}
          </span>
          <span className="text-xs text-muted-foreground">점</span>
        </div>
      )}
    </div>
  )
}
