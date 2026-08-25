import { ChartConfiguration, ChartData } from 'chart.js';
import { ExerciseHistoryEntry } from '../../core/models/exercise';
import { toDisplayWeight, weightUnitLabel } from './units';

export const WEIGHT_PROGRESS_CHART_COLORS = [
  '#005cbb',
  '#006a6a',
  '#8f4e00',
  '#ba1a1a',
  '#006e1c',
  '#6750a4',
  '#984061',
];

export interface WeightProgressSeries {
  label: string;
  history: ExerciseHistoryEntry[];
}

/** Build line-chart datasets: max display weight per local calendar day; skip empty series. */
export function buildWeightProgressChartData(
  series: WeightProgressSeries[],
  useMetric: boolean,
): ChartData<'line'> {
  const datasets: ChartData<'line'>['datasets'] = [];

  for (const item of series) {
    const points = historyToWeightPoints(item.history, useMetric);
    if (points.length === 0) {
      continue;
    }
    const color = WEIGHT_PROGRESS_CHART_COLORS[datasets.length % WEIGHT_PROGRESS_CHART_COLORS.length];
    datasets.push({
      label: item.label,
      data: points,
      borderColor: color,
      backgroundColor: color,
      tension: 0.2,
      pointRadius: 3,
      pointHoverRadius: 5,
    });
  }

  return { datasets };
}

export function buildWeightProgressChartOptions(
  useMetric: boolean,
  showLegend: boolean,
): ChartConfiguration<'line'>['options'] {
  return {
    responsive: true,
    maintainAspectRatio: false,
    parsing: false,
    interaction: {
      mode: 'nearest',
      intersect: false,
    },
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'day',
          tooltipFormat: 'PP',
          displayFormats: {
            day: 'MMM d, yyyy',
          },
        },
        title: {
          display: true,
          text: 'Date',
        },
      },
      y: {
        title: {
          display: true,
          text: `Weight (${weightUnitLabel(useMetric)})`,
        },
        beginAtZero: false,
      },
    },
    plugins: {
      legend: {
        display: showLegend,
        position: 'bottom',
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const y = context.parsed.y;
            if (y == null) {
              return context.dataset.label ?? '';
            }
            const prefix = context.dataset.label ? `${context.dataset.label}: ` : '';
            return `${prefix}${y} ${weightUnitLabel(useMetric)}`;
          },
        },
      },
    },
  };
}

/** Max display weight per workout calendar date. */
export function historyToWeightPoints(
  history: ExerciseHistoryEntry[],
  useMetric: boolean,
): { x: number; y: number }[] {
  const maxByDate = new Map<number, number>();

  for (const entry of history) {
    if (!entry.startedAt || entry.weightKg == null) {
      continue;
    }
    const weight = toDisplayWeight(entry.weightKg, useMetric);
    if (weight == null) {
      continue;
    }
    const dayMs = toLocalDateMs(entry.startedAt);
    const prev = maxByDate.get(dayMs);
    if (prev == null || weight > prev) {
      maxByDate.set(dayMs, weight);
    }
  }

  return [...maxByDate.entries()]
    .map(([dayMs, weight]) => ({ x: dayMs, y: weight }))
    .sort((a, b) => a.x - b.x);
}

/** Local calendar date (midnight) from an ISO workout startedAt. */
export function toLocalDateMs(iso: string): number {
  const d = new Date(iso);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}