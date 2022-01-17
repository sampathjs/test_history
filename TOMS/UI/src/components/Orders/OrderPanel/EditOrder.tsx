import { useEffect, useMemo } from 'react';
import { FormProvider } from 'react-hook-form';

import {
  OrderPanelViewType,
  useAuthContext,
  useOrderPanelContext,
} from 'contexts';
import { useCreateComment } from 'hooks/useCreateComment';
import { useUpdateComment } from 'hooks/useUpdateComment';
import { useUpdateOrder } from 'hooks/useUpdateOrder';
import { Order, OrderTypeNameIds } from 'types';

import { Header } from './Header';
import { buildNewComment, getLatestNonActionComment } from './helpers';
import { OrderForm, useOrderForm, useOrderFormMappers } from './OrderForm';
import { ParsedFormInputs } from './OrderForm/types';
import * as Styles from './OrderPanel.styles';
import { SubHeader } from './SubHeader';
import { useOrderWithComments } from './useOrderWithComments';

type Props = {
  orderId: Order['id'];
};

export const EditOrder = (props: Props) => {
  const { orderId } = props;
  const { comments, isError, isLoading, order } = useOrderWithComments(orderId);

  const { mutateAsync: updateOrder } = useUpdateOrder();
  const { mutateAsync: createComment } = useCreateComment();
  const { mutateAsync: updateComment } = useUpdateComment();
  const { setView } = useOrderPanelContext();
  const { user } = useAuthContext();
  const { mapFormValuesToOrder, mapOrderToFormValues } = useOrderFormMappers();
  const latestComment = getLatestNonActionComment(comments);

  const defaultValues = useMemo(() => {
    if (order) {
      return {
        ...mapOrderToFormValues(order),
        comment: latestComment?.commentText,
      };
    }
  }, [order, latestComment, mapOrderToFormValues]);

  const form = useOrderForm({ defaultValues });

  const { reset } = form;

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const [orderSide, orderType] = form.watch(['orderSide', 'orderType']);

  if (isLoading || isError || !order || !comments) {
    // TODO: replace with proper loading and error states
    return null;
  }

  const onSubmit = async (values: ParsedFormInputs) => {
    const { comment } = values;
    const updates = mapFormValuesToOrder(values);

    const updatedOrder = {
      ...order,
      ...updates,
      idBaseCurrency: updates.idBaseCurrency ?? order.idBaseCurrency,
      idTermCurrency: updates.idTermCurrency ?? order.idTermCurrency,
    };

    await updateOrder(updatedOrder);

    if (latestComment) {
      await updateComment({
        comment: { ...latestComment, commentText: comment },
        id: order.id,
        idOrderType: order.idOrderType as OrderTypeNameIds,
      });
    }

    if (!latestComment && comment) {
      await createComment({
        comment: buildNewComment(comment, user.id),
        id: order.id,
        idOrderType: order.idOrderType as OrderTypeNameIds,
      });
    }

    setView({ type: OrderPanelViewType.OrderDetails, id: order.id });
  };

  return (
    <FormProvider {...form}>
      <Header side={orderSide}>
        <Styles.HeaderTitle>{orderType}</Styles.HeaderTitle>
      </Header>
      <SubHeader order={order} />
      <OrderForm onSubmit={onSubmit} />
    </FormProvider>
  );
};
