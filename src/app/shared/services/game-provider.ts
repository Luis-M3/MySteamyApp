import { Injectable } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { HttpService } from '../../core/services/http';
import { Deal, Store, FavoriteGame, GameDetail } from '../interfaces/models';
import { Preferences } from '@capacitor/preferences';

@Injectable({ providedIn: 'root' })
export class GameProviderService {
  private storesCache: Store[] = [];
  private readonly FAV_KEY = 'favoriteGame';

  constructor(private http: HttpService) {}

  getStores(): Observable<Store[]> {
    return this.http.get<Store[]>('/stores').pipe(
      map(stores => {
        this.storesCache = stores;
        return stores;
      })
    );
  }

  getTopDeals(): Observable<Deal[]> {
    return this.http.get<Deal[]>('/deals', { pageSize: '5', sortBy: 'DealRating' });
  }

  searchDeals(query: string): Observable<Deal[]> {
    return this.http.get<Deal[]>('/deals', { title: query });
  }

  getGameDetail(gameID: string): Observable<GameDetail> {
    return this.http.get<GameDetail>('/games', { id: gameID });
  }

  getStoreById(storeID: string): Store | undefined {
    return this.storesCache.find(s => s.storeID === storeID);
  }

  getStoreLogo(storeID: string): string {
    const store = this.getStoreById(storeID);
    if (!store) return '';
    return `https://www.cheapshark.com${store.images.logo}`;
  }

  async saveFavorite(game: FavoriteGame): Promise<void> {
    await Preferences.set({ key: this.FAV_KEY, value: JSON.stringify(game) });
  }

  async getFavorite(): Promise<FavoriteGame | null> {
    const result = await Preferences.get({ key: this.FAV_KEY });
    return result.value ? JSON.parse(result.value) : null;
  }

  async removeFavorite(): Promise<void> {
    await Preferences.remove({ key: this.FAV_KEY });
  }

  getRedirectUrl(dealID: string): string {
    return `https://www.cheapshark.com/redirect?dealID=${dealID}`;
  }
}