import type { RouteObject } from 'react-router'
import { CatalogPage } from '../features/catalog/CatalogPage'
import { PokemonDetailsPage } from '../features/pokemon-details/PokemonDetailsPage'
import { App } from './App'
import { NotFoundPage } from './NotFoundPage'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <CatalogPage /> },
      { path: 'pokemon/:idOrName', element: <PokemonDetailsPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
