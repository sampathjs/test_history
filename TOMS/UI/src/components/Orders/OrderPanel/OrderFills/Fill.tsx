import { OrderFill } from 'types';
import { formatShortDateTime, parseApiDate } from 'utils/date';
import { formatValue } from 'utils/format';

import * as Styles from './OrderFills.styles';

type FillProps = {
  fill: OrderFill;
};

export const Fill = ({
  fill: {
    displayStringFillStatus,
    displayStringTrader,
    fillPrice,
    fillQuantity,
    idTrade,
    lastUpdateDateTime,
  },
}: FillProps) => {
  return (
    <Styles.TableRow>
      {/* TODO: Check if these numbers need to fixed/rounded */}
      <Styles.TableCell>{fillQuantity}</Styles.TableCell>
      <Styles.TableCell>{fillPrice}</Styles.TableCell>
      <Styles.TableCell>
        {formatShortDateTime(parseApiDate(lastUpdateDateTime))}
      </Styles.TableCell>
      <Styles.TableCell>{displayStringTrader}</Styles.TableCell>
      {/* TODO: Check that idTrade/displayStringFillStatus are correct vars to use here */}
      <Styles.TableCell>{formatValue(idTrade)}</Styles.TableCell>
      <Styles.TableCell>
        {formatValue(displayStringFillStatus)}
      </Styles.TableCell>
    </Styles.TableRow>
  );
};
