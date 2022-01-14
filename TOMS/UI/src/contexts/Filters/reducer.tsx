import { FilterValues } from 'types';

import { FilterAction, FilterPayload } from './types';

export enum FilterActions {
  SET_FILTER_DEFAULTS = 'SET_FILTER_DEFAULTS',
  UPDATE_FILTER = 'UPDATE_FILTER',
}

export const setFilterDefaultsAction = (updatedFilters: FilterPayload) => ({
  type: FilterActions.SET_FILTER_DEFAULTS,
  payload: updatedFilters,
});

export const updateFilterAction = (payload: FilterPayload) => ({
  type: FilterActions.UPDATE_FILTER,
  payload: payload.map(({ name, value }) => ({ name, value })),
});

export const initialReducerState: FilterValues = {};

export const filterReducer = (
  state: FilterValues,
  { payload, type }: FilterAction
) => {
  switch (type) {
    case FilterActions.SET_FILTER_DEFAULTS:
    case FilterActions.UPDATE_FILTER:
      return {
        ...state,
        ...Object.fromEntries(
          payload.map((filter) => [filter.name, filter.value])
        ),
      };
    default:
      return state;
  }
};
