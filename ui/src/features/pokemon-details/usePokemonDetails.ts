import { useQuery } from '@tanstack/react-query'
import type { ApiError } from '../../api/ApiError'
import { fetchPokemonDetails } from './detailsApi'
import type { PokemonDetails } from './types'

const FIVE_MINUTES = 5 * 60 * 1000

/** Only asks the service when `enabled`, so a malformed address never becomes a request. */
export function usePokemonDetails(idOrName: string, { enabled = true } = {}) {
  return useQuery<PokemonDetails, ApiError>({
    queryKey: ['pokemon', idOrName.toLowerCase()],
    queryFn: ({ signal }) => fetchPokemonDetails(idOrName, signal),
    staleTime: FIVE_MINUTES,
    enabled,
  })
}
