import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Browser } from '@capacitor/browser';
import { Deal } from '../../interfaces/models';
import { GameProviderService } from '../../services/game-provider';

@Component({
  selector: 'app-deal-detail-modal',
  templateUrl: './deal-detail-modal.component.html',
  styleUrls: ['./deal-detail-modal.component.scss'],
  standalone: false,
})
export class DealDetailModalComponent {
  @Input() deal!: Deal;

  constructor(
    private modalCtrl: ModalController,
    public gameProvider: GameProviderService
  ) {}

  get savings(): string {
    return Math.round(parseFloat(this.deal.savings)) + '%';
  }

  get storeName(): string {
    return this.gameProvider.getStoreById(this.deal.storeID)?.storeName ?? 'Unknown';
  }

  get storeLogo(): string {
    return this.gameProvider.getStoreLogo(this.deal.storeID);
  }

  async openDeal(): Promise<void> {
    await Browser.open({ url: this.gameProvider.getRedirectUrl(this.deal.dealID) });
  }

  dismiss(): void {
    this.modalCtrl.dismiss();
  }
}