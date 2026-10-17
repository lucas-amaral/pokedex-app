interface PaginationProps {
  page: number
  totalPages: number
  hasNext: boolean
  onPageChange: (page: number) => void
}

const buttonClass =
  'rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40'

/** Zero-based page in, "Page 1 of N" out. */
export function Pagination({ page, totalPages, hasNext, onPageChange }: PaginationProps) {
  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-3">
      <button type="button" className={buttonClass} disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        <span aria-hidden="true">←</span>
        <span className="sr-only">Previous page</span>
      </button>
      <span className="text-sm text-slate-600">
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" className={buttonClass} disabled={!hasNext} onClick={() => onPageChange(page + 1)}>
        <span aria-hidden="true">→</span>
        <span className="sr-only">Next page</span>
      </button>
    </nav>
  )
}
