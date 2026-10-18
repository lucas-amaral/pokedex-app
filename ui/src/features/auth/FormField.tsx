import { type ComponentProps, useId } from 'react'
import type { FieldError } from 'react-hook-form'

type FormFieldProps = ComponentProps<'input'> & { label: string; error?: FieldError }

/** A labelled input whose error is linked with aria-describedby, so the label alone stays its accessible name. */
export function FormField({ label, error, ...input }: FormFieldProps) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="text-sm text-slate-600">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`mt-1 w-full rounded-md border bg-white px-3 py-2 text-sm ${error ? 'border-red-400' : 'border-slate-300'}`}
        {...input}
      />
      {error?.message && (
        <p id={`${id}-error`} className="mt-1 text-xs text-red-700">
          {error.message}
        </p>
      )}
    </div>
  )
}
