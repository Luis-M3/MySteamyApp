import { NgModule } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { TabsPage } from './tabs.page';
import { TabsRoutingModule } from './tabs-routing.module';

@NgModule({
  imports: [IonicModule, TabsRoutingModule],
  declarations: [TabsPage]
})
export class TabsModule {}