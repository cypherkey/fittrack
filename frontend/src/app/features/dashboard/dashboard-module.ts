import { NgModule } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { SharedModule } from '../../shared/shared-module';
import { DashboardPage } from './dashboard-page/dashboard-page';
import { DashboardRoutingModule } from './dashboard-routing-module';

@NgModule({
  declarations: [DashboardPage],
  imports: [SharedModule, DashboardRoutingModule, BaseChartDirective],
})
export class DashboardModule {}
