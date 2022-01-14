import {
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableHeaderRow,
  TableRow,
} from 'components/Table';
import { useAuthContext, useTimeZoneContext } from 'contexts';
import { useOrderVersions } from 'hooks/useOrderVersions';
import { Order } from 'types';
import { formatDateTimeWithTimezone, parseApiDate } from 'utils/date';

import { getPreviousVersionNumbers } from './helpers';
import * as Styles from './OrderHistory.styles';

type Props = {
  orderId: Order['id'];
  latestVersion: Order['version'] | undefined;
  activeVersion: Order['version'] | undefined;
  onChangeVersion: (versionId: Order['version']) => void;
  visible: boolean;
};

export const OrderHistory = ({
  activeVersion,
  latestVersion,
  onChangeVersion,
  orderId,
  visible,
}: Props) => {
  const {
    data: orders,
    isError,
    isLoading,
  } = useOrderVersions(orderId, getPreviousVersionNumbers(latestVersion));
  const { user } = useAuthContext();
  const { timeZone } = useTimeZoneContext();

  if (isLoading || isError || !orders) {
    return null;
  }

  const isSelected = activeVersion || latestVersion;

  return visible ? (
    <>
      <Styles.HistoryTable>
        <TableHeader>
          <TableHeaderRow>
            <TableHeaderCell>Order ID</TableHeaderCell>
            <TableHeaderCell>Version</TableHeaderCell>
            <TableHeaderCell>Updated By</TableHeaderCell>
            <TableHeaderCell>Updated At</TableHeaderCell>
            <TableHeaderCell>Order Status</TableHeaderCell>
          </TableHeaderRow>
        </TableHeader>
        <TableBody>
          {orders.map(
            ({ displayStringOrderStatus, id, lastUpdate, version }) => (
              <TableRow
                key={version}
                active={version === isSelected}
                onClick={() => onChangeVersion(version)}
              >
                <TableCell>{id}</TableCell>
                <TableCell>{version}</TableCell>
                <TableCell>
                  {user.firstName} {user.lastName}
                </TableCell>
                <TableCell>
                  {formatDateTimeWithTimezone(
                    parseApiDate(lastUpdate),
                    timeZone
                  )}
                </TableCell>
                <TableCell>{displayStringOrderStatus}</TableCell>
              </TableRow>
            )
          )}
        </TableBody>
      </Styles.HistoryTable>
    </>
  ) : null;
};
