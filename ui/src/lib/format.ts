/** "mr-mime" → "Mr Mime". PokeAPI names are lowercase and hyphenated. */
export function formatPokemonName(name: string): string {
  return name
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

/** 1 → "#001", 1025 → "#1025". */
export function formatDexNumber(id: number): string {
  return `#${String(id).padStart(3, '0')}`
}

const weightFormat = new Intl.NumberFormat('en', { maximumFractionDigits: 1 })

/** 6.9 → "6.9 kg", 13 → "13 kg". */
export function formatWeight(kilograms: number): string {
  return `${weightFormat.format(kilograms)} kg`
}
