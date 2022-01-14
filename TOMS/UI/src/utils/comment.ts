import orderBy from 'lodash/orderBy';

import { OrderComment } from 'types';

export const getLatestNonActionComment = (comments: OrderComment[] = []) => {
  return orderBy(comments, ['id'], ['desc']).filter(
    (comment) => comment.idAssociatedAction === null
  )[0];
};
