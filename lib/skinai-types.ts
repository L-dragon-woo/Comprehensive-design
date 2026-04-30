// SkinAI Types

export interface SkinAnalysisResult {
  id: string
  date: string
  dateFormatted: string
  overallScore: number
  skinType: "건성" | "지성" | "복합성" | "중성"
  metrics: SkinMetric[]
  concerns: string[]
  recommendations: string[]
  change?: number
  improvements?: string[]
}

export interface SkinMetric {
  id: string
  title: string
  score: number
  status: "좋음" | "보통" | "주의"
  description: string
}

export interface ChatMessage {
  id: string
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

// Score color utilities
export function getScoreColor(score: number): string {
  if (score >= 80) return "text-success"
  if (score >= 60) return "text-primary"
  if (score >= 40) return "text-warning"
  return "text-destructive"
}

export function getScoreBgColor(score: number): string {
  if (score >= 80) return "bg-success/10"
  if (score >= 60) return "bg-primary/10"
  if (score >= 40) return "bg-warning/10"
  return "bg-destructive/10"
}

export function getScoreStatus(score: number): string {
  if (score >= 80) return "좋음"
  if (score >= 60) return "보통"
  if (score >= 40) return "주의"
  return "관리 필요"
}

// Mock data for demo
export const mockAnalysisResult: SkinAnalysisResult = {
  id: "latest",
  date: "2026-04-30",
  dateFormatted: "2026.04.30",
  overallScore: 78,
  skinType: "복합성",
  metrics: [
    {
      id: "hydration",
      title: "수분",
      score: 72,
      status: "보통",
      description: "피부 수분이 약간 부족해요",
    },
    {
      id: "sebum",
      title: "유분",
      score: 65,
      status: "주의",
      description: "T존 유분이 과다해요",
    },
    {
      id: "pores",
      title: "모공",
      score: 85,
      status: "좋음",
      description: "모공 상태가 양호해요",
    },
    {
      id: "pigmentation",
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
  change: 5,
  improvements: ["수분 개선", "모공 케어"],
}

export const mockHistoryData: SkinAnalysisResult[] = [
  {
    ...mockAnalysisResult,
    id: "1",
    overallScore: 82,
    change: 5,
    improvements: ["수분 개선", "모공 케어"],
  },
  {
    ...mockAnalysisResult,
    id: "2",
    date: "2026-04-23",
    dateFormatted: "2026.04.23",
    overallScore: 77,
    change: 3,
    improvements: ["유분 조절", "색소 개선"],
  },
  {
    ...mockAnalysisResult,
    id: "3",
    date: "2026-04-16",
    dateFormatted: "2026.04.16",
    overallScore: 74,
    change: -2,
    improvements: ["수분 관리 필요"],
  },
  {
    ...mockAnalysisResult,
    id: "4",
    date: "2026-04-09",
    dateFormatted: "2026.04.09",
    overallScore: 76,
    change: 4,
    improvements: ["피부결 개선", "탄력 증가"],
  },
  {
    ...mockAnalysisResult,
    id: "5",
    date: "2026-04-02",
    dateFormatted: "2026.04.02",
    overallScore: 72,
    change: 0,
    improvements: ["전체적 안정"],
  },
]
