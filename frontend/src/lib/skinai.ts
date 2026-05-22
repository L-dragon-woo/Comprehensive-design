export interface ChatMessage { id: string; role: "user" | "assistant"; content: string; timestamp: Date }
export interface AnalysisRecord { id: string; date: string; dateFormatted: string; score: number; change: number; improvements: string[] }
export interface HospitalApplication { id: string; hospitalName: string; submittedAt: string; status: "submitted" | "reviewing" | "confirmed"; includedItems: string[] }
export type AnalysisResult = {
  overallScore?: number
  date?: string
  skinType?: string
  metrics?: Array<{ id: string; title: string; score: number; status: string; description: string }>
  concerns?: string[]
  treatments?: Array<{ name: string; match: string; reason: string; note: string }>
  recommendations?: string[]
}
const applicationStorageKey = "skinai:hospital-applications"
const analysisStorageKey = "skinai:last-analysis"

const metricLabels: Record<string, { title: string; status: string; description: string }> = {
  hydration: { title: "??", status: "?? ??", description: "?? ?? ??? ?? ??" },
  sebum: { title: "??", status: "??", description: "?? ???? ???? ??" },
  pores: { title: "??", status: "??", description: "??? ??? ??" },
  pigment: { title: "??", status: "?? ??", description: "??? ?? ?? ??" },
  wrinkle: { title: "??", status: "?? ??", description: "???? ?? ?? ??" },
  age: { title: "?? ??", status: "??", description: "AI? ??? ?? ???" },
}
const concernLabels: Record<string, string> = {
  hydration: "?? ??",
  sebum: "?? ???",
  pores: "??/???",
  pigment: "??/??",
  texture: "???",
  wrinkle: "??/??",
  age: "?? ???",
}
const treatmentLabels: Record<string, { name: string; reason: string; note: string }> = {
  "Rejuran Healer": { name: "??? ??", reason: "?? ??? ?? ?? ??? ??", note: "???? ?? ??? ???? ?? ?????." },
  "Pico toning": { name: "????", reason: "??? ?? ?? ?? ??? ??", note: "??? ??? ?? ?? ??? ?????." },
  Aquapeel: { name: "????", reason: "??? ??? ?? ??? ??", note: "????? ??? ??? ??? ?? ?????." },
}
function asRecord(value: unknown): Record<string, unknown> { return value && typeof value === "object" ? value as Record<string, unknown> : {} }
function asNumber(value: unknown, fallback = 0) { const n = Number(value); return Number.isFinite(n) ? Math.max(0, Math.min(100, Math.round(n))) : fallback }
function asStringArray(value: unknown): string[] { return Array.isArray(value) ? value.map(String).filter(Boolean) : [] }
function statusForScore(score: number) { if (score >= 80) return "??"; if (score >= 60) return "??"; if (score >= 40) return "?? ??"; return "?? ??" }
function normalizeSkinType(value: unknown) {
  const text = String(value || "").toLowerCase()
  if (text.includes("combination")) return "???"
  if (text.includes("dry")) return "??"
  if (text.includes("oily")) return "??"
  if (text.includes("sensitive")) return "???"
  return String(value || "?? ?? ??")
}
export function normalizeAnalysisResponse(payload: unknown): AnalysisResult {
  const root = asRecord(payload)
  const source = asRecord(root.result) && Object.keys(asRecord(root.result)).length ? asRecord(root.result) : root
  const scores = asRecord(source.scores)
  const rawMetrics = Array.isArray(source.metrics) ? source.metrics : []
  const metrics = rawMetrics.length
    ? rawMetrics.map((metric, index) => {
        const item = asRecord(metric)
        const id = String(item.id || item.key || `metric-${index}`)
        const score = asNumber(item.score || item.value)
        const meta = metricLabels[id] || { title: String(item.title || id), status: statusForScore(score), description: String(item.description || "AI ?? ??") }
        return { id, title: String(item.title || meta.title), score, status: String(item.status || meta.status || statusForScore(score)), description: String(item.description || meta.description) }
      })
    : Object.entries(scores).map(([id, value]) => {
        const score = asNumber(value)
        const meta = metricLabels[id] || { title: id, status: statusForScore(score), description: "AI ?? ??" }
        return { id, title: meta.title, score, status: statusForScore(score), description: meta.description }
      })
  const concerns = asStringArray(source.concerns).length
    ? asStringArray(source.concerns)
    : asStringArray(source.topConcerns).map((concern) => concernLabels[concern] || concern)
  const treatments = Array.isArray(source.treatments)
    ? source.treatments.map((treatment) => {
        const item = asRecord(treatment)
        return { name: String(item.name || "?? ??"), match: String(item.match || "??"), reason: String(item.reason || "AI ?? ?? ?? ??"), note: String(item.note || "??? ?? ? ?? ??? ?????.") }
      })
    : asStringArray(source.recommendedTreatments || source.treatments || source.recommendations).map((name) => {
        const meta = treatmentLabels[name] || { name, reason: "AI ?? ?? ?? ??", note: "??? ?? ? ?? ??? ?????." }
        return { name: meta.name, match: "??", reason: meta.reason, note: meta.note }
      })
  return {
    overallScore: asNumber(source.overallScore || root.overallScore, metrics.length ? Math.round(metrics.reduce((sum, metric) => sum + metric.score, 0) / metrics.length) : 0),
    date: String(source.date || root.date || new Intl.DateTimeFormat("ko-KR").format(new Date())),
    skinType: normalizeSkinType(source.skinType),
    metrics,
    concerns,
    treatments,
    recommendations: asStringArray(source.managementTips || source.careTips || source.recommendations).map((item) => treatmentLabels[item]?.note || item),
  }
}
export function getHospitalApplications(): HospitalApplication[] { try { return JSON.parse(localStorage.getItem(applicationStorageKey) || "[]") } catch { return [] } }
export function saveHospitalApplication(application: HospitalApplication) { localStorage.setItem(applicationStorageKey, JSON.stringify([application, ...getHospitalApplications()])); window.dispatchEvent(new CustomEvent("skinai:hospital-application-updated")) }
export function formatApplicationDate(value: string) { const date = new Date(value); if (Number.isNaN(date.getTime())) return "방금 전"; return new Intl.DateTimeFormat("ko-KR", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date) }
export function saveLastAnalysis(result: AnalysisResult) { localStorage.setItem(analysisStorageKey, JSON.stringify(normalizeAnalysisResponse(result))); window.dispatchEvent(new CustomEvent("skinai:analysis-updated")) }
export function getLastAnalysis(): AnalysisResult | null { try { const v = localStorage.getItem(analysisStorageKey); return v ? JSON.parse(v) : null } catch { return null } }
export function clearLastAnalysis() { localStorage.removeItem(analysisStorageKey); window.dispatchEvent(new CustomEvent("skinai:analysis-updated")) }
export function getHistoryData(): AnalysisRecord[] { const result = getLastAnalysis(); return result?.overallScore ? [{ id: "latest", date: new Date().toISOString(), dateFormatted: new Intl.DateTimeFormat("ko-KR").format(new Date()), score: result.overallScore, change: 0, improvements: result.concerns || [] }] : [] }
export function scoreColor(score: number) { if (score >= 80) return "text-success"; if (score >= 60) return "text-primary"; if (score >= 40) return "text-warning"; return "text-destructive" }
export function scoreBgColor(score: number) { if (score >= 80) return "bg-success/10"; if (score >= 60) return "bg-primary/10"; if (score >= 40) return "bg-warning/10"; return "bg-destructive/10" }
