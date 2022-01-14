import { FilterOption } from 'types';
import { spitAndIncludeBySeparator } from 'utils/string';

import * as Styles from './TickerItem.styles';

type TickerProps = {
  option: FilterOption;
  previousOption?: FilterOption;
};

export const TickerItem = ({ option, previousOption }: TickerProps) => {
  const tickerChunks = spitAndIncludeBySeparator(option.name, '/');

  const shouldAddNuancedStyles = (
    option: FilterOption,
    previousOption: FilterOption | undefined
  ) => {
    if (!previousOption) {
      return false;
    }

    return option.name.split('/')[0] === previousOption.name.split('/')[0];
  };

  return (
    <Styles.Ticker
      $hasNuancedStyles={shouldAddNuancedStyles(option, previousOption)}
    >
      {tickerChunks.map((chunk) => (
        <div key={chunk}>{chunk}</div>
      ))}
    </Styles.Ticker>
  );
};
