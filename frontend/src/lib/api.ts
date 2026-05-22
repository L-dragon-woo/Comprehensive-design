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

export async function login(username: string, password: string) {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) throw new Error("아이디 또는 비밀번호를 확인해 주세요.")
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
