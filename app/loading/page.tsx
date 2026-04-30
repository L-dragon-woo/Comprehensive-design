"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Sparkles, CheckCircle2 } from "lucide-react"
import { cn } from "@/lib/utils"

const analysisSteps = [
  { id: 1, label: "피부 영역 감지 중...", duration: 1500 },
  { id: 2, label: "피부 톤 분석 중...", duration: 1200 },
  { id: 3, label: "모공 및 결 분석 중...", duration: 1300 },
  { id: 4, label: "피부 고민 파악 중...", duration: 1000 },
  { id: 5, label: "맞춤 솔루션 생성 중...", duration: 800 },
]

export default function LoadingPage() {
  const [currentStep, setCurrentStep] = useState(0)
  const [completedSteps, setCompletedSteps] = useState<number[]>([])
  const router = useRouter()

  useEffect(() => {
    let stepIndex = 0
    
    const processStep = () => {
      if (stepIndex < analysisSteps.length) {
        setCurrentStep(stepIndex)
        
        setTimeout(() => {
          setCompletedSteps((prev) => [...prev, stepIndex])
          stepIndex++
          processStep()
        }, analysisSteps[stepIndex].duration)
      } else {
        // All steps completed, navigate to results
        setTimeout(() => {
          router.push("/result")
        }, 500)
      }
    }
    
    processStep()
  }, [router])

  const progress = ((completedSteps.length) / analysisSteps.length) * 100

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-5 py-12">
      <div className="w-full max-w-sm">
        {/* Animated Icon */}
        <div className="relative mb-10 mx-auto w-fit">
          <div className="w-28 h-28 rounded-full bg-primary/10 flex items-center justify-center">
            <div className="w-20 h-20 rounded-full bg-primary/20 flex items-center justify-center animate-pulse">
              <Sparkles className="h-10 w-10 text-primary" />
            </div>
          </div>
          
          {/* Rotating Ring */}
          <svg
            className="absolute inset-0 w-28 h-28"
            viewBox="0 0 100 100"
            style={{ animation: "spin 3s linear infinite" }}
          >
            <circle
              cx="50"
              cy="50"
              r="46"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeDasharray="60 200"
              className="text-primary/30"
            />
          </svg>
        </div>

        {/* Title */}
        <h1 className="text-xl font-bold text-foreground mb-2 text-center text-balance">
          AI가 피부를 분석하고 있어요
        </h1>
        <p className="text-sm text-muted-foreground mb-10 text-center">
          잠시만 기다려주세요
        </p>

        {/* Progress Bar */}
        <div className="w-full mb-8">
          <div className="h-2 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all duration-500 ease-out"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-sm text-muted-foreground text-center mt-3 tabular-nums">
            {Math.round(progress)}% 완료
          </p>
        </div>

        {/* Analysis Steps */}
        <div className="space-y-3">
          {analysisSteps.map((step, index) => {
            const isCompleted = completedSteps.includes(index)
            const isCurrent = currentStep === index && !isCompleted
            
            return (
              <div
                key={step.id}
                className={cn(
                  "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300",
                  isCompleted && "bg-success/10",
                  isCurrent && "bg-primary/10",
                  !isCompleted && !isCurrent && "opacity-40"
                )}
              >
                <div
                  className={cn(
                    "w-6 h-6 rounded-full flex items-center justify-center transition-all shrink-0",
                    isCompleted && "bg-success",
                    isCurrent && "bg-primary animate-pulse"
                  )}
                >
                  {isCompleted ? (
                    <CheckCircle2 className="h-4 w-4 text-success-foreground" />
                  ) : isCurrent ? (
                    <div className="w-2 h-2 bg-primary-foreground rounded-full" />
                  ) : (
                    <div className="w-2 h-2 bg-muted-foreground/30 rounded-full" />
                  )}
                </div>
                <span
                  className={cn(
                    "text-sm font-medium transition-colors",
                    isCompleted && "text-success",
                    isCurrent && "text-primary",
                    !isCompleted && !isCurrent && "text-muted-foreground"
                  )}
                >
                  {step.label}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      <style jsx>{`
        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      `}</style>
    </div>
  )
}
