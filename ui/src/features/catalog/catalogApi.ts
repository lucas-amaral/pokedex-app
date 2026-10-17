import { getJson } from '../../api/http'
import type { Page } from '../../api/types'
import type { PokemonSummary } from './types'

export interface CatalogPageRequest {
  page: number
  size: number
}

export function fetchCatalogPage({ page, size }: CatalogPageRequest, signal?: AbortSignal) {
  return getJson<Page<PokemonSummary>>('/api/v1/catalog/pokemon', { page, size }, signal)
}
