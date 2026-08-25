import { Component, Input, computed, inject, signal } from '@angular/core';
import { ChartConfiguration, ChartData } from 'chart.js';
import 'chartjs-adapter-date-fns';
import { AuthService } from '../../../core/auth.service';
import {
  WeightProgressSeries,
  buildWeightProgressChartData,
  buildWeightProgressChartOptions,
} from '../../utils/weight-progress-chart';

@Component({
  selector: 'app-weight-progress-chart',
  templateUrl: './weight-progress-chart.html',
  standalone: false,
  styleUrl: './weight-progress-chart.scss',
})
export class WeightProgressChart {
  private readonly auth = inject(AuthService);

  /** When true, show spinner instead of chart/empty states. */
  @Input() set loading(value: boolean) {
    this.loadingSignal.set(!!value);
  }

  /** One series per exercise (label + completed-set history). Empty series are omitted from the chart. */
  @Input() set series(value: WeightProgressSeries[] | null | undefined) {
    this.seriesSignal.set(value ?? []);
  }

  /** Message when series is empty (e.g. no favorites). */
  @Input() emptyMessage = 'No data to chart yet.';

  /** Message when series exist but none have weighted completed history. */
  @Input() noDataMessage = 'No completed sets with weight yet. Log a workout to see progress here.';

  /** Show chart legend (useful for multi-series dashboard). */
  @Input() showLegend = true;

  readonly loadingSignal = signal(false);
  readonly seriesSignal = signal<WeightProgressSeries[]>([]);

  readonly chartData = computed((): ChartData<'line'> => {
    const useMetric = this.auth.user()?.useMetric ?? true;
    return buildWeightProgressChartData(this.seriesSignal(), useMetric);
  });

  readonly hasChartData = computed(() =>
    this.chartData().datasets.some((dataset) => dataset.data.length > 0),
  );

  readonly chartOptions = computed((): ChartConfiguration<'line'>['options'] => {
    const useMetric = this.auth.user()?.useMetric ?? true;
    return buildWeightProgressChartOptions(useMetric, this.showLegend);
  });
}