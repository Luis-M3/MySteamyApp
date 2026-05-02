export interface Deal {
  dealID: string;
  title: string;
  storeID: string;
  gameID: string;
  salePrice: string;
  normalPrice: string;
  savings: string;
  dealRating: string;
  thumb: string;
  metacriticScore?: string;
}

export interface Store {
  storeID: string;
  storeName: string;
  images: {
    logo: string;
    icon: string;
    banner: string;
  };
}

export interface FavoriteGame {
  gameID: string;
  title: string;
  thumb: string;
  storeID: string;
  storeName: string;
  salePrice: string;
  normalPrice: string;
  savings: string;
  dealRating: string;
  dealID: string;
}

export interface GameDetail {
  info: {
    title: string;
    steamAppID: string;
    thumb: string;
  };
  cheapestPriceEver: {
    price: string;
    date: number;
  };
  deals: GameDeal[];
}

export interface GameDeal {
  storeID: string;
  dealID: string;
  price: string;
  retailPrice: string;
  savings: string;
}