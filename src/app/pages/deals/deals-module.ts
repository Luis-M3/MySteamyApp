import { NgModule } from '@angular/core';
import { DealsRoutingModule } from './deals-routing-module';
import { DealsPage } from './deals.page';
import { SharedModule } from '../../shared/shared-module';

@NgModule({
  imports: [SharedModule, DealsRoutingModule],
  declarations: [DealsPage]
})
export class DealsPageModule {}