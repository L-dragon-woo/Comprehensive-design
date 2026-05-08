export interface ChatMessage {
  id: string
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

export interface AnalysisRecord {
  id: string
  date: string
  dateFormatted: string
  score: number
  change: number
  improvements: string[]
}

export interface HospitalApplication {
  id: string
  hospitalName: string
  submittedAt: string
  status: "submitted" | "reviewing" | "confirmed"
  includedItems: string[]
}

const applicationStorageKey = "skinai:hospital-applications"

export function getHospitalApplications(): HospitalApplication[] {
  if (typeof window === "undefined") return []

  try {
    const stored = window.localStorage.getItem(applicationStorageKey)
    if (!stored) return []
    const parsed = JSON.parse(stored)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function saveHospitalApplication(application: HospitalApplication) {
  const applications = [application, ...getHospitalApplications()]
  window.localStorage.setItem(applicationStorageKey, JSON.stringify(applications))
  window.dispatchEvent(new CustomEvent("skinai:hospital-application-updated"))
}

export function formatApplicationDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "방금 전"

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date)
}

export function scoreColor(score: number) {
  if (score >= 80) return "text-success"
  if (score >= 60) return "text-primary"
  if (score >= 40) return "text-warning"
  return "text-destructive"
}

export function scoreBgColor(score: number) {
  if (score >= 80) return "bg-success/10"
  if (score >= 60) return "bg-primary/10"
  if (score >= 40) return "bg-warning/10"
  return "bg-destructive/10"
}

export const historyData: AnalysisRecord[] = [
  { id: "1", date: "2026-04-30", dateFormatted: "2026.04.30", score: 82, change: 5, improvements: ["수분 개선", "모공 케어"] },
  { id: "2", date: "2026-04-23", dateFormatted: "2026.04.23", score: 77, change: 3, improvements: ["유분 조절", "색소 개선"] },
  { id: "3", date: "2026-04-16", dateFormatted: "2026.04.16", score: 74, change: -2, improvements: ["수분 관리 필요"] },
  { id: "4", date: "2026-04-09", dateFormatted: "2026.04.09", score: 76, change: 4, improvements: ["피부결 개선", "탄력 증가"] },
  { id: "5", date: "2026-04-02", dateFormatted: "2026.04.02", score: 72, change: 0, improvements: ["전체적 안정"] },
]
