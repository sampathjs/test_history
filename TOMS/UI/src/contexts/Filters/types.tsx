import { FilterValue } from 'types';

import { FilterActions } from './reducer';

export type FilterPayload = { name: string; value: FilterValue }[];

export type FilterAction = {
  type: FilterActions;
  payload: FilterPayload;
};
