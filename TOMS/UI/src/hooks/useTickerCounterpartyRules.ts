import concat from 'lodash/concat';
import groupBy from 'lodash/groupBy';
import uniq from 'lodash/uniq';
import { useQuery } from 'react-query';

import { TickerRules } from 'types';
import { buildUrl, request } from 'utils';

import { QueryKeys, TickerRulesResponse } from './types';

type TickerRulesTuple = [number[], number[], number[]];

type TickerRulesQueryValues = {
  includeDisplayStrings: boolean;
};

const getTickerRules = (params?: string) =>
  request<TickerRulesResponse[]>(buildUrl('counterPartyTickerRules', params));

const transformRules = (data: TickerRulesResponse[]) =>
  Object.fromEntries<TickerRules>(
    Object.entries(groupBy(data, 'idCounterParty')).map(([id, rules]) => {
      const [productTickersIds, locationIds, metalFormIds] =
        rules.reduce<TickerRulesTuple>(
          (
            [accTickers, accMetalLocations, accMetalForms],
            { idMetalForm, idMetalLocation, idTicker }
          ) => [
            concat(accTickers, idTicker),
            concat(accMetalLocations, idMetalLocation),
            concat(accMetalForms, idMetalForm),
          ],
          [[], [], []]
        );

      return [
        id,
        {
          productTickerIds: uniq(productTickersIds),
          locationIds: uniq(locationIds),
          metalFormIds: uniq(metalFormIds),
        },
      ];
    })
  );

export const useTickerRules = (
  tickerRulesQueryValues: TickerRulesQueryValues = {
    includeDisplayStrings: false,
  }
) => {
  const params = tickerRulesQueryValues
    ? new URLSearchParams(
        Object.entries(tickerRulesQueryValues).map(([id, value]) => [
          id,
          String(value),
        ])
      ).toString()
    : undefined;

  return useQuery(
    [QueryKeys.COUNTERPARTY_TICKER_RULES, params],
    () => getTickerRules(params),
    {
      select: transformRules,
    }
  );
};
