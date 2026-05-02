import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Deal, Store } from '../../interfaces/models';
import { GameProviderService } from '../../services/game-provider';

@Component({
  selector: 'app-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  standalone: false,
})
export class CardComponent {
  @Input() deal!: Deal;
  @Input() isFavorite = false;
  @Output() favoriteToggled = new EventEmitter<Deal>();
  @Output() cardClicked = new EventEmitter<Deal>();

  constructor(public gameProvider: GameProviderService) {}

  get savings(): string {
    return Math.round(parseFloat(this.deal.savings)) + '%';
  }

  get storeName(): string {
    return this.gameProvider.getStoreById(this.deal.storeID)?.storeName ?? 'Unknown';
  }

  get storeLogo(): string {
    return this.gameProvider.getStoreLogo(this.deal.storeID);
  }

  onFavorite(event: Event): void {
    event.stopPropagation();
    this.favoriteToggled.emit(this.deal);
  }
}