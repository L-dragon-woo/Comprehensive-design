"use client"

import { Camera, Sparkles, MessageCircle, TrendingUp, Shield, Clock, ArrowRight, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { BottomNav } from "@/components/skinai/bottom-nav"
import { Header } from "@/components/skinai/header"
import { PageContainer } from "@/components/skinai/page-container"
import { FeatureCard } from "@/components/skinai/feature-card"
import { ScoreRing } from "@/components/skinai/score-ring"
import Link from "next/link"
import { cn } from "@/lib/utils"

export default function HomePage() {
  // 최근 분석 결과가 있는지 여부 (실제로는 API나 로컬스토리지에서 가져옴)
  const hasRecentAnalysis = true
  const recentScore = 78
  const lastAnalysisDate = "2026.04.30"

  return (
    <>
      <Header showLogo showNotification />
      <PageContainer>
        {/* Hero Section */}
        <section className="py-8 md:py-12">
          <div className="flex items-center gap-2 mb-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-primary/10">
              <Sparkles className="h-5 w-5 text-primary" />
            </div>
            <span className="text-sm font-semibold text-primary">AI 피부 분석</span>
          </div>
          
          <h1 className="text-2xl md:text-3xl font-bold text-foreground leading-tight mb-3 text-balance">
            사진 한 장으로
            <br />
            <span className="text-primary">피부 상태</span>를 확인하세요
          </h1>
          
          <p className="text-muted-foreground text-base leading-relaxed mb-8">
            AI가 피부 고민을 분석하고
            <br className="md:hidden" />
            맞춤 솔루션을 제안해 드려요
          </p>
          
          <Link href="/capture" className="block">
            <Button 
              size="lg" 
              className="w-full h-14 text-base font-semibold rounded-2xl shadow-lg shadow-primary/25 hover:shadow-primary/30 hover:scale-[1.02] active:scale-[0.98] transition-all"
            >
              <Camera className="mr-2 h-5 w-5" />
              피부 분석 시작하기
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </section>

        {/* Recent Analysis Section */}
        {hasRecentAnalysis && (
          <section className="py-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-foreground">최근 분석 결과</h2>
              <Link 
                href="/history" 
                className="flex items-center gap-1 text-sm text-primary font-medium hover:underline"
              >
                전체보기
                <ChevronRight className="h-4 w-4" />
              </Link>
            </div>
            
            <Link href="/result" className="block group">
              <div className="bg-card rounded-2xl p-5 shadow-sm border border-border hover:border-primary/30 hover:shadow-md transition-all">
                <div className="flex items-center gap-5">
                  <ScoreRing score={recentScore} size={80} strokeWidth={6} />
                  <div className="flex-1">
                    <p className="text-sm text-muted-foreground mb-1">
                      {lastAnalysisDate} 분석
                    </p>
                    <p className="text-lg font-semibold text-foreground mb-2">
                      종합 피부 점수
                    </p>
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-success/10 text-success text-xs font-medium">
                        <TrendingUp className="h-3 w-3 mr-1" />
                        +5점
                      </span>
                      <span className="text-xs text-muted-foreground">
                        지난 분석 대비
                      </span>
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground group-hover:text-primary transition-colors" />
                </div>
              </div>
            </Link>
          </section>
        )}

        {/* Features Section */}
        <section className="py-6">
          <h2 className="text-lg font-semibold text-foreground mb-4">
            SkinAI로 할 수 있는 것
          </h2>
          
          <div className="grid grid-cols-2 gap-3">
            <FeatureCard
              href="/capture"
              icon={Camera}
              title="피부 촬영"
              description="AI가 정확하게 분석해요"
            />
            <FeatureCard
              href="/chat"
              icon={MessageCircle}
              iconColor="text-success"
              iconBg="bg-success/10"
              title="AI 상담"
              description="궁금한 점을 물어보세요"
            />
            <FeatureCard
              href="/history"
              icon={TrendingUp}
              iconColor="text-warning"
              iconBg="bg-warning/10"
              title="변화 추적"
              description="피부 변화를 기록해요"
            />
            <FeatureCard
              href="/history"
              icon={Clock}
              iconColor="text-muted-foreground"
              iconBg="bg-muted"
              title="분석 기록"
              description="지난 결과를 확인해요"
            />
          </div>
        </section>

        {/* Trust Section */}
        <section className="py-6 pb-8">
          <div className="bg-secondary rounded-2xl p-5">
            <div className="flex items-start gap-4">
              <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-card shrink-0">
                <Shield className="h-5 w-5 text-primary" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-foreground mb-1">
                  안전한 데이터 관리
                </h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  사진과 분석 결과는 암호화되어 안전하게 보관되며,
                  오직 본인만 확인할 수 있어요.
                </p>
              </div>
            </div>
          </div>
        </section>
      </PageContainer>
      <BottomNav />
    </>
  )
}
