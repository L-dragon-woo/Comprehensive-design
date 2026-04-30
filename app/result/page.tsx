"use client"

import { Droplets, Sun, Sparkles, CircleDot, MessageCircle, Download, Share2, Home } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Header } from "@/components/skinai/header"
import { PageContainer } from "@/components/skinai/page-container"
import { AnalysisCard } from "@/components/skinai/analysis-card"
import { ScoreRing } from "@/components/skinai/score-ring"
import { cn } from "@/lib/utils"
import Link from "next/link"

const skinAnalysisData = {
  overallScore: 78,
  date: "2026년 4월 30일",
  skinType: "복합성",
  metrics: [
    {
      id: "hydration",
      icon: Droplets,
      iconColor: "text-primary",
      iconBg: "bg-primary/10",
      title: "수분",
      score: 72,
      status: "보통",
      description: "피부 수분이 약간 부족해요",
    },
    {
      id: "sebum",
      icon: Sun,
      iconColor: "text-warning",
      iconBg: "bg-warning/10",
      title: "유분",
      score: 65,
      status: "주의",
      description: "T존 유분이 과다해요",
    },
    {
      id: "pores",
      icon: CircleDot,
      iconColor: "text-success",
      iconBg: "bg-success/10",
      title: "모공",
      score: 85,
      status: "좋음",
      description: "모공 상태가 양호해요",
    },
    {
      id: "pigmentation",
      icon: CircleDot,
      iconColor: "text-destructive",
      iconBg: "bg-destructive/10",
      title: "색소침착",
      score: 68,
      status: "보통",
      description: "볼 부근에 색소침착이 있어요",
    },
  ],
  concerns: ["T존 유분 과다", "볼 색소침착", "수분 부족"],
  recommendations: [
    "아침 세안 후 수분 토너 사용",
    "자외선 차단제 꼼꼼히 바르기",
    "주 2회 각질 케어 추천",
  ],
}

export default function ResultPage() {
  return (
    <>
      <Header title="분석 결과" showBack />
      <PageContainer hasBottomNav={false}>
        {/* Overall Score Section */}
        <section className="py-6">
          <div className="bg-card rounded-3xl p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-6">
              <div className="flex-1">
                <p className="text-sm text-muted-foreground mb-1">
                  {skinAnalysisData.date}
                </p>
                <h2 className="text-xl font-bold text-foreground mb-1">
                  종합 피부 점수
                </h2>
                <p className="text-sm text-muted-foreground">
                  {skinAnalysisData.skinType} 피부
                </p>
              </div>
              <ScoreRing score={skinAnalysisData.overallScore} size={100} strokeWidth={7} />
            </div>
            
            {/* Concerns Tags */}
            <div className="flex flex-wrap gap-2">
              {skinAnalysisData.concerns.map((concern, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-3 py-1.5 rounded-full bg-accent text-accent-foreground text-xs font-medium"
                >
                  {concern}
                </span>
              ))}
            </div>
          </div>
        </section>

        {/* Detailed Analysis Section */}
        <section className="py-4">
          <h3 className="text-lg font-semibold text-foreground mb-4">
            상세 분석
          </h3>
          <div className="space-y-3">
            {skinAnalysisData.metrics.map((metric) => (
              <AnalysisCard
                key={metric.id}
                icon={metric.icon}
                iconColor={metric.iconColor}
                iconBg={metric.iconBg}
                title={metric.title}
                subtitle={metric.description}
                value={metric.score}
                valueLabel={metric.status}
              />
            ))}
          </div>
        </section>

        {/* AI Recommendations Section */}
        <section className="py-4">
          <h3 className="text-lg font-semibold text-foreground mb-4">
            AI 추천 케어
          </h3>
          <div className="bg-secondary rounded-2xl p-5">
            <div className="flex items-start gap-3 mb-4">
              <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-card shrink-0">
                <Sparkles className="h-5 w-5 text-primary" />
              </div>
              <div>
                <h4 className="text-base font-semibold text-foreground">
                  맞춤 케어 솔루션
                </h4>
                <p className="text-sm text-muted-foreground">
                  분석 결과를 바탕으로 추천드려요
                </p>
              </div>
            </div>
            <ul className="space-y-3">
              {skinAnalysisData.recommendations.map((rec, index) => (
                <li key={index} className="flex items-start gap-3">
                  <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/10 text-primary text-xs font-bold shrink-0">
                    {index + 1}
                  </span>
                  <span className="text-sm text-foreground leading-relaxed">{rec}</span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* Action Buttons */}
        <section className="py-6 space-y-3">
          <Link href="/chat" className="block">
            <Button
              size="lg"
              className="w-full h-14 rounded-2xl text-base font-semibold shadow-lg shadow-primary/20"
            >
              <MessageCircle className="mr-2 h-5 w-5" />
              AI에게 더 물어보기
            </Button>
          </Link>
          
          <div className="flex gap-3">
            <Button
              variant="outline"
              size="lg"
              className="flex-1 h-12 rounded-xl"
            >
              <Download className="mr-2 h-4 w-4" />
              저장하기
            </Button>
            <Button
              variant="outline"
              size="lg"
              className="flex-1 h-12 rounded-xl"
            >
              <Share2 className="mr-2 h-4 w-4" />
              공유하기
            </Button>
          </div>

          <Link href="/" className="block">
            <Button
              variant="ghost"
              size="lg"
              className="w-full h-12 rounded-xl text-muted-foreground"
            >
              <Home className="mr-2 h-4 w-4" />
              홈으로 돌아가기
            </Button>
          </Link>
        </section>
      </PageContainer>
    </>
  )
}
