export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  idLifecycleStatus: number;
  roleId: number;
  tradeableCounterPartyIds: number[];
  tradeableInternalPartyIds: number[];
  tradeablePortfolioIds: number[];
  idDefaultInternalBu: number;
  idDefaultInternalPortfolio: number;
}
