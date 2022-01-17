import { useComments } from 'hooks/useComments';
import { useOrder } from 'hooks/useOrder';
import { Order } from 'types';

export const useOrderWithComments = (
  id: Order['id'],
  versionId?: Order['version']
) => {
  const orderQuery = useOrder(id, versionId);
  const commentsQuery = useComments(orderQuery.data);
  const isLoading = orderQuery.isLoading || commentsQuery.isLoading;
  const isError = orderQuery.isError || commentsQuery.isError;

  return {
    order: orderQuery.data,
    comments: commentsQuery.data,
    isLoading,
    isError,
  };
};
