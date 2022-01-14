import { rem } from 'polished';
import styled from 'styled-components/macro';

import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableRow,
} from 'components/Table';
import { getColor, getGradient } from 'styles';

export const Container = styled.div`
  margin: 0 ${rem(12)};
  padding: 0 ${rem(16)} ${rem(16)};
  background-color: ${getColor('background.ordersGrid')};
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
`;

export const OrdersTable = styled(Table)`
  height: 100%;
  position: relative;
  overflow: auto;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    height: ${rem(60)};
    width: 100%;
    pointer-events: none;

    ${getGradient('orderBlend')}
  }
`;

type GridHeaderProps = {
  bodyScrollbarWidth: number;
};

export const GridHeader = styled(TableHeader)<GridHeaderProps>`
  padding-right: ${(props) => rem(props.bodyScrollbarWidth)};
`;

export const RowSelectorHeaderCell = styled(TableHeaderCell)`
  flex: none;
  width: ${rem(22)};
`;

export const GridContent = styled.div`
  overflow: auto;
  position: relative;
`;

export const GridBody = styled(TableBody)`
  position: relative;
`;

export const GridRow = styled(TableRow)`
  position: absolute;
  width: 100%;
`;

export const RowSelectorRowCell = styled(TableCell)`
  flex: none;
  width: ${rem(22)};
  padding: 0;
  display: inline-flex;
  align-items: center;
`;

export const ColumnPickerWrapper = styled.div`
  position: absolute;
  top: 0;
  width: 100%;
`;
