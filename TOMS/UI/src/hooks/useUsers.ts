import { useQuery } from 'react-query';

import { User } from 'types';
import { buildUrl, request } from 'utils';

import { QueryKeys } from './types';

type UserQueryValues = {
  email?: string;
  userId?: number;
  userRoleId?: number;
};

export const useUsers = (userQueryValues?: UserQueryValues) => {
  const params = userQueryValues
    ? new URLSearchParams(
        Object.entries(userQueryValues).map(([id, value]) => [
          id,
          String(value),
        ])
      ).toString()
    : '';

  return useQuery([QueryKeys.USERS, params], () =>
    request<User[]>(buildUrl('user', params))
  );
};
