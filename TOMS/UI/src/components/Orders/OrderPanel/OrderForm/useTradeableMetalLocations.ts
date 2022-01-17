import { OptionValue } from 'components/Select';
import { useRefData } from 'hooks/useRefData';
import { useTickerRules } from 'hooks/useTickerCounterpartyRules';
import { RefDataMap, RefTypeIds, TickerRules } from 'types';
import { getRefDataByTypeId } from 'utils/references';

export const getTradeableMetalLocations = (
  counterpartyRuleset: TickerRules,
  refData?: RefDataMap
) =>
  getRefDataByTypeId(refData, RefTypeIds.METAL_LOCATION).filter(({ id }) =>
    counterpartyRuleset.locationIds.includes(id)
  );

export const useTradeableMetalLocations = (idExternalBu?: OptionValue) => {
  const { data: counterpartyRules } = useTickerRules();
  const refData = useRefData();

  if (!idExternalBu || !refData || !counterpartyRules?.[idExternalBu]) {
    return [];
  }

  const counterpartyRuleset = counterpartyRules[idExternalBu];

  return getTradeableMetalLocations(counterpartyRuleset, refData);
};
