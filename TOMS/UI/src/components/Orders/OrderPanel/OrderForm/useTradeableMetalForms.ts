import { OptionValue } from 'components/Select';
import { useRefData } from 'hooks/useRefData';
import { useTickerRules } from 'hooks/useTickerCounterpartyRules';
import { RefDataMap, RefTypeIds, TickerRules } from 'types';
import { getRefDataByTypeId } from 'utils/references';

export const getTradeableMetalForms = (
  counterpartyRuleset: TickerRules,
  refData?: RefDataMap
) =>
  getRefDataByTypeId(refData, RefTypeIds.METAL_FORM).filter(({ id }) =>
    counterpartyRuleset.metalFormIds.includes(id)
  );

export const useTradeableMetalForms = (idExternalBu?: OptionValue) => {
  const { data: counterpartyRules } = useTickerRules();
  const refData = useRefData();

  if (!idExternalBu || !refData || !counterpartyRules?.[idExternalBu]) {
    return [];
  }

  const counterpartyRuleset = counterpartyRules[idExternalBu];

  return getTradeableMetalForms(counterpartyRuleset, refData);
};
