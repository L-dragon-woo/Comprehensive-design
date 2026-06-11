import { clearTransientUserLocalData } from "@/lib/skinai"

export type UserProfile = {
  username: string
  displayName: string
  gender?: string | null
  age?: number | null
  skinTreatmentHistory?: string | null
  hasAllergy: boolean
  allergyDetails?: string | null
  hasDisease: boolean
  diseaseDetails?: string | null
}
export type AuthResponse = { accessToken: string; refreshToken: string; expiresIn: number; tokenType: string; user: UserProfile }
export type ProfilePayload = Omit<UserProfile, "username">
export type SavedAnalysis = { analysisId: string; createdAt: string; analysis: unknown }
export type StoredFile = { key?: string | null; url?: string | null; uploadedAt?: string | null; storageStatus?: "stored" | "failed" }
export type PresignedUpload = { key: string; url: string; method: "PUT"; contentType: string; expiresAt: string }

const accessKey = "skinai:access-token"
const refreshKey = "skinai:refresh-token"
const userKey = "skinai:user"
const localDataOwnerKey = "skinai:local-data-owner"

export function getAccessToken() {
  return localStorage.getItem(accessKey)
}

export function getRefreshToken() {
  return localStorage.getItem(refreshKey)
}

export function getCurrentUser(): UserProfile | null {
  const value = localStorage.getItem(userKey)
  return value ? (JSON.parse(value) as UserProfile) : null
}

export function isAuthenticated() {
  return Boolean(getAccessToken())
}

export function saveAuth(auth: AuthResponse) {
  const localDataOwner = localStorage.getItem(localDataOwnerKey)
  if (localDataOwner !== auth.user.username) clearTransientUserLocalData()
  localStorage.setItem(accessKey, auth.accessToken)
  localStorage.setItem(refreshKey, auth.refreshToken)
  localStorage.setItem(userKey, JSON.stringify(auth.user))
  localStorage.setItem(localDataOwnerKey, auth.user.username)
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
}

export function clearAuth() {
  clearTransientUserLocalData()
  localStorage.removeItem(accessKey)
  localStorage.removeItem(refreshKey)
  localStorage.removeItem(userKey)
  localStorage.removeItem(localDataOwnerKey)
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
}

export async function readErrorReason(res: Response) {
  const fallback = `${res.status} ${res.statusText || "request failed"}`
  const contentType = res.headers.get("content-type") || ""

  try {
    if (contentType.includes("application/json") || contentType.includes("application/problem+json")) {
      const data = (await res.json()) as Record<string, unknown>
      const value = data.detail || data.message || data.error || data.title
      return typeof value === "string" && value.trim() ? value : fallback
    }

    const text = await res.text()
    return text.trim() || fallback
  } catch {
    return fallback
  }
}

function authErrorMessage(reason: string, fallback: string) {
  const normalized = reason.toLowerCase()
  if (normalized.includes("database is unavailable")) return "데이터베이스에 연결할 수 없어 회원가입을 완료하지 못했습니다."
  if (normalized.includes("503") || normalized.includes("service unavailable")) return "서버가 준비되지 않아 회원가입을 완료하지 못했습니다."
  if (normalized.includes("500") || normalized.includes("internal server error")) return "서버 오류로 회원가입을 완료하지 못했습니다. 백엔드와 DB 상태를 확인해 주세요."
  if (normalized.includes("username must be a valid email")) return "이메일 형식으로 입력해 주세요."
  if (normalized.includes("username already exists")) return "이미 사용 중인 이메일입니다."
  if (normalized.includes("username is required")) return "이메일을 입력해 주세요."
  if (normalized.includes("username must be 3-30")) return "이메일은 올바른 형식으로 입력해 주세요."
  if (normalized.includes("password must be 4-100")) return "비밀번호는 4-100자로 입력해 주세요."
  if (normalized.includes("displayname must be 40")) return "이름은 40자 이하로 입력해 주세요."
  if (normalized.includes("invalid credentials")) return "아이디 또는 비밀번호를 확인해 주세요."
  return reason.trim() || fallback
}

export async function login(username: string, password: string) {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) throw new Error(authErrorMessage(await readErrorReason(res), "아이디 또는 비밀번호를 확인해 주세요."))
  const data = (await res.json()) as AuthResponse
  saveAuth(data)
  return data
}

export async function register(username: string, password: string, profile: ProfilePayload) {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, ...profile }),
  })
  if (!res.ok) throw new Error(authErrorMessage(await readErrorReason(res), "회원가입 정보를 확인해 주세요."))
  const data = (await res.json()) as AuthResponse
  saveAuth(data)
  return data
}

export async function getMyProfile() {
  const res = await apiFetch("/api/auth/me")
  if (!res.ok) throw new Error(await readErrorReason(res))
  const data = (await res.json()) as UserProfile
  localStorage.setItem(userKey, JSON.stringify(data))
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
  return data
}

export async function updateMyProfile(profile: ProfilePayload) {
  const res = await apiFetch("/api/auth/me", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(profile),
  })
  if (!res.ok) throw new Error(await readErrorReason(res))
  const data = (await res.json()) as UserProfile
  localStorage.setItem(userKey, JSON.stringify(data))
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
  return data
}

export async function getMyAnalyses() {
  const res = await apiFetch("/api/analyses")
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as SavedAnalysis[]
}

export async function getMyAnalysis(id: string) {
  const res = await apiFetch(`/api/analyses/${encodeURIComponent(id)}`)
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as SavedAnalysis
}

export async function uploadReportFile(file: Blob, filename: string, analysisId?: string | null) {
  const formData = new FormData()
  formData.append("file", file, filename)
  if (analysisId) formData.append("analysisId", analysisId)

  const res = await apiFetch("/api/files/reports", { method: "POST", body: formData })
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as StoredFile
}

export async function getStoredFileUrl(key: string) {
  const res = await apiFetch(`/api/files/url?key=${encodeURIComponent(key)}`)
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as StoredFile
}

export async function createImagePresignedUpload(file: Blob, filename: string) {
  const res = await apiFetch("/api/files/images/presigned-upload", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      filename,
      contentType: file.type || "image/jpeg",
      contentLength: file.size,
    }),
  })
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as PresignedUpload
}

export async function uploadToPresignedUrl(upload: PresignedUpload, file: Blob) {
  const res = await fetch(upload.url, {
    method: upload.method,
    headers: { "Content-Type": upload.contentType },
    body: file,
  })
  if (!res.ok) throw new Error(`S3 upload failed: ${res.status} ${res.statusText}`)
}

export async function analyzePresignedImage(imageKey: string, filename: string, gender = "female") {
  const res = await apiFetch("/api/analyses/from-s3", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ imageKey, filename, gender }),
  })
  if (!res.ok) throw new Error(await readErrorReason(res))
  return (await res.json()) as Record<string, unknown>
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token) headers.set("Authorization", `Bearer ${token}`)
  const res = await fetch(input, { ...init, headers })
  if (res.status === 401 || res.status === 403) clearAuth()
  return res
}

export async function logout() {
  const token = getRefreshToken()
  if (token) {
    await apiFetch("/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: token }),
    }).catch(() => undefined)
  }
  clearAuth()
}
