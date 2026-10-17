import { useLocation } from 'react-router'

/** Navigation state carried from the catalog to a Pokémon page, so "Back to catalog" returns to the same page. */
export interface CatalogReturnState {
  catalogSearch: string
}

/** The state to pass when leaving the catalog (or a details page that came from it). */
export function useCatalogReturnState(): CatalogReturnState {
  const location = useLocation()
  const carried = (location.state as CatalogReturnState | null)?.catalogSearch
  return { catalogSearch: location.pathname === '/' ? location.search : (carried ?? '') }
}

/** Where "Back to catalog" should go: the catalog page the user came from, or the first page. */
export function useCatalogReturnPath(): string {
  return `/${useCatalogReturnState().catalogSearch}`
}
