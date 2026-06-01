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
  pdfUrl?: string
  pdfKey?: string
  analysisId?: string
  analysisSnapshot?: AnalysisResult
  submissionNote?: string
}

export type AnalysisMetric = {
  id: string
  title: string
  score: number
  status: string
  description: string
  category?: string
}

export type AnalysisResult = {
  overallScore?: number
  date?: string
  skinType?: string
  rawAnalysis?: unknown
  aiSummary?: string
  imageDataUrl?: string
  imageKey?: string
  imageUrl?: string
  metrics?: AnalysisMetric[]
  concerns?: string[]
  treatments?: Array<{ name: string; match: string; reason: string; note: string }>
  recommendations?: string[]
}

const applicationStorageKey = "skinai:hospital-applications"
const analysisStorageKey = "skinai:last-analysis"
const lastAnalysisIdKey = "skinai:last-analysis-id"
const analysisImagesStorageKey = "skinai:analysis-images"
const notesStorageKey = "skinai:analysis-notes"

const metricLabels: Record<string, { title: string; status: string; description: string; category: string }> = {
  hydration: { title: "수분", status: "관리 필요", description: "피부 수분 밸런스를 확인하세요", category: "피부 상태" },
  sebum: { title: "유분", status: "보통", description: "유분과 번들거림 상태를 확인하세요", category: "피부 상태" },
  pores: { title: "모공", status: "보통", description: "모공과 피부결 상태를 확인하세요", category: "피부 상태" },
  pigment: { title: "색소", status: "관리 필요", description: "잡티와 기미 가능성을 확인하세요", category: "색소" },
  wrinkle: { title: "주름", status: "관리 필요", description: "주름과 탄력 상태를 확인하세요", category: "주름" },
  texture: { title: "피부결", status: "보통", description: "광채와 피부결 균일도를 확인하세요", category: "균일도" },
  age: { title: "피부 나이", status: "참고", description: "AI가 추정한 피부 나이입니다", category: "종합" },
}

const concernLabels: Record<string, string> = {
  hydration: "수분 부족",
  sebum: "유분 밸런스",
  pores: "모공/피부결",
  pigment: "색소/잡티",
  texture: "피부결",
  wrinkle: "주름/탄력",
  age: "피부 나이",
}

const treatmentLabels: Record<string, { name: string; reason: string; note: string }> = {
  "Rejuran Healer": {
    name: "리쥬란 힐러",
    reason: "피부 장벽과 탄력 개선에 도움",
    note: "피부 상태에 따라 시술 간격과 강도를 조절하세요.",
  },
  "Pico toning": {
    name: "피코토닝",
    reason: "색소와 잡티 고민 완화에 도움",
    note: "자외선 차단과 보습 관리를 함께 진행하세요.",
  },
  Aquapeel: {
    name: "아쿠아필",
    reason: "피지와 각질 정리에 도움",
    note: "민감한 피부라면 상담 후 진행하는 것이 좋습니다.",
  },
}

function treatmentMeta(name: string) {
  const compact = name.replace(/\s+/g, "").toLowerCase()
  if (compact.includes("볼꺼짐") || compact.includes("필러")) {
    return {
      name,
      reason: "볼 부위 볼륨과 피부결 개선에 도움",
      note: "필러와 스킨부스터 조합 여부는 대면 상담에서 볼륨 정도를 보고 결정하세요.",
    }
  }
  if (compact.includes("보톡스") || compact.includes("botox")) {
    return {
      name,
      reason: "표정 주름 완화에 도움",
      note: "이마와 미간의 표정 사용 습관, 눈썹 처짐 가능성을 함께 확인하세요.",
    }
  }
  if (compact.includes("피코") || compact.includes("토닝")) {
    return treatmentLabels["Pico toning"]
  }
  if (compact.includes("리쥬란")) {
    return treatmentLabels["Rejuran Healer"]
  }
  if (compact.includes("아쿠아")) {
    return treatmentLabels.Aquapeel
  }
  return {
    name,
    reason: "AI 종합 분석 요약 기반 추천",
    note: "전문가 상담 후 진행 여부를 결정하세요.",
  }
}

