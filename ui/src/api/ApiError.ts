import type { ProblemDetail } from './types'

/** Any failed call to the service, carrying the problem details the service sent (if any). */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail?: string
  readonly errors: NonNullable<ProblemDetail['errors']>

  constructor(status: number, problem: ProblemDetail = {}) {
    const title = problem.title ?? 'Request failed'
    super(problem.detail ?? title)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.detail = problem.detail
    this.errors = problem.errors ?? []
  }
}
