import { useMutation, useQueryClient } from 'react-query';

import { Order, OrderComment, OrderCommentCreation } from 'types';
import { request } from 'utils';

import { getEndpointTypeForOrderType } from './helpers';
import { MutationKeys, QueryKeys } from './types';

type CreateCommentRequest = {
  comment: OrderCommentCreation;
  id: Order['id'];
  idOrderType: number;
};

const createComment = ({ comment, id, idOrderType }: CreateCommentRequest) => {
  const endpoint = getEndpointTypeForOrderType(idOrderType);

  return request<OrderComment['id']>(`${endpoint}/${id}/comments`, {
    method: 'POST',
    body: JSON.stringify(comment),
  });
};

export const useCreateComment = () => {
  const queryClient = useQueryClient();

  return useMutation(createComment, {
    mutationKey: MutationKeys.CREATE_COMMENT,
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries([QueryKeys.COMMENTS, id]);
    },
  });
};
