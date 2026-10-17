import { formatStatName } from '../lib/format'

export interface Stat {
  name: string
  value: number
}

const MAX_STAT = 255

export function StatBars({ stats }: { stats: Stat[] }) {
  const total = stats.reduce((sum, stat) => sum + stat.value, 0)

  return (
    <dl className="space-y-1.5 text-sm">
      {stats.map((stat) => {
        const label = formatStatName(stat.name)
        return (
          <div key={stat.name} className="grid grid-cols-[4.5rem_2.5rem_1fr] items-center gap-2">
            <dt className="text-slate-500">{label}</dt>
            <dd className="text-right font-medium tabular-nums">{stat.value}</dd>
            <dd>
              <div
                role="meter"
                aria-label={label}
                aria-valuenow={stat.value}
                aria-valuemin={0}
                aria-valuemax={MAX_STAT}
                className="h-2 overflow-hidden rounded-full bg-slate-100"
              >
                <div className="h-full rounded-full bg-red-500" style={{ width: `${(stat.value / MAX_STAT) * 100}%` }} />
              </div>
            </dd>
          </div>
        )
      })}
      <div className="grid grid-cols-[4.5rem_2.5rem_1fr] items-center gap-2 border-t border-slate-100 pt-1.5">
        <dt className="text-slate-500">Total</dt>
        <dd className="text-right font-semibold tabular-nums">{total}</dd>
      </div>
    </dl>
  )
}