function treatmentsFromAiSummary(summary?: string) {
  if (!summary?.trim()) return []
  const lines = summary.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const treatments: Array<{ name: string; match: string; reason: string; note: string }> = []
  let current: { name: string; match: string; reason: string; note: string } | null = null

  for (const rawLine of lines) {
    const line = rawLine
      .replace(/^[-*]\s+/, "")
      .replace(/\*\*(.+?)\*\*/g, "$1")
      .trim()
    const treatmentMatch = line.match(/^[-*]?\s*권장\s*시술\s*:\s*(.+)$/)
    if (treatmentMatch) {
      if (current) treatments.push(current)
      const rawName = treatmentMatch[1]
        .replace(/\s*\(code\s+[^)]+\)/i, "")
        .replace(/\s*\([A-Z]+_\d+\)\s*$/i, "")
        .trim()
      const meta = treatmentMeta(rawName)
      current = { name: meta.name, match: "추천", reason: meta.reason, note: meta.note }
      continue
    }

    const featureMatch = line.match(/^[-*]?\s*시술\s*특징\s*:\s*(.+)$/)
    if (featureMatch && current) {
      current.reason = featureMatch[1].trim()
    }
  }

  if (current) treatments.push(current)
  return treatments
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {}
}

function asNumber(value: unknown, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? Math.max(0, Math.min(100, Math.round(n))) : fallback
}

function asStringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : []
}

function averageScore(values: unknown[]) {
  const numbers = values.map((value) => Number(value)).filter(Number.isFinite)
  return numbers.length ? asNumber(numbers.reduce((sum, value) => sum + value, 0) / numbers.length) : 0
}

function statusForScore(score: number) {
  if (score >= 80) return "양호"
  if (score >= 60) return "보통"
  if (score >= 40) return "관리 필요"
  return "집중 관리"
}

function normalizeSkinType(value: unknown) {
  const text = String(value || "").toLowerCase()
  if (text.includes("combination")) return "복합성"
  if (text.includes("dry")) return "건성"
  if (text.includes("oily")) return "지성"
  if (text.includes("sensitive")) return "민감성"
  return String(value || "피부 타입 분석")
}

