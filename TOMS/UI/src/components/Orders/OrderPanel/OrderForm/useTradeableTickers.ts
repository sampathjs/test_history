import { OptionValue } from 'components/Select';
import { useAuthContext } from 'contexts';
import { useRefData } from 'hooks/useRefData';
import { useTickerRules } from 'hooks/useTickerCounterpartyRules';
import { usePortfolioTickerRules } from 'hooks/useTickerPortfolioRules';
import {
  KeyedPortfolioTickerRule,
  RefDataMap,
  RefTypeIds,
  TickerRules,
  User,
} from 'types';
import { getRefDataByTypeId } from 'utils/references';

const getTradeableTickerIdsFromPortfolioRuleset = (
  portfolioRuleset: KeyedPortfolioTickerRule,
  user: User
) =>
  Object.entries(portfolioRuleset).reduce<number[]>(
    (acc, [idTicker, ruleset]) =>
      user.tradeablePortfolioIds.includes(ruleset?.idPortfolio)
        ? [...acc, Number(idTicker)]
        : acc,
    []
  );

const getTradeableTickers = (
  refData: RefDataMap,
  user: User,
  counterpartyRuleset: TickerRules,
  portfolioRuleset: KeyedPortfolioTickerRule
) => {
  const tradeableTickerIdsFromCounterpartyRuleset =
    counterpartyRuleset.productTickerIds;

  const tradeableTickerIdsFromPortfolioRuleset =
    getTradeableTickerIdsFromPortfolioRuleset(portfolioRuleset, user);

  const tickerIds = tradeableTickerIdsFromCounterpartyRuleset.filter((id) =>
    tradeableTickerIdsFromPortfolioRuleset.includes(id)
  );

  return getRefDataByTypeId(refData, RefTypeIds.TICKER).filter(({ id }) =>
    tickerIds.includes(id)
  );
};

export const useTradeableTickers = (
  idInternalBu?: OptionValue,
  idExternalBu?: OptionValue
) => {
  const refData = useRefData();
  const { data: counterpartyTickerRules } = useTickerRules();
  const { data: portfolioTickerRules } = usePortfolioTickerRules();
  const { user } = useAuthContext();

  if (
    !refData ||
    !user ||
    !idInternalBu ||
    !idExternalBu ||
    !counterpartyTickerRules?.[idExternalBu] ||
    !portfolioTickerRules?.[idInternalBu]
  ) {
    return [];
  }

  const counterpartyTickerRuleset = counterpartyTickerRules[idExternalBu];
  const portfolioTickerRuleset = portfolioTickerRules[idInternalBu];

  return getTradeableTickers(
    refData,
    user,
    counterpartyTickerRuleset,
    portfolioTickerRuleset
  );
};
