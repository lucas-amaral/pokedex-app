import { setupServer } from 'msw/node'

/** Fake API for tests. Each test registers the responses it needs with server.use(...). */
export const server = setupServer()
