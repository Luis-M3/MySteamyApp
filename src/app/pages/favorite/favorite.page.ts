import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { GameProviderService } from '../../shared/services/game-provider';
import { DealDetailModalComponent } from '../../shared/components/deal-detail-modal/deal-detail-modal.component';
import { FavoriteGame, Deal } from '../../shared/interfaces/models';

@Component({
  selector: 'app-favorite',
  templateUrl: './favorite.page.html',
  styleUrls: ['./favorite.page.scss'],
  standalone: false,
})
export class FavoritePage {
  favorite: FavoriteGame | null = null;
  isLoading = true;

  constructor(
    private gameProvider: GameProviderService,
    private modalCtrl: ModalController
  ) {}

  async ionViewWillEnter(): Promise<void> {
    this.isLoading = true;
    this.favorite = await this.gameProvider.getFavorite();
    this.isLoading = false;
  }

  get storeLogo(): string {
    return this.favorite ? this.gameProvider.getStoreLogo(this.favorite.storeID) : '';
  }

  get savings(): string {
    return this.favorite ? Math.round(parseFloat(this.favorite.savings)) + '%' : '';
  }

  async openDetail(): Promise<void> {
    if (!this.favorite) return;
    const deal: Deal = {
      dealID: this.favorite.dealID,
      title: this.favorite.title,
      storeID: this.favorite.storeID,
      gameID: this.favorite.gameID,
      salePrice: this.favorite.salePrice,
      normalPrice: this.favorite.normalPrice,
      savings: this.favorite.savings,
      dealRating: this.favorite.dealRating,
      thumb: this.favorite.thumb
    };
    const modal = await this.modalCtrl.create({
      component: DealDetailModalComponent,
      componentProps: { deal },
      breakpoints: [0, 0.85],
      initialBreakpoint: 0.85,
      cssClass: 'deal-modal'
    });
    await modal.present();
  }

  async removeFavorite(): Promise<void> {
    await this.gameProvider.removeFavorite();
    this.favorite = null;
  }
}