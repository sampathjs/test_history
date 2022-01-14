import { useMutation, useQueryClient } from 'react-query';

import { Order, OrderComment } from 'types';
import { request } from 'utils';

import { getEndpointTypeForOrderType } from './helpers';
import { MutationKeys, QueryKeys } from './types';

type UpdateCommentRequest = {
  comment: OrderComment;
  id: Order['id'];
  idOrderType: number;
};

const updateComment = ({ comment, id, idOrderType }: UpdateCommentRequest) => {
  const endpoint = `${getEndpointTypeForOrderType(
    idOrderType
  )}/${id}/comments/${comment.id}`;

  return request(endpoint, {
    method: 'PUT',
    body: JSON.stringify(comment),
  });
};

export const useUpdateComment = () => {
  const queryClient = useQueryClient();

  return useMutation(updateComment, {
    mutationKey: MutationKeys.UPDATE_COMMENT,
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries([QueryKeys.COMMENTS, id]);
    },
  });
};
