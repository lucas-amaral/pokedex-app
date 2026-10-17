import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/ApiError'

const MAX_RETRIES = 2

/** Client errors (4xx) will fail again, so only server and network failures are retried. */
function shouldRetry(failureCount: number, error: Error) {
  const isClientError = error instanceof ApiError && error.status >= 400 && error.status < 500
  return !isClientError && failureCount < MAX_RETRIES
}

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: shouldRetry,
        refetchOnWindowFocus: false,
      },
    },
  })
}
