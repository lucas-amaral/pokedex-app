import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import type { ApiError } from '../../api/ApiError'
import { useDocumentTitle } from '../../lib/useDocumentTitle'
import { logIn, register } from './authApi'
import { useAuth } from './authContext'
import { FormField } from './FormField'
import type { LoginState } from './LoginPage'
import type { LoginResponse } from './types'

/** Same rules as the service, so mistakes show before anything is sent. */
const registerSchema = z.object({
  username: z
    .string()
    .trim()
    .regex(/^[A-Za-z0-9._-]{3,30}$/, 'Use 3 to 30 letters, digits, dots, hyphens or underscores'),
  email: z.email('Enter a valid email address'),
  password: z
    .string()
    .min(8, 'Use at least 8 characters')
    .refine((password) => new TextEncoder().encode(password).length <= 72, 'Use at most 72 bytes')
    .refine((password) => password.trim().length > 0, 'A password cannot be only spaces'),
})

type RegisterValues = z.infer<typeof registerSchema>

const FIELD_BY_CONFLICT: Record<string, keyof RegisterValues> = { 'Username taken': 'username', 'Email taken': 'email' }

export function RegisterPage() {
  useDocumentTitle('Create an account')
  const location = useLocation()
  const state = (location.state ?? {}) as LoginState
  const navigate = useNavigate()
  const { user, signIn } = useAuth()
  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: '', email: '', password: '' },
  })
  const signUp = useMutation<LoginResponse, ApiError, RegisterValues>({
    mutationFn: async ({ username, email, password }) => {
      await register(username, email, password)
      return logIn(username, password)
    },
    onSuccess: (response) => {
      signIn(response)
      void navigate(state.from ?? '/', { replace: true })
    },
    onError: (error) => {
      const conflicting = FIELD_BY_CONFLICT[error.title]
      if (conflicting) form.setError(conflicting, { message: error.detail ?? error.title })
      for (const { field, message } of error.errors) {
        if (field === 'username' || field === 'email' || field === 'password') form.setError(field, { message })
      }
    },
  })
  const errors = form.formState.errors
  const handled = signUp.error && (FIELD_BY_CONFLICT[signUp.error.title] || signUp.error.errors.length > 0)


  // Already logged in (a bookmark, the back button): there is nothing to do here.
  if (user) return <Navigate to={state.from ?? '/'} replace />
  return (
    <section aria-labelledby="register-heading" className="mx-auto max-w-sm space-y-4 rounded-xl border border-slate-200 bg-white p-6">
      <h1 id="register-heading" className="text-xl font-semibold">
        Create an account
      </h1>
      <form onSubmit={form.handleSubmit((values) => signUp.mutate(values))} noValidate className="space-y-3">
        <FormField label="Username" autoComplete="username" error={errors.username} {...form.register('username')} />
        <FormField label="Email" type="email" autoComplete="email" error={errors.email} {...form.register('email')} />
        <FormField label="Password" type="password" autoComplete="new-password" error={errors.password} {...form.register('password')} />
        {signUp.isError && !handled && (
          <p role="alert" className="text-sm text-red-700">
            {signUp.error.title}
          </p>
        )}
        <button
          type="submit"
          disabled={signUp.isPending}
          className="w-full rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
        >
          {signUp.isPending ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" state={state} className="font-medium text-red-600 hover:underline">
          Log in
        </Link>
      </p>
    </section>
  )
}
