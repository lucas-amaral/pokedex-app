import { ApiError } from './ApiError'
import type { ProblemDetail } from './types'

type QueryParams = Record<string, string | number | undefined>

export interface JsonResponse<T> {
  status: number
  data: T
}

/** GET a JSON resource from the service. Paths are relative (/api/...); the dev server and nginx proxy them. */
export async function getJson<T>(path: string, params: QueryParams = {}, signal?: AbortSignal): Promise<T> {
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) url.searchParams.set(key, String(value))
  }
  const response = await send(url, { headers: { Accept: 'application/json' }, signal })
  return (await response.json()) as T
}

/** Send a JSON body; the status is returned too, because 200 and 201 can mean different things. */
export async function sendJson<T>(method: 'POST' | 'PUT' | 'PATCH', path: string, body: unknown): Promise<JsonResponse<T>> {
  const response = await send(new URL(path, window.location.origin), {
    method,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return { status: response.status, data: (await response.json()) as T }
}

async function send(url: URL, init: RequestInit): Promise<Response> {
  let response: Response
  try {
    response = await fetch(url, init)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, { title: 'Network error', detail: 'Could not reach the server. Check your connection.' })
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return response
}

async function readProblem(response: Response): Promise<ProblemDetail | undefined> {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return undefined
  }
}
