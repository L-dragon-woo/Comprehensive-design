export type HospitalSearchResult = {
  id: string
  name: string
  roadAddress: string
  address: string
  phone: string
  distance: string
  x: string
  y: string
  placeUrl: string
}

type CachedHospitalSearch = {
  hospitals: HospitalSearchResult[]
  coords: { x: string; y: string } | null
  query: string
  savedAt: number
}

const cacheKey = "skinai:hospital-search-cache"
const defaultHospitalQuery = "피부과"
const cacheTtlMs = 10 * 60 * 1000

export function getCachedHospitalSearch(): CachedHospitalSearch | null {
  try {
    const cached = JSON.parse(localStorage.getItem(cacheKey) || "null") as CachedHospitalSearch | null
    if (!cached?.hospitals?.length) return null
    if (Date.now() - cached.savedAt > cacheTtlMs) return null
    return cached
  } catch {
    return null
  }
}

function saveHospitalSearchCache(cache: CachedHospitalSearch) {
  localStorage.setItem(cacheKey, JSON.stringify(cache))
}

export async function getBrowserCoords(timeout = 7000): Promise<{ x: string; y: string } | null> {
  if (!navigator.geolocation) return null
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => resolve({ x: String(position.coords.longitude), y: String(position.coords.latitude) }),
      () => resolve(null),
      { enableHighAccuracy: false, maximumAge: 5 * 60 * 1000, timeout },
    )
  })
}

export async function searchHospitals(options: { query?: string; coords?: { x: string; y: string } | null } = {}) {
  const query = options.query?.trim() || defaultHospitalQuery
  const params = new URLSearchParams({ query })
  if (options.coords) {
    params.set("x", options.coords.x)
    params.set("y", options.coords.y)
    params.set("radius", "5000")
  }

  const res = await fetch(`/api/hospitals/search?${params}`)
  if (!res.ok) throw new Error(res.status === 503 ? "카카오 REST API 키가 설정되지 않았습니다." : `병원 검색에 실패했습니다. (${res.status})`)
  const hospitals = (await res.json()) as HospitalSearchResult[]
  saveHospitalSearchCache({ hospitals, coords: options.coords || null, query, savedAt: Date.now() })
  return hospitals
}

export async function prefetchNearbyHospitals() {
  try {
    const coords = await getBrowserCoords()
    return await searchHospitals({ coords })
  } catch {
    return []
  }
}
