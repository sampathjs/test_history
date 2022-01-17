import { OptionType, OptionValue } from 'components/Select';
import { Party, RefData } from 'types';
import { Nullable } from 'types/util';

export const isSelectedOptionAvailable = (
  options: OptionType[],
  selected?: Nullable<OptionType>
) => options.find((option) => option.value === selected?.value);

export const getValueFromSelectedOption = (
  selectedOption: Nullable<OptionType>
) => {
  if (selectedOption === null) {
    return selectedOption;
  }

  return selectedOption.value;
};

export const getSelectOptionsFromRefData = (refDataByTypeId: RefData[]) =>
  refDataByTypeId.map(({ id, name }) => ({
    label: name,
    value: id,
  }));

export const makeSelectOptionFromPartyData = (partyData: Party) => {
  return {
    label: partyData.name,
    value: partyData.id,
  };
};

export const getSelectOptionsFromPartyData = (partyData: Party[]) =>
  partyData.map(makeSelectOptionFromPartyData);

export const getSelectedOptionFromPartyData = (
  partyData: Party[],
  value: OptionValue
) => {
  const selectedParty = partyData.find((party) => party.id === value);

  if (!selectedParty) {
    return undefined;
  }

  return makeSelectOptionFromPartyData(selectedParty);
};

export const makeSelectOptionFromRefData = (refDataByTypeId: RefData) => {
  const { id, name } = refDataByTypeId;
  return {
    label: name,
    value: id,
  };
};
