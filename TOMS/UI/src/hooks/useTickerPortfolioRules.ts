import groupBy from 'lodash/groupBy';
import keyBy from 'lodash/keyBy';
import { useQuery } from 'react-query';

import { KeyedPortfolioTickerRule, PortfolioTickerRuleResponse } from 'types';
import { buildUrl, request } from 'utils';

import { QueryKeys } from './types';

type PortfolioTickerRulesQueryValues = {
  includeDisplayStrings: boolean;
};

const getPortfolioTickerRules = (params?: string) =>
  request<PortfolioTickerRuleResponse[]>(
    buildUrl('tickerPortfolioRules', params)
  );

const groupRulesByPartyTicker = (data: PortfolioTickerRuleResponse[]) =>
  Object.fromEntries<KeyedPortfolioTickerRule>(
    Object.entries(groupBy(data, 'idParty')).map(([id, rules]) => [
      String(id),
      keyBy(rules, 'idTicker'),
    ])
  );

export const usePortfolioTickerRules = (
  portfolioTickerRulesQueryValues: PortfolioTickerRulesQueryValues = {
    // TODO: Ping Jens to add this standard query param to reduce payload size
    includeDisplayStrings: false,
  }
) => {
  const params = portfolioTickerRulesQueryValues
    ? new URLSearchParams(
        Object.entries(portfolioTickerRulesQueryValues).map(([id, value]) => [
          id,
          String(value),
        ])
      ).toString()
    : undefined;

  return useQuery(
    [QueryKeys.PORTFOLIO_TICKER_RULES, params],
    () => getPortfolioTickerRules(params),
    {
      select: groupRulesByPartyTicker,
    }
  );
};
