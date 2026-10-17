import { getJson } from '../../api/http'
import type { PokemonDetails } from './types'

export function fetchPokemonDetails(idOrName: string, signal?: AbortSignal) {
  return getJson<PokemonDetails>(`/api/v1/catalog/pokemon/${encodeURIComponent(idOrName)}`, {}, signal)
}