export function normalizeAnalysisResponse(payload: unknown): AnalysisResult {
  const root = asRecord(payload)
  const result = asRecord(root.result)
  const source = Object.keys(result).length ? result : root
  const scores = asRecord(source.scores)
  const rawMetrics = Array.isArray(source.metrics) ? source.metrics : []
  const pigment = asRecord(source.pigment)
  const wrinkle = asRecord(source.wrinkle)
  const homogenity = asRecord(source.homogenity)
  const cheekSagging = asRecord(source.cheek_sagging)
  const chinSagging = asRecord(source.chin_sagging)
  const aiSummary = typeof source.aiSummary === "string" ? source.aiSummary : typeof root.aiSummary === "string" ? root.aiSummary : undefined

  // Build detailed per-region metrics from pipeline output
  const pipelineMetrics: AnalysisMetric[] = []

  if (typeof source.age === "number") {
    const s = asNumber(source.age)
    pipelineMetrics.push({ id: "age", title: "피부 나이", score: s, status: "참고", description: "AI가 추정한 피부 나이 (성별 입력 시 정확도 향상)", category: "종합" })
  }
  if (pigment.left !== undefined) { const s = asNumber(pigment.left); pipelineMetrics.push({ id: "pigment_left", title: "색소 (좌)", score: s, status: statusForScore(s), description: "좌측 볼 색소 균일도", category: "색소" }) }
  if (pigment.right !== undefined) { const s = asNumber(pigment.right); pipelineMetrics.push({ id: "pigment_right", title: "색소 (우)", score: s, status: statusForScore(s), description: "우측 볼 색소 균일도", category: "색소" }) }

  const wrinkleFields: Array<{ key: string; title: string }> = [
    { key: "forehead", title: "이마/미간 주름" },
    { key: "right_eye", title: "눈가 주름 (우)" },
    { key: "left_eye", title: "눈가 주름 (좌)" },
    { key: "nasolabial", title: "팔자 주름" },
    { key: "perioral", title: "입가 주름" },
    { key: "right_vol", title: "볼 주름 (우)" },
    { key: "left_vol", title: "볼 주름 (좌)" },
  ]
  for (const { key, title } of wrinkleFields) {
    if (wrinkle[key] !== undefined) {
      const s = asNumber(wrinkle[key])
      pipelineMetrics.push({ id: `wrinkle_${key}`, title, score: s, status: statusForScore(s), description: `${title} 상태`, category: "주름" })
    }
  }

  if (homogenity.radiance !== undefined) { const s = asNumber(homogenity.radiance); pipelineMetrics.push({ id: "homogenity_radiance", title: "광채", score: s, status: statusForScore(s), description: "피부 광채 및 투명도", category: "균일도" }) }
  if (homogenity.texture !== undefined) { const s = asNumber(homogenity.texture); pipelineMetrics.push({ id: "homogenity_texture", title: "피부결", score: s, status: statusForScore(s), description: "피부결 매끄러움", category: "균일도" }) }
  if (cheekSagging.total !== undefined) { const s = asNumber(cheekSagging.total); pipelineMetrics.push({ id: "cheek_sagging", title: "볼 처짐", score: s, status: statusForScore(s), description: "볼 처짐 정도", category: "처짐" }) }
  if (chinSagging.total !== undefined) { const s = asNumber(chinSagging.total); pipelineMetrics.push({ id: "chin_sagging", title: "턱 처짐", score: s, status: statusForScore(s), description: "턱 처짐 정도", category: "처짐" }) }

  const aiModelScores: Record<string, number> = {}
  if (typeof source.age === "number") aiModelScores.age = asNumber(source.age)
  if (Object.keys(pigment).length) aiModelScores.pigment = averageScore(Object.values(pigment))
  if (Object.keys(wrinkle).length) aiModelScores.wrinkle = averageScore(Object.values(wrinkle))
  if (Object.keys(homogenity).length) aiModelScores.texture = averageScore(Object.values(homogenity))

  const metrics: AnalysisMetric[] = pipelineMetrics.length
    ? pipelineMetrics
    : rawMetrics.length
    ? rawMetrics.map((metric, index) => {
        const item = asRecord(metric)
        const id = String(item.id || item.key || `metric-${index}`)
        const score = asNumber(item.score || item.value)
        const meta = metricLabels[id] || {
          title: String(item.title || id),
          status: statusForScore(score),
          description: String(item.description || "AI 분석 항목"),
        }
        return {
          id,
          title: String(item.title || meta.title),
          score,
          status: String(item.status || meta.status || statusForScore(score)),
          description: String(item.description || meta.description),
          category: String(item.category || meta.category || "기타"),
        }
      })
    : Object.entries(Object.keys(scores).length ? scores : aiModelScores).map(([id, value]) => {
        const score = asNumber(value)
        const meta = metricLabels[id] || { title: id, status: statusForScore(score), description: "AI 분석 항목", category: "기타" }
        return { id, title: meta.title, score, status: statusForScore(score), description: meta.description, category: meta.category }
      })

  const concerns =
    asStringArray(source.concerns).length
      ? asStringArray(source.concerns)
      : asStringArray(source.topConcerns).length
        ? asStringArray(source.topConcerns).map((concern) => concernLabels[concern] || concern)
        : metrics
            .filter((metric) => metric.id !== "age")
            .sort((a, b) => a.score - b.score)
            .slice(0, 3)
            .map((metric) => metric.title)

  const sourceTreatments = Array.isArray(source.treatments) ? source.treatments : []
  const directTreatments = sourceTreatments.map((treatment) => {
    if (typeof treatment === "string") {
      const meta = treatmentLabels[treatment] || treatmentMeta(treatment)
      return { name: meta.name, match: "추천", reason: meta.reason, note: meta.note }
    }

        const item = asRecord(treatment)
        return {
          name: String(item.name || "추천 시술"),
          match: String(item.match || "추천"),
          reason: String(item.reason || "AI 분석 결과 기반 추천"),
          note: String(item.note || "전문가 상담 후 진행 여부를 결정하세요."),
        }
      })
  const fallbackTreatmentNames = asStringArray(source.recommendedTreatments)
  const fallbackTreatments = fallbackTreatmentNames.map((name) => {
        const meta = treatmentLabels[name] || treatmentMeta(name)
        return { name: meta.name, match: "추천", reason: meta.reason, note: meta.note }
      })
  const summaryTreatments = treatmentsFromAiSummary(aiSummary)
  const treatments = summaryTreatments.length ? summaryTreatments : directTreatments.length ? directTreatments : fallbackTreatments

  const recommendations = asStringArray(source.managementTips || source.careTips || source.recommendations).length
    ? asStringArray(source.managementTips || source.careTips || source.recommendations).map((item) => treatmentLabels[item]?.note || item)
    : [
        "자외선 차단제를 꾸준히 사용하고 색소 변화를 관찰하세요.",
        "수분과 장벽 관리로 피부 컨디션을 먼저 안정화하세요.",
        "시술 강도와 주기는 전문가 상담 후 단계적으로 결정하세요.",
      ]

  return {
    overallScore: asNumber(
      source.overallScore || root.overallScore,
      metrics.length
        ? Math.round(metrics.filter((metric) => metric.id !== "age").reduce((sum, metric) => sum + metric.score, 0) / Math.max(1, metrics.filter((metric) => metric.id !== "age").length))
        : 0,
    ),
    date: String(source.date || root.date || new Intl.DateTimeFormat("ko-KR").format(new Date())),
    skinType: normalizeSkinType(source.skinType),
    rawAnalysis: source,
    aiSummary,
    imageDataUrl: typeof source.imageDataUrl === "string" ? source.imageDataUrl : typeof root.imageDataUrl === "string" ? root.imageDataUrl : undefined,
    imageKey: typeof source.imageKey === "string" ? source.imageKey : typeof root.imageKey === "string" ? root.imageKey : undefined,
    imageUrl: typeof source.imageUrl === "string" ? source.imageUrl : typeof root.imageUrl === "string" ? root.imageUrl : undefined,
    metrics,
    concerns,
    treatments,
    recommendations,
  }
}

