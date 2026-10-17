import { useEffect } from 'react'

const APP_NAME = 'Pokédex'

/** Names the browser tab after the page ("Pikachu · Pokédex") and restores the previous name on leaving. */
export function useDocumentTitle(pageTitle: string | undefined) {
  useEffect(() => {
    if (!pageTitle) return
    const previous = document.title
    document.title = `${pageTitle} · ${APP_NAME}`
    return () => {
      document.title = previous
    }
  }, [pageTitle])
}
