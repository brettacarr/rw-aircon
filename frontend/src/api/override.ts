import apiClient from './client'
import type { Override, OverrideCreateRequest } from '@/types'

/**
 * Get the current active override (if any)
 * Returns null if no active override exists
 */
export async function getOverride(): Promise<Override | null> {
  try {
    const response = await apiClient.get<Override>('/override')
    // 204 No Content means no active override
    if (response.status === 204) {
      return null
    }
    return response.data
  } catch {
    // Handle 204 response which axios may treat as an error
    return null
  }
}

/**
 * Create a new override
 * Duration options: "1h", "2h", "4h", "until_next"
 */
export async function createOverride(request: OverrideCreateRequest): Promise<Override> {
  const response = await apiClient.post<Override>('/override', request)
  return response.data
}

/**
 * Cancel the current active override
 */
export async function cancelOverride(): Promise<void> {
  await apiClient.delete('/override')
}
