export interface TickerRules {
  productTickerIds: number[];
  locationIds: number[];
  metalFormIds: number[];
}

export type PortfolioTickerRuleResponse = {
  idPortfolio: number;
  displayStringPortfolio?: string;
  idParty: number;
  displayStringParty?: string;
  idTicker: number;
  displayStringTicker?: string;
  idIndex: number;
  displayStringIndex?: string;
};

export type KeyedPortfolioTickerRule = {
  [idTicker: string]: PortfolioTickerRuleResponse;
};
