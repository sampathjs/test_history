import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const Table = styled.div`
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
`;

export const TableHeader = styled.div`
  border-top: 1px solid ${getColor('primary.lilac60')};
`;

export const TableHeaderRow = styled.div`
  display: flex;
`;

type TableHeaderCellProps = {
  sorted?: boolean;
};

export const TableHeaderCell = styled.div<TableHeaderCellProps>`
  color: ${getColor('white.40')};
  padding: ${rem(10)} ${rem(6)};
  flex: 1;
  border-bottom: 1px solid ${getColor('primary.lilac30')};
  display: flex;
  align-items: center;
  justify-content: space-between;
  user-select: none;

  &:hover {
    background-color: ${getColor('background.tertiaryNav')};
    color: ${getColor('white.100')};
  }
`;

export const TableHeaderCellTitle = styled.span`
  ${ellipsis()}
  ${getFont('table.header')}
`;

export const TableHeaderCellSortIcon = styled.span`
  display: inline-block;
  margin-left: ${rem(2)};
  font-size: ${rem(10)};
`;

export const TableBody = styled.div``;

type TableRowProps = {
  active?: boolean;
};

export const TableRow = styled.div<TableRowProps>`
  display: flex;
  color: ${getColor('white.50')};
  box-shadow: inset 0 -1px 0 0px ${getColor('primary.lilac30')};

  &:hover {
    background-color: ${getColor('background.tertiaryNav')};
    box-shadow: inset 0 -1px 0 0px ${getColor('background.tertiaryNav')};
    color: ${getColor('white.100')};
  }

  ${(props) =>
    props.active &&
    css`
      background-color: ${getColor('background.primaryNav')};
      box-shadow: inset 0 -1px 0 0px ${getColor('background.primaryNav')};
      color: ${getColor('white.80')};
    `}
`;

type TableCellProps = {
  columnHeaderHovered?: boolean;
};

export const TableCell = styled.div<TableCellProps>`
  flex: 1;
  padding: ${rem(6)};

  ${ellipsis()}
  ${getFont('table.cell')}

  ${(props) =>
    props.columnHeaderHovered &&
    css`
      background-color: ${getColor('background.tertiaryNav')};
      color: ${getColor('white.80')};
      border-bottom-color: ${getColor('background.tertiaryNav')};
    `}
`;

export const TableNoResults = styled.div`
  text-align: center;
  padding: ${rem(10)};
  color: ${getColor('white.50')};

  ${getFont('body.medium')}
`;
