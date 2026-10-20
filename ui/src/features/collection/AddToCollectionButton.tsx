import { Link, useLocation } from 'react-router'
import { useAuth } from '../auth/authContext'
import { useCollectedCopy, useSyncPokemon } from './collectionQueries'

interface AddToCollectionButtonProps {
  idOrName: string
  pokeapiId: number
}

/**
 * Offers to copy a catalog Pokémon into the collection, but only when it is not there yet;
 * otherwise links to the collected copy.
 */
export function AddToCollectionButton({ idOrName, pokeapiId }: AddToCollectionButtonProps) {
  const collected = useCollectedCopy(pokeapiId)
  const sync = useSyncPokemon()
  const { user } = useAuth()
  const location = useLocation()

  if (sync.isSuccess) {
    return (
      <p role="status" className="text-sm text-green-700">
        Added to the collection. <OpenLink id={sync.data.pokemon.id} />
      </p>
    )
  }

  if (collected.isPending) {
    return <div aria-hidden="true" className="h-9 animate-pulse rounded-md bg-slate-100" />
  }

  if (collected.data) {
    return (
      <p className="text-sm text-slate-600">
        <span className="font-medium text-slate-900">In the collection</span> · <OpenLink id={collected.data.id} />
      </p>
    )
  }

  if (!user) {
    return (
      <Link to="/login" state={{ from: location }} className="block text-sm font-medium text-red-600 hover:underline">
        Log in to add it to the collection
      </Link>
    )
  }

  return (
    <div className="space-y-2">
      <button
        type="button"
        onClick={() => sync.mutate(idOrName)}
        disabled={sync.isPending}
        className="w-full rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
      >
        {sync.isPending ? 'Adding…' : 'Add to collection'}
      </button>
      {sync.isError && (
        <p role="alert" className="text-sm text-red-700">
          <span className="font-medium">{sync.error.title}</span>
          {sync.error.detail && ` ${sync.error.detail}`}
        </p>
      )}
    </div>
  )
}

function OpenLink({ id }: { id: string }) {
  return (
    <Link to={`/collection/${id}`} className="font-medium text-red-600 underline">
      Open in the collection
    </Link>
  )
}
