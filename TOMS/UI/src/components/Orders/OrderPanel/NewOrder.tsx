import isEmpty from 'lodash/isEmpty';
import { useEffect } from 'react';
import { FormProvider } from 'react-hook-form';
import { useQueryClient } from 'react-query';

import { useAuthContext, useOrderPanelContext } from 'contexts';
import { QueryKeys } from 'hooks/types';
import { useCreateComment } from 'hooks/useCreateComment';
import { useCreateOrder } from 'hooks/useCreateOrder';
import { useOrder } from 'hooks/useOrder';
import { useParties } from 'hooks/useParties';
import { Order, OrderCommentCreation, OrderCreation, RefIds } from 'types';
import { API_DATE_TIME_FORMAT, getDateToday } from 'utils/date';
import { getSelectedOptionFromPartyData } from 'utils/form';

import { Header } from './Header';
import {
  getDefaultFormValues,
  getIdForOrderStatusPending,
  getTradeableParties,
} from './helpers';
import {
  OrderForm,
  ParsedFormInputs,
  useOrderForm,
  useOrderFormMappers,
} from './OrderForm';
import * as Styles from './OrderPanel.styles';

type Props = {
  copyFromId?: Order['id'];
};

export const NewOrder = ({ copyFromId }: Props) => {
  const { clearView } = useOrderPanelContext();
  const { user } = useAuthContext();
  const { data: partyData } = useParties();
  const queryClient = useQueryClient();
  const createOrder = useCreateOrder();
  const createComment = useCreateComment();
  const { mapFormValuesToOrder, mapOrderToFormValues } = useOrderFormMappers();
  const { data: copyFromOrder } = useOrder(copyFromId || -1);

  const [internalParties] = getTradeableParties(user, partyData);

  const jmPmmUnit = getSelectedOptionFromPartyData(
    internalParties,
    user.idDefaultInternalBu
  );

  const defaultValues = {
    ...getDefaultFormValues(),
    jmPmmUnit,
  };

  const form = useOrderForm({ defaultValues });

  const { reset } = form;

  useEffect(() => {
    if (copyFromOrder) {
      reset(mapOrderToFormValues(copyFromOrder));
    }
  }, [reset, copyFromOrder, mapOrderToFormValues]);

  const [orderSide, orderType] = form.watch(['orderSide', 'orderType']);

  const onSubmit = (data: ParsedFormInputs) => {
    console.log('submitted', data);

    const newOrder = mapFormValuesToOrder(data);
    const {
      idBaseCurrency,
      idBaseQuantityUnit,
      idMetalForm,
      idMetalLocation,
      idTermCurrency,
    } = newOrder;

    if (
      !idBaseCurrency ||
      !idTermCurrency ||
      !idBaseQuantityUnit ||
      !idMetalLocation ||
      !idMetalForm
    ) {
      // TODO: Throw error
      return;
    }

    const genericCreationProperties = {
      id: 0,
      createdAt: getDateToday().toFormat(API_DATE_TIME_FORMAT),
      lastUpdate: getDateToday().toFormat(API_DATE_TIME_FORMAT),
      idCreatedByUser: user.id,
      idUpdatedByUser: user.id,
    };

    const order: OrderCreation = {
      ...genericCreationProperties,
      ...newOrder,
      idBaseCurrency,
      idBaseQuantityUnit,
      idMetalForm,
      idMetalLocation,
      idTermCurrency,
      idOrderStatus: getIdForOrderStatusPending(data.orderType),
      version: 0,
      createdAt: getDateToday().toFormat(API_DATE_TIME_FORMAT),
      lastUpdate: getDateToday().toFormat(API_DATE_TIME_FORMAT),
      fillPercentage: 0,
    };

    console.log('parsed', order);

    createOrder.mutate(
      { order },
      {
        onSuccess: (id: number) => {
          if (!isEmpty(data.comment) && id && order && order.idOrderType) {
            const comment: OrderCommentCreation = {
              ...genericCreationProperties,
              commentText: data.comment,
              idLifeCycle: RefIds.LIFECYCLE_AUTHORISED_ACTIVE,
              idAssociatedAction: null,
            };

            createComment.mutate(
              {
                comment,
                id,
                idOrderType: order.idOrderType,
              },
              {
                onSuccess: () => {
                  clearView();
                  return queryClient.invalidateQueries(QueryKeys.ORDERS);
                },
              }
            );
          } else {
            clearView();
            return queryClient.invalidateQueries(QueryKeys.ORDERS);
          }
        },
      }
    );
  };

  return (
    <FormProvider {...form}>
      <Header side={orderSide}>
        <Styles.HeaderTitle>{orderType || 'New Order'}</Styles.HeaderTitle>
      </Header>
      <OrderForm onSubmit={onSubmit} />
    </FormProvider>
  );
};
