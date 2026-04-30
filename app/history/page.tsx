"use client"

import { useState } from "react"
import { Calendar, TrendingUp, TrendingDown, Minus, ChevronRight, Sparkles, ArrowUpRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Header } from "@/components/skinai/header"
import { PageContainer } from "@/components/skinai/page-container"
import { BottomNav } from "@/components/skinai/bottom-nav"
import { cn } from "@/lib/utils"
import Link from "next/link"

interface AnalysisRecord {
  id: string
  date: string
  dateFormatted: string
  score: number
  change: number
  improvements: string[]
}

const historyData: AnalysisRecord[] = [
  {
    id: "1",
    date: "2026-04-30",
    dateFormatted: "2026.04.30",
    score: 82,
    change: 5,
    improvements: ["수분 개선", "모공 케어"],
  },
  {
    id: "2",
    date: "2026-04-23",
    dateFormatted: "2026.04.23",
    score: 77,
    change: 3,
    improvements: ["유분 조절", "색소 개선"],
  },
  {
    id: "3",
    date: "2026-04-16",
    dateFormatted: "2026.04.16",
    score: 74,
    change: -2,
    improvements: ["수분 관리 필요"],
  },
  {
    id: "4",
    date: "2026-04-09",
    dateFormatted: "2026.04.09",
    score: 76,
    change: 4,
    improvements: ["피부결 개선", "탄력 증가"],
  },
  {
    id: "5",
    date: "2026-04-02",
    dateFormatted: "2026.04.02",
    score: 72,
    change: 0,
    improvements: ["전체적 안정"],
  },
]

function ScoreChangeIndicator({ change, size = "sm" }: { change: number; size?: "sm" | "lg" }) {
  const iconSize = size === "lg" ? "h-4 w-4" : "h-3 w-3"
  const textSize = size === "lg" ? "text-sm" : "text-xs"
  
  if (change > 0) {
    return (
      <span className={cn("inline-flex items-center gap-0.5 font-semibold text-success", textSize)}>
        <TrendingUp className={iconSize} />
        +{change}점
      </span>
    )
  }
  if (change < 0) {
    return (
      <span className={cn("inline-flex items-center gap-0.5 font-semibold text-destructive", textSize)}>
        <TrendingDown className={iconSize} />
        {change}점
      </span>
    )
  }
  return (
    <span className={cn("inline-flex items-center gap-0.5 font-medium text-muted-foreground", textSize)}>
      <Minus className={iconSize} />
      변동없음
    </span>
  )
}

