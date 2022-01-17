import { Nullable } from './util';

export enum PartyTypeIds {
  INTERNAL_BUSINESS_UNIT = 1,
  EXTERNAL_BUSINESS_UNIT = 2,
}

export type Party = {
  id: number;
  name: string;
  typeId: number;
  idLegalEntity: Nullable<number>;
  sortColumn: number;
};
