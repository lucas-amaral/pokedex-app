import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '../../api/ApiError'
import type { Page } from '../../api/types'
import { type CollectionPageRequest, fetchCollectedPokemon, fetchCollectionPage, syncPokemon } from './collectionApi'
import type { CollectedPokemon, CollectedPokemonSummary, SyncResult } from './types'

/** Query keys in one place, so a sync can refresh exactly the cached data it affects. */
export const collectionKeys = {
  all: ['collection'] as const,
  lists: () => [...collectionKeys.all, 'list'] as const,
  list: (request: CollectionPageRequest) => [...collectionKeys.lists(), request] as const,
  item: (id: string) => [...collectionKeys.all, 'item', id] as const,
}

export function useCollectionPage(request: CollectionPageRequest) {
  return useQuery<Page<CollectedPokemonSummary>, ApiError>({
    queryKey: collectionKeys.list(request),
    queryFn: ({ signal }) => fetchCollectionPage(request, signal),
    placeholderData: keepPreviousData,
  })
}

/**
 * The collected copy of a catalog Pokémon, or null when it is not in the collection. It shares the list key prefix,
 * so a sync refreshes it along with the other lists.
 */
export function useCollectedCopy(pokeapiId: number) {
  return useQuery<Page<CollectedPokemonSummary>, ApiError, CollectedPokemonSummary | null>({
    queryKey: collectionKeys.list({ page: 0, size: 1, pokeapiId }),
    queryFn: ({ signal }) => fetchCollectionPage({ page: 0, size: 1, pokeapiId }, signal),
    select: (page) => page.content[0] ?? null,
  })
}

export function useCollectedPokemon(id: string) {
  return useQuery<CollectedPokemon, ApiError>({
    queryKey: collectionKeys.item(id),
    queryFn: ({ signal }) => fetchCollectedPokemon(id, signal),
  })
}

/** Adds or refreshes a Pokémon; afterwards the stored copy is cached and every collection list refetches. */
export function useSyncPokemon() {
  const queryClient = useQueryClient()
  return useMutation<SyncResult, ApiError, string>({
    mutationFn: syncPokemon,
    onSuccess: (result) => {
      queryClient.setQueryData(collectionKeys.item(result.pokemon.id), result.pokemon)
      void queryClient.invalidateQueries({ queryKey: collectionKeys.lists() })
    },
  })
}
