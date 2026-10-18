import type { RouteObject } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { CatalogPage } from '../features/catalog/CatalogPage'
import { CollectedPokemonPage } from '../features/collection/CollectedPokemonPage'
import { CollectionPage } from '../features/collection/CollectionPage'
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
      { path: 'collection', element: <CollectionPage /> },
      { path: 'collection/:id', element: <CollectedPokemonPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