function ScoreTrendChart({ data }: { data: AnalysisRecord[] }) {
  const chartData = [...data].reverse()
  const maxScore = Math.max(...chartData.map(d => d.score))
  const minScore = Math.min(...chartData.map(d => d.score))
  const range = maxScore - minScore || 1
  
  // SVG chart dimensions
  const width = 100
  const height = 60
  const padding = 10
  
  const points = chartData.map((record, index) => {
    const x = padding + (index / (chartData.length - 1)) * (width - padding * 2)
    const y = height - padding - ((record.score - minScore) / range) * (height - padding * 2)
    return { x, y, score: record.score, date: record.dateFormatted }
  })
  
  const pathD = points.reduce((acc, point, index) => {
    if (index === 0) return `M ${point.x} ${point.y}`
    return `${acc} L ${point.x} ${point.y}`
  }, "")

  return (
    <div className="bg-card rounded-2xl p-5 border border-border shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-semibold text-foreground">피부 점수 변화</h3>
        <span className="text-xs text-muted-foreground">최근 5회 분석</span>
      </div>
      
      <div className="relative h-40">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-32"
          preserveAspectRatio="none"
        >
          {/* Gradient fill under line */}
          <defs>
            <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="var(--color-primary)" stopOpacity="0.3" />
              <stop offset="100%" stopColor="var(--color-primary)" stopOpacity="0" />
            </linearGradient>
          </defs>
          
          {/* Area under the line */}
          <path
            d={`${pathD} L ${points[points.length - 1].x} ${height - padding} L ${points[0].x} ${height - padding} Z`}
            fill="url(#scoreGradient)"
          />
          
          {/* Line */}
          <path
            d={pathD}
            fill="none"
            stroke="var(--color-primary)"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          
          {/* Points */}
          {points.map((point, index) => (
            <circle
              key={index}
              cx={point.x}
              cy={point.y}
              r="3"
              fill="var(--color-card)"
              stroke="var(--color-primary)"
              strokeWidth="2"
            />
          ))}
        </svg>
        
        {/* X-axis labels */}
        <div className="flex justify-between px-2 mt-2">
          {chartData.map((record, index) => (
            <span key={index} className="text-xs text-muted-foreground tabular-nums">
              {record.dateFormatted.slice(5)}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}

function RecentAnalysisSummary({ record }: { record: AnalysisRecord }) {
  return (
    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-2xl p-5 text-primary-foreground shadow-lg">
      <div className="flex items-start justify-between mb-4">
        <div>
          <p className="text-sm opacity-90 mb-1">최근 분석일</p>
          <p className="text-lg font-semibold">{record.dateFormatted}</p>
        </div>
        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-primary-foreground/20">
          <Calendar className="h-5 w-5" />
        </div>
      </div>
      
      <div className="flex items-end justify-between">
        <div>
          <p className="text-sm opacity-90 mb-1">최근 피부 점수</p>
          <div className="flex items-baseline gap-2">
            <span className="text-4xl font-bold tabular-nums">{record.score}</span>
            <span className="text-lg opacity-90">점</span>
          </div>
        </div>
        
        <div className="text-right">
          <p className="text-sm opacity-90 mb-1">지난 분석 대비</p>
          <div className={cn(
            "inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-sm font-semibold",
            record.change > 0 
              ? "bg-primary-foreground/20 text-primary-foreground" 
              : record.change < 0 
                ? "bg-destructive/30 text-primary-foreground"
                : "bg-primary-foreground/10 text-primary-foreground/80"
          )}>
            {record.change > 0 ? (
              <>
                <TrendingUp className="h-4 w-4" />
                +{record.change}점
              </>
            ) : record.change < 0 ? (
              <>
                <TrendingDown className="h-4 w-4" />
                {record.change}점
              </>
            ) : (
              <>
                <Minus className="h-4 w-4" />
                변동없음
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function HistoryRecordCard({ record }: { record: AnalysisRecord }) {
  return (
    <div className="bg-card rounded-2xl p-4 border border-border shadow-sm hover:border-primary/30 hover:shadow-md transition-all">
      <div className="flex items-start gap-4">
        {/* Date Icon */}
        <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-secondary shrink-0">
          <Calendar className="h-5 w-5 text-muted-foreground" />
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <p className="text-base font-semibold text-foreground">
              {record.dateFormatted}
            </p>
            <ScoreChangeIndicator change={record.change} />
          </div>
          
          {/* Score */}
          <div className="flex items-baseline gap-1 mb-3">
            <span className="text-2xl font-bold text-foreground tabular-nums">{record.score}</span>
            <span className="text-sm text-muted-foreground">점</span>
          </div>
          
          {/* Improvements */}
          <div className="mb-3">
            <p className="text-xs text-muted-foreground mb-1.5">주요 개선 항목</p>
            <div className="flex flex-wrap gap-1.5">
              {record.improvements.map((item, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-1 rounded-full bg-accent text-accent-foreground text-xs font-medium"
                >
                  {item}
                </span>
              ))}
            </div>
          </div>
          
          {/* Detail Button */}
          <Link href={`/result?id=${record.id}`}>
            <Button 
              variant="outline" 
              size="sm" 
              className="w-full rounded-xl text-sm font-medium hover:bg-primary hover:text-primary-foreground hover:border-primary transition-colors"
            >
              상세 보기
              <ArrowUpRight className="h-4 w-4 ml-1" />
            </Button>
          </Link>
        </div>
      </div>
    </div>
  )
}

export default function HistoryPage() {
  const [filter, setFilter] = useState<"all" | "month" | "week">("all")
  
  const latestRecord = historyData[0]

  const filteredData = historyData.filter(record => {
    if (filter === "all") return true
    const recordDate = new Date(record.date)
    const now = new Date()
    if (filter === "week") {
      const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      return recordDate >= weekAgo
    }
    if (filter === "month") {
      const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
      return recordDate >= monthAgo
    }
    return true
  })

  return (
    <>
      <Header title="분석 기록" showBack showNotification />
      <PageContainer>
        {/* Recent Analysis Summary Card */}
        <section className="py-6">
          <RecentAnalysisSummary record={latestRecord} />
        </section>

        {/* Score Trend Chart */}
        <section className="pb-6">
          <ScoreTrendChart data={historyData} />
        </section>

        {/* Filter Tabs */}
        <section className="pb-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-foreground">날짜별 분석 기록</h3>
            <div className="flex items-center gap-1 p-1 bg-muted rounded-xl">
              {(["all", "month", "week"] as const).map((f) => (
                <button
                  key={f}
                  onClick={() => setFilter(f)}
                  className={cn(
                    "px-3 py-1.5 text-xs font-medium rounded-lg transition-all",
                    filter === f
                      ? "bg-card text-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {f === "all" ? "전체" : f === "month" ? "이번 달" : "이번 주"}
                </button>
              ))}
            </div>
          </div>

          {/* History List */}
          <div className="space-y-3">
            {filteredData.map((record) => (
              <HistoryRecordCard key={record.id} record={record} />
            ))}
          </div>

          {filteredData.length === 0 && (
            <div className="py-12 text-center">
              <p className="text-muted-foreground">해당 기간에 분석 기록이 없어요</p>
            </div>
          )}
        </section>

        {/* Empty State */}
        {historyData.length === 0 && (
          <div className="py-20 text-center">
            <div className="flex items-center justify-center w-16 h-16 rounded-full bg-muted mx-auto mb-4">
              <Sparkles className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              아직 분석 기록이 없어요
            </h3>
            <p className="text-sm text-muted-foreground mb-6">
              첫 번째 피부 분석을 시작해보세요
            </p>
            <Link href="/capture">
              <Button className="rounded-xl">피부 분석 시작하기</Button>
            </Link>
          </div>
        )}
      </PageContainer>
      <BottomNav />
    </>
  )
}
