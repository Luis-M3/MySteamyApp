import { NgModule } from '@angular/core';
import { FavoritePageRoutingModule } from './favorite-routing-module';
import { FavoritePage } from './favorite.page';
import { SharedModule } from '../../shared/shared-module';

@NgModule({
  imports: [SharedModule, FavoritePageRoutingModule],
  declarations: [FavoritePage]
})
export class FavoritePageModule {}