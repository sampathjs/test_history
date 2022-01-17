import { useQueries } from 'react-query';

import { QueryKeys } from 'hooks/types';
import { Reference, ReferenceType } from 'types';
import { request } from 'utils';

export const useRefDataQueries = () =>
  useQueries([
    {
      queryKey: QueryKeys.REFERENCE_DATA,
      queryFn: () => request<Reference[]>('references'),
    },
    {
      queryKey: QueryKeys.REFERENCE_TYPE_DATA,
      queryFn: () => request<ReferenceType[]>('referenceTypes'),
    },
  ]);
