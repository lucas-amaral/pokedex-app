import type { ApiError } from '../api/ApiError'

interface ErrorStateProps {
  error: ApiError
  onRetry: () => void
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm">
      <p className="font-semibold text-red-800">{error.title}</p>
      {error.detail && <p className="mt-1 text-red-700">{error.detail}</p>}
      <button
        type="button"
        onClick={onRetry}
        className="mt-3 rounded-md border border-red-300 bg-white px-3 py-1.5 font-medium text-red-700 hover:bg-red-100"
      >
        Try again
      </button>
    </div>
  )
}
