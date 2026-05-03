import { Injectable } from '@angular/core';
import { from, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CapacitorHttp } from '@capacitor/core';

@Injectable({ providedIn: 'root' })
export class HttpService {
  private baseUrl = 'https://www.cheapshark.com/api/1.0';

  get<T>(endpoint: string, params?: Record<string, string>): Observable<T> {
    const url = new URL(`${this.baseUrl}${endpoint}`);

    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        url.searchParams.set(k, v);
      });
    }

    return from(
      CapacitorHttp.get({
        url: url.toString(),
        headers: {
          'Accept': 'application/json'
        }
      })
    ).pipe(
      map(response => {
        if (response.status !== 200) {
          throw new Error(`HTTP ${response.status}`);
        }
        return response.data as T;
      })
    );
  }
}