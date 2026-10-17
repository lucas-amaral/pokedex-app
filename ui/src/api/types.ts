/** Mirrors the service's PageResponse. Pages are zero-based. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** RFC 9457 problem details, as returned by the service for every error. */
export interface ProblemDetail {
  status?: number
  title?: string
  detail?: string
  errors?: { parameter?: string; field?: string; message: string }[]
}
