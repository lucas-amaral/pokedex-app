import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useRemoveCollectedPokemon } from './collectionQueries'

/** "Remove from collection", with an inline confirmation; afterwards the collection page says what was removed. */
export function RemoveFromCollection({ id, title }: { id: string; title: string }) {
  const [isConfirming, setConfirming] = useState(false)
  const remove = useRemoveCollectedPokemon(id)
  const navigate = useNavigate()

  function confirm() {
    remove.mutate(undefined, {
      onSuccess: () => void navigate('/collection', { state: { removedName: title } }),
    })
  }

  if (!isConfirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="w-full rounded-md px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
      >
        Remove from collection
      </button>
    )
  }

  return (
    <div role="alertdialog" aria-labelledby="remove-question" className="space-y-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm">
      <p id="remove-question" className="font-medium text-red-800">
        Remove {title} from the collection?
      </p>
      <p className="text-red-700">Its nickname, notes, region, names and tags will be lost.</p>
      {remove.isError && <p className="text-red-700">{remove.error.title}</p>}
      <div className="flex justify-end gap-2">
        <button type="button" onClick={() => setConfirming(false)} className="rounded-md px-3 py-1.5 text-slate-600 hover:bg-white">
          Cancel
        </button>
        <button
          type="button"
          onClick={confirm}
          disabled={remove.isPending}
          className="rounded-md bg-red-600 px-3 py-1.5 font-medium text-white hover:bg-red-700 disabled:opacity-60"
        >
          {remove.isPending ? 'Removing…' : 'Remove'}
        </button>
      </div>
    </div>
  )
}
