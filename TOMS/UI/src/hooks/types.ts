import { Order } from 'types';

export enum QueryKeys {
  COMMENTS = 'comments',
  REFERENCE_DATA = 'reference-data',
  REFERENCE_TYPE_DATA = 'reference-type-data',
  PARTIES = 'parties',
  ORDER = 'order',
  ORDER_FILLS = 'order-fills',
  ORDER_STATUS = 'order-status',
  ORDERS = 'orders',
  ORDER_VERSIONS = 'order-versions',
  USERS = 'users',
  PORTFOLIO_TICKER_RULES = 'portfolio-ticker-rules',
  COUNTERPARTY_TICKER_RULES = 'counterparty-ticker-rules',
}

export enum MutationKeys {
  CREATE_ORDER = 'create-order',
  UPDATE_ORDER = 'update-order',
  CREATE_COMMENT = 'create-comment',
  UPDATE_COMMENT = 'update-comment',
  CREATE_FILLS = 'create-fills',
}

export type OrdersResponse = {
  content: Order[];
  totalElements: number;
};

export interface TickerRulesResponse {
  idCounterParty: number;
  counterPartyDisplayName: string;
  idTicker: number;
  tickerDisplayName: string;
  idMetalLocation: number;
  metalLocationDisplayString: string;
  idMetalForm: number;
  metalFormDisplayString: string;
  accountName: string;
}
