import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { ApiError } from '../../api/ApiError'
import type { Page } from '../../api/types'
import { type CatalogPageRequest, fetchCatalogPage } from './catalogApi'
import type { PokemonSummary } from './types'

const FIVE_MINUTES = 5 * 60 * 1000

/**
 * One page of the catalog. Pages already seen come from the query cache; while a new page loads,
 * the previous one stays on screen (placeholder data) so the grid does not flash empty.
 */
export function useCatalogPage(request: CatalogPageRequest) {
  return useQuery<Page<PokemonSummary>, ApiError>({
    queryKey: ['catalog', request.page, request.size],
    queryFn: ({ signal }) => fetchCatalogPage(request, signal),
    placeholderData: keepPreviousData,
    staleTime: FIVE_MINUTES,
  })
}
