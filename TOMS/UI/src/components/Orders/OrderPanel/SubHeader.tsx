import { useTimeZoneContext } from 'contexts';
import { Order } from 'types';
import { formatDateTimeWithTimezone, parseApiDate } from 'utils/date';

import * as Styles from './SubHeader.styles';

type Props = {
  order: Order;
};

export const SubHeader = (props: Props) => {
  const { order } = props;
  const { timeZone } = useTimeZoneContext();

  return (
    <Styles.SubHeader>
      <Styles.SubHeaderText>Order ID: {order.id}</Styles.SubHeaderText>
      <Styles.SubHeaderText>
        Exec:{' '}
        {formatDateTimeWithTimezone(parseApiDate(order.createdAt), timeZone)}
      </Styles.SubHeaderText>
      <Styles.Status>{order.displayStringOrderStatus}</Styles.Status>
    </Styles.SubHeader>
  );
};