export function getHospitalApplications(): HospitalApplication[] {
  try {
    return JSON.parse(localStorage.getItem(applicationStorageKey) || "[]")
  } catch {
    return []
  }
}

export function saveHospitalApplication(application: HospitalApplication) {
  localStorage.setItem(applicationStorageKey, JSON.stringify([application, ...getHospitalApplications()]))
  window.dispatchEvent(new CustomEvent("skinai:hospital-application-updated"))
}

export function formatApplicationDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "방금 전"
  return new Intl.DateTimeFormat("ko-KR", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date)
}

export function saveLastAnalysisId(id: string) {
  localStorage.setItem(lastAnalysisIdKey, id)
}

export function getLastAnalysisId(): string | null {
  return localStorage.getItem(lastAnalysisIdKey)
}

export function saveAnalysisImage(analysisId: string, imageDataUrl: string) {
  try {
    const all = JSON.parse(localStorage.getItem(analysisImagesStorageKey) || "{}")
    all[analysisId] = imageDataUrl
    localStorage.setItem(analysisImagesStorageKey, JSON.stringify(all))
  } catch {}
}

export function getAnalysisImage(analysisId: string): string | null {
  try {
    const all = JSON.parse(localStorage.getItem(analysisImagesStorageKey) || "{}")
    return typeof all[analysisId] === "string" ? all[analysisId] : null
  } catch {
    return null
  }
}

export function getAnalysisNotes(analysisId: string): string {
  try {
    const all = JSON.parse(localStorage.getItem(notesStorageKey) || "{}")
    return typeof all[analysisId] === "string" ? all[analysisId] : ""
  } catch {
    return ""
  }
}

export function saveAnalysisNotes(analysisId: string, notes: string) {
  try {
    const all = JSON.parse(localStorage.getItem(notesStorageKey) || "{}")
    all[analysisId] = notes
    localStorage.setItem(notesStorageKey, JSON.stringify(all))
  } catch {}
}

export function saveLastAnalysis(result: unknown, imageDataUrl?: string) {
  const normalized = normalizeAnalysisResponse(result)
  if (imageDataUrl) normalized.imageDataUrl = imageDataUrl
  localStorage.setItem(analysisStorageKey, JSON.stringify(normalized))
  window.dispatchEvent(new CustomEvent("skinai:analysis-updated"))
}

export function getLastAnalysis(): AnalysisResult | null {
  try {
    const value = localStorage.getItem(analysisStorageKey)
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

export function clearLastAnalysis() {
  localStorage.removeItem(analysisStorageKey)
  localStorage.removeItem(lastAnalysisIdKey)
  window.dispatchEvent(new CustomEvent("skinai:analysis-updated"))
}

export function getHistoryData(): AnalysisRecord[] {
  const result = getLastAnalysis()
  return result?.overallScore
    ? [
        {
          id: "latest",
          date: new Date().toISOString(),
          dateFormatted: new Intl.DateTimeFormat("ko-KR").format(new Date()),
          score: result.overallScore,
          change: 0,
          improvements: result.concerns || [],
        },
      ]
    : []
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
