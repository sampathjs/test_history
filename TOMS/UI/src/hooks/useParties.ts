import sortBy from 'lodash/sortBy';
import { useQuery } from 'react-query';

import { Party } from 'types';
import { buildUrl, request } from 'utils';

import { QueryKeys } from './types';

type PartyQueryValues = Record<'legalEntityId' | 'partyTypeId', number>;

export const useParties = (partyQueryValues?: PartyQueryValues) => {
  const params = partyQueryValues
    ? new URLSearchParams(
        Object.entries(partyQueryValues).map(([id, value]) => [
          id,
          String(value),
        ])
      ).toString()
    : '';

  return useQuery(
    [QueryKeys.PARTIES, params],
    () => request<Party[]>(buildUrl('parties', params)),
    { select: (data) => sortBy(data, 'name') }
  );
};
