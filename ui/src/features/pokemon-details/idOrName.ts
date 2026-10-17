/** Same rule as the service: a Pokédex number or a name made of letters, digits and hyphens, up to 50 characters. */
const ID_OR_NAME = /^[A-Za-z0-9-]{1,50}$/

export function isValidIdOrName(value: string): boolean {
  return ID_OR_NAME.test(value)
}
