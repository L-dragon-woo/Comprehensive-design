export interface UserLocation {
  latitude: number
  longitude: number
}

export interface Hospital {
  id: string
  name: string
  distance: string
  distanceMeters: number
  rating: number
  address: string
  latitude: number
  longitude: number
  specialties: string[]
  matchedTreatments: string[]
  waitTime: string
  phone: string
}

interface HospitalListResponse {
  items: Hospital[]
  total: number
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ""

export function getCurrentLocation(): Promise<UserLocation> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("현재 브라우저에서 위치 정보를 사용할 수 없어요."))
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        })
      },
      () => reject(new Error("위치 권한을 허용하면 현재 위치 기준 병원을 볼 수 있어요.")),
      {
        enableHighAccuracy: true,
        timeout: 10_000,
        maximumAge: 300_000,
      },
    )
  })
}

export async function fetchHospitals(params: { query?: string; location?: UserLocation }): Promise<Hospital[]> {
  const searchParams = new URLSearchParams()
  const query = params.query?.trim()

  if (query) {
    searchParams.set("query", query)
  }

  if (params.location) {
    searchParams.set("lat", String(params.location.latitude))
    searchParams.set("lng", String(params.location.longitude))
    searchParams.set("sort", "distance")
  }

  const queryString = searchParams.toString()
  const response = await fetch(`${apiBaseUrl}/api/hospitals${queryString ? `?${queryString}` : ""}`)

  if (!response.ok) {
    throw new Error("병원 목록을 불러오지 못했어요.")
  }

  const data = (await response.json()) as HospitalListResponse
  return data.items
}
