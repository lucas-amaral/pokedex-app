import { describe, expect, it } from 'vitest'
import { editFieldsSchema, toFormValues, toUpdateRequest } from './editFieldsSchema'
import { collectedBulbasaur } from './fixtures'

const valid = {
  nickname: ' Bulby ',
  notes: '',
  region: 'Kanto',
  localizedNames: [{ language: 'pt-BR', name: 'Bulbassauro' }],
  tags: 'Starter, kanto ,',
}

describe('editFieldsSchema', () => {
  it('turns the form into the request the service expects', () => {
    const parsed = editFieldsSchema.parse(valid)

    expect(toUpdateRequest(parsed, 3)).toEqual({
      version: 3,
      nickname: 'Bulby',
      notes: null,
      region: 'Kanto',
      localizedNames: { 'pt-BR': 'Bulbassauro' },
      tags: ['starter', 'kanto'],
    })
  })

  it('fills the form from a collected Pokémon', () => {
    expect(toFormValues(collectedBulbasaur)).toEqual({
      nickname: 'Bulby',
      notes: 'Our first starter',
      region: 'Kanto',
      localizedNames: [
        { language: 'ja', name: 'フシギダネ' },
        { language: 'pt-BR', name: 'Bulbassauro' },
      ],
      tags: 'starter',
    })
  })

  it.each([
    ['nickname', { nickname: 'n'.repeat(31) }],
    ['notes', { notes: 'n'.repeat(501) }],
    ['region', { region: 'r'.repeat(51) }],
    ['tags', { tags: 'has space' }],
    ['tags', { tags: Array.from({ length: 11 }, (_, index) => `tag-${index}`).join(',') }],
  ])('rejects an invalid %s', (field, change) => {
    const result = editFieldsSchema.safeParse({ ...valid, ...change })

    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path[0]).toBe(field)
  })

  it.each(['pt_BR', 'PT', 'portuguese'])('rejects the language tag %s', (language) => {
    const result = editFieldsSchema.safeParse({ ...valid, localizedNames: [{ language, name: 'Bulbassauro' }] })

    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path).toEqual(['localizedNames', 0, 'language'])
  })

  it('rejects the same language twice', () => {
    const result = editFieldsSchema.safeParse({
      ...valid,
      localizedNames: [
        { language: 'pt-BR', name: 'Bulbassauro' },
        { language: 'pt-BR', name: 'Bulbassaura' },
      ],
    })

    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path).toEqual(['localizedNames', 1, 'language'])
  })
})
