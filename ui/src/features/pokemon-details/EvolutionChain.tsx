import { Link } from 'react-router'
import { useCatalogReturnState } from '../../app/catalogReturn'
import { formatEvolutionCondition, formatPokemonName } from '../../lib/format'
import type { EvolutionNode } from './types'

interface EvolutionChainProps {
  chain: EvolutionNode
  /** Species of the Pokémon on screen; an alternate form highlights its species. */
  currentSpecies: string
  /** Name of the Pokémon on screen when it is an alternate form, which evolves as its species does. */
  alternateForm?: string
}

/**
 * Draws the evolution tree recursively: each stage, then a column with every branch it can evolve into.
 * Stages stack vertically on phones and run left to right on wider screens.
 */
export function EvolutionChain({ chain, currentSpecies, alternateForm }: EvolutionChainProps) {
  const species = formatPokemonName(currentSpecies)
  return (
    <section aria-labelledby="evolution-heading" className="space-y-3">
      <h2 id="evolution-heading" className="font-semibold">
        Evolution chain
      </h2>
      {alternateForm && (
        <p role="note" className="rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-600">
          {formatPokemonName(alternateForm)} is an alternate form of {species}. Evolution belongs to the species, so this is
          the {species} chain.
        </p>
      )}
      <Stage node={chain} currentSpecies={currentSpecies} />
    </section>
  )
}

/** Above this many branches (Eevee has 8), they are laid out as a grid below the stage instead of a column beside it. */
const MAX_BRANCHES_IN_A_ROW = 2

function Stage({ node, currentSpecies }: { node: EvolutionNode; currentSpecies: string }) {
  const fansOut = node.evolvesTo.length > MAX_BRANCHES_IN_A_ROW

  if (fansOut) {
    return (
      <div className="flex flex-col items-center gap-3">
        <StageLink node={node} isCurrent={node.name === currentSpecies} />
        <ul className="grid grid-cols-2 gap-x-2 gap-y-3 sm:grid-cols-4">
          {node.evolvesTo.map((next) => (
            <li key={next.speciesId} className="flex flex-col items-center justify-end gap-1">
              <Condition node={next} arrow="↓" />
              <Stage node={next} currentSpecies={currentSpecies} />
            </li>
          ))}
        </ul>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center gap-2 sm:flex-row">
      <StageLink node={node} isCurrent={node.name === currentSpecies} />
      {node.evolvesTo.length > 0 && (
        <div className="flex flex-col gap-2">
          {node.evolvesTo.map((next) => (
            <div key={next.speciesId} className="flex flex-col items-center gap-2 sm:flex-row">
              <Condition node={next} arrow="→" />
              <Stage node={next} currentSpecies={currentSpecies} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/** The condition to reach a stage, with an arrow: down on phones, or the given direction on wider screens. */
function Condition({ node, arrow }: { node: EvolutionNode; arrow: '→' | '↓' }) {
  return (
    <span className="flex flex-col items-center text-center text-xs text-slate-500 sm:w-24">
      {node.condition && formatEvolutionCondition(node.condition)}
      <span aria-hidden="true" className="text-slate-400">
        <span className="sm:hidden">↓</span>
        <span className="hidden sm:inline">{arrow}</span>
      </span>
    </span>
  )
}

function StageLink({ node, isCurrent }: { node: EvolutionNode; isCurrent: boolean }) {
  const returnState = useCatalogReturnState()

  return (
    <Link
      to={`/pokemon/${node.name}`}
      state={returnState}
      aria-current={isCurrent ? 'page' : undefined}
      className={`flex w-24 flex-col items-center rounded-lg border p-1.5 text-xs font-medium transition hover:border-slate-300 hover:bg-slate-50 ${
        isCurrent ? 'border-red-300 bg-red-50' : 'border-slate-200 bg-white'
      }`}
    >
      {node.spriteUrl && <img src={node.spriteUrl} alt="" width={64} height={64} loading="lazy" className="size-16 [image-rendering:pixelated]" />}
      {formatPokemonName(node.name)}
    </Link>
  )
}
