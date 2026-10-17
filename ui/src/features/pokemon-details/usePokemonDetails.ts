import { useQuery } from '@tanstack/react-query'
import type { ApiError } from '../../api/ApiError'
import { fetchPokemonDetails } from './detailsApi'
import type { PokemonDetails } from './types'

const FIVE_MINUTES = 5 * 60 * 1000

export function usePokemonDetails(idOrName: string) {
  return useQuery<PokemonDetails, ApiError>({
    queryKey: ['pokemon', idOrName.toLowerCase()],
    queryFn: ({ signal }) => fetchPokemonDetails(idOrName, signal),
    staleTime: FIVE_MINUTES,
  })
}
