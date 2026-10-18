import { zodResolver } from '@hookform/resolvers/zod'
import { useId } from 'react'
import { type FieldError, type FieldPath, useFieldArray, useForm } from 'react-hook-form'
import type { ApiError } from '../../api/ApiError'
import { useUpdateCollectedPokemon } from './collectionQueries'
import { type EditFieldsInput, type EditFieldsOutput, editFieldsSchema, toFormValues, toUpdateRequest } from './editFieldsSchema'
import type { CollectedPokemon } from './types'

const CONFLICT = 409
const EDITABLE: readonly FieldPath<EditFieldsInput>[] = ['nickname', 'notes', 'region', 'localizedNames', 'tags']

interface EditFieldsFormProps {
  pokemon: CollectedPokemon
  onSaved: () => void
  onCancel: () => void
  onReloadLatest: () => void
}

/**
 * Edits the proprietary fields. Zod checks the input first; errors the service still finds are shown next to
 * the matching field, and a 409 offers to load the latest version instead of overwriting someone else's change.
 */
export function EditFieldsForm({ pokemon, onSaved, onCancel, onReloadLatest }: EditFieldsFormProps) {
  const form = useForm<EditFieldsInput, unknown, EditFieldsOutput>({
    resolver: zodResolver(editFieldsSchema),
    defaultValues: toFormValues(pokemon),
  })
  const names = useFieldArray({ control: form.control, name: 'localizedNames' })
  const save = useUpdateCollectedPokemon(pokemon.id)
  const errors = form.formState.errors

  function submit(fields: EditFieldsOutput) {
    save.mutate(toUpdateRequest(fields, pokemon.version), {
      onSuccess: onSaved,
      onError: (error) => showServerErrors(error),
    })
  }

  function showServerErrors(error: ApiError) {
    for (const { field, message } of error.errors) {
      const target = EDITABLE.find((name) => name === field)
      if (target) form.setError(target, { message })
    }
  }

  const isConflict = save.error?.status === CONFLICT

  return (
    <form onSubmit={form.handleSubmit(submit)} noValidate className="space-y-3 text-sm">
      <div className="grid gap-3 sm:grid-cols-2">
        <TextField label="Nickname" error={errors.nickname} {...form.register('nickname')} />
        <TextField label="Region" error={errors.region} {...form.register('region')} />
      </div>

      <NotesField error={errors.notes} {...form.register('notes')} />

      <fieldset className="space-y-2">
        <legend className="text-xs text-slate-500">Localized names</legend>
        {names.fields.map((row, index) => {
          const rowErrors = errors.localizedNames?.[index]
          return (
            <div key={row.id} className="flex items-start gap-2">
              <div className="w-28">
                <input
                  aria-label={`Language ${index + 1}`}
                  placeholder="pt-BR"
                  aria-invalid={rowErrors?.language ? true : undefined}
                  className={inputClass(rowErrors?.language)}
                  {...form.register(`localizedNames.${index}.language`)}
                />
                <ErrorText error={rowErrors?.language} />
              </div>
              <div className="flex-1">
                <input
                  aria-label={`Name in language ${index + 1}`}
                  placeholder="Bulbassauro"
                  aria-invalid={rowErrors?.name ? true : undefined}
                  className={inputClass(rowErrors?.name)}
                  {...form.register(`localizedNames.${index}.name`)}
                />
                <ErrorText error={rowErrors?.name} />
              </div>
              <button
                type="button"
                aria-label={`Remove language ${index + 1}`}
                onClick={() => names.remove(index)}
                className="mt-1 rounded-md px-2 py-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              >
                ×
              </button>
            </div>
          )
        })}
        <ErrorText error={errors.localizedNames?.root ?? (errors.localizedNames as FieldError | undefined)} />
        <button
          type="button"
          onClick={() => names.append({ language: '', name: '' })}
          className="text-sm font-medium text-red-600 hover:underline"
        >
          Add a language
        </button>
      </fieldset>

      <TextField
        label="Tags"
        hint="Separate tags with commas, for example: starter, kanto"
        error={firstError(errors.tags)}
        {...form.register('tags')}
      />

      {save.isError && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-red-800">
          <p className="font-medium">{save.error.title}</p>
          {save.error.detail && <p className="mt-0.5 text-red-700">{save.error.detail}</p>}
          {isConflict && (
            <button type="button" onClick={onReloadLatest} className="mt-2 font-medium underline">
              Load the latest version
            </button>
          )}
        </div>
      )}

      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-md px-3 py-1.5 text-slate-600 hover:bg-slate-100">
          Cancel
        </button>
        <button
          type="submit"
          disabled={save.isPending}
          className="rounded-md bg-red-600 px-3 py-1.5 font-medium text-white hover:bg-red-700 disabled:opacity-60"
        >
          {save.isPending ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  )
}

type TextFieldProps = React.ComponentProps<'input'> & { label: string; hint?: string; error?: FieldError }

/** The label holds only the field name; hint and error are linked with aria-describedby so they don't become the name. */
function TextField({ label, hint, error, ...input }: TextFieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  return (
    <div>
      <label htmlFor={id} className="text-xs text-slate-500">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        className={inputClass(error)}
        {...input}
      />
      {hint && !error && (
        <span id={hintId} className="mt-0.5 block text-xs text-slate-400">
          {hint}
        </span>
      )}
      <ErrorText id={errorId} error={error} />
    </div>
  )
}

function NotesField({ error, ...textarea }: React.ComponentProps<'textarea'> & { error?: FieldError }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="text-xs text-slate-500">
        Notes
      </label>
      <textarea
        id={id}
        rows={3}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={inputClass(error)}
        {...textarea}
      />
      <ErrorText id={`${id}-error`} error={error} />
    </div>
  )
}

function ErrorText({ id, error }: { id?: string; error?: FieldError }) {
  return error?.message ? (
    <span id={id} className="mt-0.5 block text-xs text-red-700">
      {error.message}
    </span>
  ) : null
}

/** Tags are validated one by one, so their error can sit on the list or on one item. */
function firstError(error: unknown): FieldError | undefined {
  if (!error || typeof error !== 'object') return undefined
  if ('message' in error && typeof error.message === 'string') return error as FieldError
  return Object.values(error).find(
    (item): item is FieldError => typeof item === 'object' && item !== null && 'message' in item,
  )
}

function inputClass(error?: FieldError) {
  return `mt-0.5 w-full rounded-md border bg-white px-2.5 py-1.5 text-sm ${error ? 'border-red-400' : 'border-slate-300'}`
}
