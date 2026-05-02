import { Component, OnInit, OnDestroy } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { GameProviderService } from '../../shared/services/game-provider';
import { DealDetailModalComponent } from '../../shared/components/deal-detail-modal/deal-detail-modal.component';
import { Deal, FavoriteGame } from '../../shared/interfaces/models';

@Component({
  selector: 'app-deals',
  templateUrl: './deals.page.html',
  styleUrls: ['./deals.page.scss'],
  standalone: false,
})
export class DealsPage implements OnInit, OnDestroy {
  topDeals: Deal[] = [];
  searchResults: Deal[] = [];
  isSearching = false;
  isLoading = true;
  favoriteGameID: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private gameProvider: GameProviderService,
    private modalCtrl: ModalController
  ) {}

  ngOnInit(): void {
    forkJoin([
      this.gameProvider.getStores(),
      this.gameProvider.getTopDeals()
    ]).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ([, deals]) => {
          this.topDeals = deals;
          this.isLoading = false;
        },
        error: () => { this.isLoading = false; }
      });

    this.loadFavorite();
  }

  async loadFavorite(): Promise<void> {
    const fav = await this.gameProvider.getFavorite();
    this.favoriteGameID = fav?.gameID ?? null;
  }

  onSearch(query: string): void {
    if (!query.trim()) {
      this.isSearching = false;
      this.searchResults = [];
      return;
    }
    this.isSearching = true;
    this.isLoading = true;
    this.gameProvider.searchDeals(query).subscribe({
      next: results => {
        this.searchResults = results;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  isFavorite(deal: Deal): boolean {
    return this.favoriteGameID === deal.gameID;
  }

  async toggleFavorite(deal: Deal): Promise<void> {
    if (this.isFavorite(deal)) {
      await this.gameProvider.removeFavorite();
      this.favoriteGameID = null;
    } else {
      const store = this.gameProvider.getStoreById(deal.storeID);
      const fav: FavoriteGame = {
        gameID: deal.gameID,
        title: deal.title,
        thumb: deal.thumb,
        storeID: deal.storeID,
        storeName: store?.storeName ?? '',
        salePrice: deal.salePrice,
        normalPrice: deal.normalPrice,
        savings: deal.savings,
        dealRating: deal.dealRating,
        dealID: deal.dealID
      };
      await this.gameProvider.saveFavorite(fav);
      this.favoriteGameID = deal.gameID;
    }
  }

  async openDetail(deal: Deal): Promise<void> {
    const modal = await this.modalCtrl.create({
      component: DealDetailModalComponent,
      componentProps: { deal },
      breakpoints: [0, 0.85],
      initialBreakpoint: 0.85,
      cssClass: 'deal-modal'
    });
    await modal.present();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}