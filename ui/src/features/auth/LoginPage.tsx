import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, Navigate, type Location, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import type { ApiError } from '../../api/ApiError'
import { useDocumentTitle } from '../../lib/useDocumentTitle'
import { logIn } from './authApi'
import { useAuth } from './authContext'
import { FormField } from './FormField'
import type { LoginResponse } from './types'

const loginSchema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})

type LoginValues = z.infer<typeof loginSchema>

/** Pages can send people here with { from, expired } in the navigation state. */
export interface LoginState {
  from?: Location
  expired?: boolean
}

export function LoginPage() {
  useDocumentTitle('Log in')
  const location = useLocation()
  const state = (location.state ?? {}) as LoginState
  const navigate = useNavigate()
  const { user, signIn } = useAuth()
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { username: '', password: '' } })
  const login = useMutation<LoginResponse, ApiError, LoginValues>({
    mutationFn: ({ username, password }) => logIn(username, password),
    onSuccess: (response) => {
      signIn(response)
      void navigate(state.from ?? '/', { replace: true })
    },
  })


  // Already logged in (a bookmark, the back button): there is nothing to do here.
  if (user) return <Navigate to={state.from ?? '/'} replace />
  return (
    <section aria-labelledby="login-heading" className="mx-auto max-w-sm space-y-4 rounded-xl border border-slate-200 bg-white p-6">
      <h1 id="login-heading" className="text-xl font-semibold">
        Log in
      </h1>
      {state.expired && <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">Your session expired. Log in again.</p>}
      <form onSubmit={form.handleSubmit((values) => login.mutate(values))} noValidate className="space-y-3">
        <FormField label="Username" autoComplete="username" error={form.formState.errors.username} {...form.register('username')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={form.formState.errors.password}
          {...form.register('password')}
        />
        {login.isError && (
          <p role="alert" className="text-sm text-red-700">
            {login.error.status === 401 ? (login.error.detail ?? login.error.title) : login.error.title}
          </p>
        )}
        <button
          type="submit"
          disabled={login.isPending}
          className="w-full rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
        >
          {login.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="text-sm text-slate-500">
        New here?{' '}
        <Link to="/register" state={state} className="font-medium text-red-600 hover:underline">
          Create an account
        </Link>
      </p>
    </section>
  )
}
