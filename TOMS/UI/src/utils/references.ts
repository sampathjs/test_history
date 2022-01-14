import { RefDataMap, RefTypeIds } from 'types';

export const getRefDataByTypeId = (
  data: RefDataMap | undefined,
  typeId: RefTypeIds | RefTypeIds[] | undefined
) =>
  data && typeId
    ? Object.values(data).filter(({ type }) =>
        type
          ? Array.isArray(typeId)
            ? typeId.includes(type.id)
            : type.id === typeId
          : undefined
      )
    : [];
