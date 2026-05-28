export type UserProfile = { username: string; displayName: string }
export type AuthResponse = { accessToken: string; refreshToken: string; expiresIn: number; tokenType: string; user: UserProfile }

const accessKey = "skinai:access-token"
const refreshKey = "skinai:refresh-token"
const userKey = "skinai:user"

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
  localStorage.setItem(accessKey, auth.accessToken)
  localStorage.setItem(refreshKey, auth.refreshToken)
  localStorage.setItem(userKey, JSON.stringify(auth.user))
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
}

export function clearAuth() {
  localStorage.removeItem(accessKey)
  localStorage.removeItem(refreshKey)
  localStorage.removeItem(userKey)
  window.dispatchEvent(new CustomEvent("skinai:auth-updated"))
}

async function readErrorReason(res: Response) {
  const fallback = res.statusText || "request failed"
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
  if (normalized.includes("username must be a valid email")) return "이메일 형식으로 입력해 주세요."
  if (normalized.includes("username already exists")) return "이미 사용 중인 아이디입니다."
  if (normalized.includes("username is required")) return "아이디를 입력해 주세요."
  if (normalized.includes("username must be 3-30")) return "아이디는 영문, 숫자, 점, 밑줄, 하이픈으로 3-30자 입력해 주세요."
  if (normalized.includes("password must be 4-100")) return "비밀번호는 4-100자로 입력해 주세요."
  if (normalized.includes("displayname must be 40")) return "이름은 40자 이하로 입력해 주세요."
  if (normalized.includes("invalid credentials")) return "아이디 또는 비밀번호를 확인해 주세요."
  return fallback
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

export async function register(username: string, password: string, displayName: string) {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, displayName }),
  })
  if (!res.ok) throw new Error(authErrorMessage(await readErrorReason(res), "회원가입 정보를 확인해 주세요."))
  const data = (await res.json()) as AuthResponse
  saveAuth(data)
  return data
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token) headers.set("Authorization", `Bearer ${token}`)
  const res = await fetch(input, { ...init, headers })
  if (res.status === 401) clearAuth()
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
