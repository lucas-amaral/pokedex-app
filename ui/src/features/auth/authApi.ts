import { sendJson } from '../../api/http'
import type { AuthUser, LoginResponse } from './types'

export async function logIn(username: string, password: string): Promise<LoginResponse> {
  return (await sendJson<LoginResponse>('POST', '/api/v1/auth/login', { username, password })).data
}

export async function register(username: string, email: string, password: string): Promise<AuthUser> {
  return (await sendJson<AuthUser>('POST', '/api/v1/auth/register', { username, email, password })).data
}
