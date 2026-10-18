import { z } from 'zod'
import type { CollectedPokemon } from './types'

/** Same rules as the service's CustomFields, so most mistakes are caught before a request is sent. */
const LANGUAGE_TAG = /^[a-z]{2,3}(-[A-Z]{2})?$/
const TAG = /^[a-z0-9]+(-[a-z0-9]+)*$/

const localizedName = z.object({
  language: z.string().trim().regex(LANGUAGE_TAG, "Use a language tag like 'pt-BR' or 'ja'"),
  name: z.string().trim().min(1, 'Enter the name in this language').max(50, 'At most 50 characters'),
})

export const editFieldsSchema = z.object({
  nickname: z.string().trim().max(30, 'At most 30 characters'),
  notes: z.string().trim().max(500, 'At most 500 characters'),
  region: z.string().trim().max(50, 'At most 50 characters'),
  localizedNames: z
    .array(localizedName)
    .max(10, 'At most 10 languages')
    .superRefine((rows, context) => {
      const seen = new Set<string>()
      rows.forEach((row, index) => {
        if (seen.has(row.language)) {
          context.addIssue({ code: 'custom', path: [index, 'language'], message: 'This language is already listed' })
        }
        seen.add(row.language)
      })
    }),
  /** Typed as "starter, Grass-Team"; becomes ["starter", "grass-team"]. */
  tags: z
    .string()
    .transform((text) => [...new Set(text.split(',').map((tag) => tag.trim().toLowerCase()).filter(Boolean))])
    .pipe(
      z
        .array(z.string().max(30, 'Tags are at most 30 characters').regex(TAG, 'Tags use lowercase letters, digits and hyphens'))
        .max(10, 'At most 10 tags'),
    ),
})

export type EditFieldsInput = z.input<typeof editFieldsSchema>
export type EditFieldsOutput = z.output<typeof editFieldsSchema>

export interface UpdateCollectedPokemonRequest {
  version: number
  nickname: string | null
  notes: string | null
  region: string | null
  localizedNames: Record<string, string>
  tags: string[]
}

export function toFormValues(pokemon: CollectedPokemon): EditFieldsInput {
  return {
    nickname: pokemon.nickname ?? '',
    notes: pokemon.notes ?? '',
    region: pokemon.region ?? '',
    localizedNames: Object.entries(pokemon.localizedNames).map(([language, name]) => ({ language, name })),
    tags: pokemon.tags.join(', '),
  }
}

/** Empty text is sent as null; the service treats both as "not set". */
export function toUpdateRequest(fields: EditFieldsOutput, version: number): UpdateCollectedPokemonRequest {
  return {
    version,
    nickname: fields.nickname || null,
    notes: fields.notes || null,
    region: fields.region || null,
    localizedNames: Object.fromEntries(fields.localizedNames.map(({ language, name }) => [language, name])),
    tags: fields.tags,
  }
}
