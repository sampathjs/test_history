import { useMemo } from 'react';

import { RefDataMap } from 'types';

import { useRefDataQueries } from './queries/useRefDataQueries';

export const useRefData = (): RefDataMap | undefined => {
  const [
    { data: references, isSuccess: isReferencesSuccess },
    { data: referenceTypes, isSuccess: isReferenceTypesSuccess },
  ] = useRefDataQueries();

  return useMemo(() => {
    if (
      !isReferencesSuccess ||
      !isReferenceTypesSuccess ||
      !references ||
      !referenceTypes
    ) {
      return undefined;
    }

    return references.reduce(
      (acc, { idType, ...reference }) => ({
        ...acc,
        [reference.id]: {
          ...reference,
          type: referenceTypes.find((refType) => idType === refType.id),
        },
      }),
      {}
    );
  }, [
    references,
    referenceTypes,
    isReferencesSuccess,
    isReferenceTypesSuccess,
  ]);
};
