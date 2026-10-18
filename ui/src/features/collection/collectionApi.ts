import { getJson, sendJson } from '../../api/http'
import type { Page } from '../../api/types'
import type { CollectedPokemon, CollectedPokemonSummary, SyncResult } from './types'

const CREATED = 201

export async function syncPokemon(idOrName: string): Promise<SyncResult> {
  const response = await sendJson<CollectedPokemon>('POST', '/api/v1/pokemon/sync', { idOrName })
  return { pokemon: response.data, created: response.status === CREATED }
}

export interface CollectionPageRequest {
  page: number
  size: number
  name?: string
  tag?: string
  /** Exact PokeAPI number: tells whether a catalog Pokémon is already in the collection. */
  pokeapiId?: number
}

export function fetchCollectionPage({ page, size, name, tag, pokeapiId }: CollectionPageRequest, signal?: AbortSignal) {
  return getJson<Page<CollectedPokemonSummary>>(
    '/api/v1/pokemon',
    { page, size, name: name || undefined, tag: tag || undefined, pokeapiId },
    signal,
  )
}

export function fetchCollectedPokemon(id: string, signal?: AbortSignal) {
  return getJson<CollectedPokemon>(`/api/v1/pokemon/${encodeURIComponent(id)}`, {}, signal)
}
