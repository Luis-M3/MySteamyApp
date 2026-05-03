import { NgModule } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { TabsPage } from './tabs.page';
import { TabsRoutingModule } from './tabs-routing.module';

@NgModule({
  imports: [IonicModule, CommonModule, TabsRoutingModule],
  declarations: [TabsPage]
})
export class TabsModule {}