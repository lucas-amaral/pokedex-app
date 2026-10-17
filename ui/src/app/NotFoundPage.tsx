import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 text-center">
      <h1 className="text-lg font-semibold">Page not found</h1>
      <p className="mt-1 text-sm text-slate-500">There is nothing at this address.</p>
      <Link to="/" className="mt-4 inline-block text-sm font-medium text-red-600 hover:underline">
        Back to the catalog
      </Link>
    </div>
  )
}
