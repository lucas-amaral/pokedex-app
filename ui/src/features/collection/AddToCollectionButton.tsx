import { Link } from 'react-router'
import { useSyncPokemon } from './collectionQueries'

/** Copies a catalog Pokémon into the collection and says whether it was added or only refreshed. */
export function AddToCollectionButton({ idOrName }: { idOrName: string }) {
  const sync = useSyncPokemon()

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
      {sync.isSuccess && (
        <p role="status" className="text-sm text-green-700">
          {sync.data.created ? 'Added to your collection.' : 'Already in your collection; PokeAPI data refreshed.'}{' '}
          <Link to={`/collection/${sync.data.pokemon.id}`} className="font-medium underline">
            Open in your collection
          </Link>
        </p>
      )}
      {sync.isError && (
        <p role="alert" className="text-sm text-red-700">
          <span className="font-medium">{sync.error.title}</span>
          {sync.error.detail && ` ${sync.error.detail}`}
        </p>
      )}
    </div>
  )
}
