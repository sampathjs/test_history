import sumBy from 'lodash/sumBy';

import { FillStatusIds, OrderFill, OrderFillCreation, User } from 'types';
import { createApiDate } from 'utils/date';

import { FormFill } from '.';
import { FormValues } from './types';

export const mapFormValuesToFills = (
  values: FormValues,
  user: User
): OrderFillCreation[] => {
  return values.fills.map((fill) => ({
    id: 0,
    fillQuantity: Number(fill.volume),
    fillPrice: Number(fill.price),
    idTrader: user.id,
    idUpdatedBy: user.id,
    lastUpdateDateTime: createApiDate(),
    idFillStatus: FillStatusIds.FILL_STATUS_OPEN,
  }));
};

export const getTotalQuantity = (
  fills: OrderFill[] = [],
  formFills: FormFill[]
) => {
  return (
    sumBy(fills, 'fillQuantity') +
    sumBy(formFills, (formFill) => Number(formFill.volume))
  );
};
