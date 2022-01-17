import { Order } from 'types';
import { TimeZone } from 'types/date';
import { formatDateTimeWithTimezone, parseApiDate } from 'utils/date';
import { formatQuantity, formatValue } from 'utils/format';
import { formatOrderType } from 'utils/order';

import { OrderFill } from './OrderFill';
import { OrderSide } from './OrderSide';

type Props = {
  column: keyof Order;
  order: Order;
  timeZone: TimeZone;
};

const formatTextValue = (props: Props) => {
  const { column, order, timeZone } = props;

  if (column === 'displayStringOrderType') {
    return formatOrderType(order[column]);
  }

  if (column === 'createdAt') {
    return formatDateTimeWithTimezone(parseApiDate(order[column]), timeZone);
  }

  if (column === 'baseQuantity') {
    return formatQuantity(order[column]);
  }

  return formatValue(order[column]);
};

export const GridCellValue = (props: Props) => {
  const { column, order } = props;

  if (column === 'displayStringBuySell') {
    return <OrderSide side={order[column]} />;
  }

  if (column === 'fillPercentage') {
    return (
      <OrderFill side={order.displayStringBuySell} fillAmount={order[column]} />
    );
  }

  const value = formatTextValue(props);

  return <span title={String(value)}>{value}</span>;
};
